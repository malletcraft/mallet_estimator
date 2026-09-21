#!/usr/bin/env python3
"""Validate .github/workflows/*.yml the way GitHub does, not the way PyYAML does.

CLAUDE.md names this file and says it runs as its own CI job. It did not
exist. This is it.

THE TRAP IT IS FOR: `yaml.safe_load` TOLERATES DUPLICATE KEYS, silently
keeping the last. A workflow with two `push:` keys under `on:` therefore
parses cleanly on a laptop and is REJECTED BY GITHUB AT STARTUP -- which
shows up as a completed run with ZERO JOBS and no annotations, and reads
exactly like a run that has not started yet. That cost a day once
(deploy.yml, 2026-08). Silence and success look identical, so the duplicate
key has to be an error here or nothing ever catches it.

Checks, each one earned by a real failure:
  1. The file parses at all.
  2. No duplicate mapping key anywhere (the one above).
  3. `on:` exists and is not empty -- an `on:` that resolves to nothing is a
     workflow that never triggers and never says so.
  4. Every job has `runs-on` and at least one step.
  5. No `environment:` that resolves to an empty string (deploy.yml failed at
     startup on exactly this, for a day, with a red run and zero jobs).
  6. A step that references `${{ secrets.* }}` is not also `continue-on-error`
     without saying why -- a credential failure that cannot turn a run red is
     the silence this project keeps paying for. (Warning, not an error: some
     probes are deliberately best-effort and carry a comment saying so.)

Run:  python3 tools/lint_workflows.py
Exit: 0 clean, 1 on any error.
"""
import pathlib
import sys

import yaml


class NoDuplicates(yaml.SafeLoader):
    pass


def _no_duplicate_keys(loader, node, deep=False):
    mapping = {}
    for key_node, value_node in node.value:
        key = loader.construct_object(key_node, deep=deep)
        if key in mapping:
            raise yaml.constructor.ConstructorError(
                "while constructing a mapping", node.start_mark,
                f"duplicate key {key!r} -- GitHub rejects this file at startup, "
                f"and the run reports zero jobs with no annotations",
                key_node.start_mark)
        mapping[key] = loader.construct_object(value_node, deep=deep)
    return mapping


NoDuplicates.add_constructor(
    yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, _no_duplicate_keys)


def lint(path):
    errors, warnings = [], []
    raw = path.read_text()
    try:
        doc = yaml.load(raw, Loader=NoDuplicates)
    except yaml.YAMLError as exc:
        return [f"{path}: {exc}"], []

    if not isinstance(doc, dict):
        return [f"{path}: top level is not a mapping"], []

    # PyYAML resolves a bare `on:` to the boolean True, which is why this
    # looks for both spellings rather than the obvious one.
    triggers = doc.get("on", doc.get(True))
    if not triggers:
        errors.append(f"{path}: `on:` is missing or empty -- nothing will ever trigger it")

    jobs = doc.get("jobs") or {}
    if not jobs:
        errors.append(f"{path}: no jobs")
    for name, job in jobs.items():
        if not isinstance(job, dict):
            errors.append(f"{path}: job {name} is not a mapping")
            continue
        if not job.get("runs-on") and not job.get("uses"):
            errors.append(f"{path}: job {name} has no runs-on")
        steps = job.get("steps")
        if not job.get("uses") and not steps:
            errors.append(f"{path}: job {name} has no steps")
        if "environment" in job and not job["environment"]:
            errors.append(f"{path}: job {name} has an empty `environment:` -- "
                          f"GitHub rejects the run at startup and reports zero jobs")
        for step in steps or []:
            if not isinstance(step, dict):
                continue
            uses_secret = "secrets." in yaml.dump(step.get("env") or {})
            if uses_secret and step.get("continue-on-error"):
                warnings.append(
                    f"{path}: step {step.get('name', '?')!r} uses secrets and is "
                    f"continue-on-error -- a credential failure here cannot turn "
                    f"the run red. Intended? Say so in a comment.")
    return errors, warnings


def main():
    root = pathlib.Path(__file__).resolve().parent.parent / ".github/workflows"
    files = sorted(list(root.glob("*.yml")) + list(root.glob("*.yaml")))
    if not files:
        print(f"no workflows found under {root} -- that is itself suspicious")
        return 1
    errors, warnings = [], []
    for f in files:
        e, w = lint(f)
        errors += e
        warnings += w
    for w in warnings:
        print(f"WARN  {w}")
    for e in errors:
        print(f"ERROR {e}")
    print(f"\n{len(files)} workflow(s), {len(errors)} error(s), {len(warnings)} warning(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
