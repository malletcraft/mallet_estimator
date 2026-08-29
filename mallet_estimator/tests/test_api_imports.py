# Every name a function uses, it must be able to REACH when it runs.
#
# python -m unittest mallet_estimator.tests.test_api_imports
#
# Written 2026-08-29, after create_materials shipped CI-green, deployed
# green, and threw NameError: name 'inventory' is not defined on its first
# live call. The cause is a house style rather than a typo: api.py is loaded
# on every request, so its heavy siblings are imported INSIDE each function
# instead of at the top. Write one function the ordinary way and it reads
# correctly, compiles, passes every test, deploys, and fails the moment
# somebody presses the button.
#
# Nothing in the pure suite imports frappe, so no test in it can ever execute
# a whitelisted function body — a deliberate trade for a fast bench-free
# suite, and this is what it costs. This check buys the missing coverage back
# statically, without importing the modules under test.
#
# It uses pyflakes rather than the hand-rolled AST walk I wrote first. That
# version only knew about names matching a sibling MODULE, and within an hour
# of writing it I made the same mistake again in estimate_sku.py with a name
# it could not see — `from mallet_estimator import estimator as E`, used as
# `E`. An alias is the same bug wearing a different name, and a guard with a
# hole exactly where the next instance lands is worse than no guard, because
# it is trusted.
#
# ONLY undefined names fail this. Unused imports and shadowed variables are
# style, they are already all over a codebase this size, and a gate that
# fails on style is a gate people learn to route around.
import os
import unittest

APP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def undefined_names(paths):
    """[(path, line, message)] for every unreachable name pyflakes finds."""
    # Imported here so the failure below is about the missing tool rather
    # than a collection error nobody reads.
    from pyflakes import api as pyflakes_api
    from pyflakes import reporter as pyflakes_reporter
    from pyflakes import messages as pyflakes_messages

    found = []

    import io

    class OnlyUndefined(pyflakes_reporter.Reporter):
        def __init__(self):
            # StringIO rather than /dev/null: pyflakes never closes the
            # streams it is handed, and an unclosed file per call turns a
            # clean run into a wall of ResourceWarnings.
            super().__init__(io.StringIO(), io.StringIO())

        def flake(self, message):
            if isinstance(message, (pyflakes_messages.UndefinedName,
                                    pyflakes_messages.UndefinedLocal,
                                    pyflakes_messages.UndefinedExport)):
                found.append((message.filename, message.lineno, str(message)))

        def unexpectedError(self, filename, msg):
            found.append((filename, 0, "could not be parsed: %s" % msg))

        def syntaxError(self, filename, msg, lineno, offset, text):
            found.append((filename, lineno or 0, "syntax error: %s" % msg))

    rep = OnlyUndefined()
    for p in paths:
        pyflakes_api.checkPath(p, rep)
    return found


def app_python_files():
    out = []
    for root, dirs, files in os.walk(APP_DIR):
        dirs[:] = [d for d in dirs if d not in ("__pycache__", "node_modules")]
        for f in files:
            if f.endswith(".py"):
                out.append(os.path.join(root, f))
    return sorted(out)


class TestNamesAreReachable(unittest.TestCase):

    def test_pyflakes_is_actually_installed(self):
        # A guard that SKIPS when its tool is missing is a guard that reports
        # success having checked nothing — the exact shape this repo has been
        # bitten by (a workflow with no checkout that ran green having done
        # nothing). So its absence is a failure, loudly.
        try:
            import pyflakes  # noqa: F401
        except ImportError:  # pragma: no cover
            self.fail("pyflakes is not installed, so this check verified "
                      "NOTHING. `pip install pyflakes` — it is in ci.yml's "
                      "install step for exactly this reason.")

    def test_no_function_uses_a_name_it_cannot_reach(self):
        bad = undefined_names(app_python_files())
        self.assertEqual(bad, [], "\n".join(
            "%s:%s %s" % (os.path.relpath(p, APP_DIR), ln, msg)
            for p, ln, msg in bad))

    def test_the_check_fires_on_the_bug_that_caused_it(self):
        # A guard nobody has watched fire is a guard nobody knows the failure
        # of — the lesson from the ImageMeter sync check, which was
        # unreachable for its most important case for two days while reading
        # green. So both real shapes are reconstructed and the checker is
        # ASKED about them, rather than trusted because the repo is clean.
        import tempfile
        cases = {
            # The one that shipped: a sibling imported in a different
            # function, which does not help this one.
            "module": (
                "def helper():\n"
                "    from mallet_estimator import inventory\n"
                "    return inventory.thing()\n"
                "def broken():\n"
                "    return inventory.is_material_code('x')\n"),
            # The one I then wrote in estimate_sku.py, which the AST version
            # of this check could not see at all.
            "alias": (
                "def helper():\n"
                "    from mallet_estimator import estimator as E\n"
                "    return E.thing()\n"
                "def broken():\n"
                "    return E.joinery_lines(3)\n"),
        }
        for label, src in cases.items():
            with tempfile.NamedTemporaryFile("w", suffix=".py", delete=False) as fh:
                fh.write(src)
                tmp = fh.name
            try:
                bad = undefined_names([tmp])
            finally:
                os.unlink(tmp)
            self.assertEqual(len(bad), 1, "%s: expected one finding, got %s" % (label, bad))
            self.assertIn("undefined name", bad[0][2], label)
