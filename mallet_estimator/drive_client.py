# The Drive transport. Deliberately thin: what to sync is decided in
# drive_sync.py (pure, tested); this only moves bytes.
#
# No google SDK. The service-account flow is a signed JWT exchanged for an
# access token, and the rest is plain REST — so the bench gains no new
# dependency: frappe already ships PyJWT and requests.
#
# Every call passes supportsAllDrives / includeItemsFromAllDrives because the
# handover folder lives in a SHARED DRIVE. That is not a detail: a service
# account has no Drive storage of its own, so creating a file in an ordinary
# "My Drive" folder fails with storageQuotaExceeded. In a shared drive the
# files belong to the drive, and it works.
import base64
import json
import os
import time

import jwt
import requests

SCOPE = "https://www.googleapis.com/auth/drive"
API = "https://www.googleapis.com/drive/v3"
UPLOAD = "https://www.googleapis.com/upload/drive/v3"
FOLDER_MIME = "application/vnd.google-apps.folder"
ENV_KEY = "MCFT_GDRIVE_SA_JSON"

_ALL_DRIVES = {"supportsAllDrives": "true", "includeItemsFromAllDrives": "true"}


class DriveError(Exception):
    pass


def load_credentials(raw=None):
    """The key is base64 of the service-account JSON (raw JSON also accepted).
    It never lives in the repo — only in an environment variable."""
    raw = raw or os.environ.get(ENV_KEY) or ""
    if not raw:
        raise DriveError(
            f"{ENV_KEY} is not set — the bench has no Google credential")
    try:
        return json.loads(base64.b64decode(raw))
    except Exception:
        try:
            return json.loads(raw)
        except Exception as exc:
            raise DriveError(f"{ENV_KEY} is not valid JSON or base64 JSON: {exc}")


class DriveClient:

    def __init__(self, credentials=None, timeout=120):
        self.creds = credentials or load_credentials()
        self.timeout = timeout
        self._token = None
        self._expires = 0

    # ---- auth -----------------------------------------------------------
    def token(self):
        if self._token and time.time() < self._expires - 60:
            return self._token
        now = int(time.time())
        assertion = jwt.encode({
            "iss": self.creds["client_email"], "scope": SCOPE,
            "aud": self.creds["token_uri"], "iat": now, "exp": now + 3600,
        }, self.creds["private_key"], algorithm="RS256")
        r = requests.post(self.creds["token_uri"], timeout=self.timeout, data={
            "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
            "assertion": assertion})
        if r.status_code != 200:
            raise DriveError(f"token request failed [{r.status_code}]: {r.text[:300]}")
        body = r.json()
        self._token = body["access_token"]
        self._expires = time.time() + int(body.get("expires_in", 3600))
        return self._token

    def _headers(self, extra=None):
        h = {"Authorization": "Bearer " + self.token()}
        h.update(extra or {})
        return h

    def _check(self, r, what):
        if r.status_code not in (200, 201):
            raise DriveError(f"{what} failed [{r.status_code}]: {r.text[:300]}")
        return r

    # ---- reading --------------------------------------------------------
    def list_children(self, parent_id, only_folders=False, page_size=200,
                      extra_q=None):
        q = f"'{parent_id}' in parents and trashed = false"
        if only_folders:
            q += f" and mimeType = '{FOLDER_MIME}'"
        if extra_q:
            q += f" and ({extra_q})"
        out, page = [], None
        while True:
            params = dict(_ALL_DRIVES, q=q, corpora="allDrives", pageSize=page_size,
                          fields="nextPageToken, files(id,name,mimeType,size,modifiedTime)")
            if page:
                params["pageToken"] = page
            r = self._check(requests.get(f"{API}/files", headers=self._headers(),
                                         params=params, timeout=self.timeout), "list")
            body = r.json()
            out.extend(body.get("files", []))
            page = body.get("nextPageToken")
            if not page:
                return out

    def find_child(self, parent_id, name):
        for f in self.list_children(parent_id):
            if f["name"] == name:
                return f
        return None

    def download(self, file_id):
        r = self._check(requests.get(f"{API}/files/{file_id}", headers=self._headers(),
                                     params=dict(_ALL_DRIVES, alt="media"),
                                     timeout=self.timeout), "download")
        return r.content

    def walk_files(self, root_id, _trail=(), since=None, name_prefixes=()):
        """Every file under a folder, with the folder trail that led to it —
        the trail is what tells a person which room a returning photo is from.

        since / name_prefixes narrow the walk AT DRIVE, not afterwards. The
        ImageMeter folder holds years of unrelated photos, and fetching all of
        them to throw nearly all away each hour is what made a scan cap
        necessary in the first place.

        Two things the filter must not break, and both are in the query rather
        than left to luck:
        - FOLDERS are always returned whatever their own timestamp, or an old
          folder hides every new photo inside it.
        - A file carrying one of our id prefixes is always returned however
          old it looks, because naming a capture outranks age — that is the
          rule that lets a long-delayed annotation still attach itself."""
        keep = []
        if since:
            keep.append(f"modifiedTime > '{since}'")
        for p in name_prefixes:
            keep.append(f"name contains '{p}'")
        extra_q = None
        if keep:
            extra_q = " or ".join([f"mimeType = '{FOLDER_MIME}'"] + keep)

        found = []
        for f in self.list_children(root_id, extra_q=extra_q):
            if f["mimeType"] == FOLDER_MIME:
                found.extend(self.walk_files(
                    f["id"], _trail + (f["name"],),
                    since=since, name_prefixes=name_prefixes))
            else:
                found.append({"id": f["id"], "title": f["name"],
                              "parents_path": list(_trail),
                              "modified": f.get("modifiedTime")})
        return found

    # ---- writing --------------------------------------------------------
    def ensure_folder(self, parent_id, name):
        """Idempotent: a re-run must reuse the folder, not make a second one
        with the same name (Drive allows duplicates by name)."""
        found = self.find_child(parent_id, name)
        if found and found["mimeType"] == FOLDER_MIME:
            return found["id"]
        r = self._check(requests.post(
            f"{API}/files", headers=self._headers({"Content-Type": "application/json"}),
            params=dict(_ALL_DRIVES), timeout=self.timeout,
            json={"name": name, "mimeType": FOLDER_MIME, "parents": [parent_id]}),
            f"create folder {name}")
        return r.json()["id"]

    def ensure_path(self, root_id, names):
        parent = root_id
        for n in names:
            parent = self.ensure_folder(parent, n)
        return parent

    def upload(self, parent_id, name, data, mime="image/jpeg"):
        """Metadata first, then the bytes. Two steps rather than a multipart
        body because requests sends multipart/form-data and Drive wants
        multipart/related — this avoids hand-rolling the envelope."""
        r = self._check(requests.post(
            f"{API}/files", headers=self._headers({"Content-Type": "application/json"}),
            params=dict(_ALL_DRIVES), timeout=self.timeout,
            json={"name": name, "parents": [parent_id]}), f"create {name}")
        file_id = r.json()["id"]
        self._check(requests.patch(
            f"{UPLOAD}/files/{file_id}", headers=self._headers({"Content-Type": mime}),
            params=dict(_ALL_DRIVES, uploadType="media"),
            data=data, timeout=self.timeout), f"upload {name}")
        return file_id

    def rename(self, file_id, new_name):
        """Prefer this over delete-and-re-upload: it keeps the bytes, the id
        and the history, and it needs no delete right — a Content manager on a
        shared drive can rename but cannot permanently delete (Drive answers
        404, not 403, which reads misleadingly like a missing file)."""
        r = self._check(requests.patch(
            f"{API}/files/{file_id}",
            headers=self._headers({"Content-Type": "application/json"}),
            params=dict(_ALL_DRIVES), timeout=self.timeout,
            json={"name": new_name}), f"rename to {new_name}")
        return r.json()["id"]

    def delete(self, file_id):
        r = requests.delete(f"{API}/files/{file_id}", headers=self._headers(),
                            params=dict(_ALL_DRIVES), timeout=self.timeout)
        if r.status_code not in (200, 204):
            raise DriveError(f"delete failed [{r.status_code}]: {r.text[:200]}")
