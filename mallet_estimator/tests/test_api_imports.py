# Every sibling module a function NAMES, it must be able to REACH.
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
# a whitelisted function body — that is a deliberate trade (the tests stay
# fast and bench-free) and this is what it costs. The check below buys the
# missing coverage back statically: parse the file, and for each top-level
# function ask whether the sibling names it uses are in scope by the time it
# runs. No frappe, no bench, no import of the module under test.
import ast
import os
import unittest

APP_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# The modules that are expensive enough to be imported lazily, and therefore
# the ones a function can plausibly reference without having imported.
SIBLINGS = {
    os.path.splitext(f)[0]
    for f in os.listdir(APP_DIR)
    if f.endswith(".py") and not f.startswith("__")
}


def _module_level_names(tree):
    """Names bound at module scope: imports, assignments, defs."""
    names = set()
    for node in tree.body:
        if isinstance(node, ast.Import):
            for a in node.names:
                names.add(a.asname or a.name.split(".")[0])
        elif isinstance(node, ast.ImportFrom):
            for a in node.names:
                names.add(a.asname or a.name)
        elif isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            names.add(node.name)
        elif isinstance(node, ast.Assign):
            for t in node.targets:
                if isinstance(t, ast.Name):
                    names.add(t.id)
        elif isinstance(node, ast.AnnAssign) and isinstance(node.target, ast.Name):
            names.add(node.target.id)
    return names


def _local_imports(fn):
    """Sibling names imported anywhere INSIDE this function."""
    names = set()
    for node in ast.walk(fn):
        if isinstance(node, ast.Import):
            for a in node.names:
                names.add(a.asname or a.name.split(".")[0])
        elif isinstance(node, ast.ImportFrom):
            for a in node.names:
                names.add(a.asname or a.name)
    return names


def _sibling_names_used(fn):
    """Sibling module names this function LOADS (reads, not assigns)."""
    used = set()
    for node in ast.walk(fn):
        if isinstance(node, ast.Name) and isinstance(node.ctx, ast.Load):
            if node.id in SIBLINGS:
                used.add(node.id)
    return used


def unreachable_siblings(path):
    """[(function, name)] for every sibling a function uses but cannot see."""
    with open(path) as fh:
        tree = ast.parse(fh.read(), filename=path)
    module_names = _module_level_names(tree)
    bad = []
    for node in tree.body:
        if not isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
            continue
        reachable = module_names | _local_imports(node)
        # Parameters and anything the function binds itself shadow a module
        # name legitimately — a local called `inventory` is not this bug.
        for arg in node.args.args + node.args.kwonlyargs:
            reachable.add(arg.arg)
        for inner in ast.walk(node):
            if isinstance(inner, ast.Name) and isinstance(inner.ctx, ast.Store):
                reachable.add(inner.id)
        for name in sorted(_sibling_names_used(node) - reachable):
            bad.append((node.name, name))
    return bad


class TestApiImports(unittest.TestCase):

    def test_api_functions_can_reach_the_modules_they_name(self):
        path = os.path.join(APP_DIR, "api.py")
        bad = unreachable_siblings(path)
        self.assertEqual(bad, [], "\n".join(
            "api.%s() uses `%s` but never imports it — NameError on first call"
            % (fn, name) for fn, name in bad))

    def test_the_check_would_have_caught_the_bug_that_caused_it(self):
        # A guard nobody has watched fire is a guard nobody knows the failure
        # of — the lesson from the ImageMeter sync check two days ago. So the
        # broken shape is reconstructed here and the checker is asked about
        # it, rather than trusted because the real file is clean.
        import tempfile
        src = (
            "import frappe\n"
            "def helper():\n"
            "    from mallet_estimator import inventory\n"
            "    return inventory.thing()\n"
            "def broken():\n"
            "    return inventory.is_material_code('x')\n"
            "def fixed():\n"
            "    from mallet_estimator import inventory\n"
            "    return inventory.is_material_code('x')\n"
        )
        with tempfile.NamedTemporaryFile("w", suffix=".py", delete=False) as fh:
            fh.write(src)
            tmp = fh.name
        try:
            bad = unreachable_siblings(tmp)
        finally:
            os.unlink(tmp)
        # broken() only. A sibling imported inside a DIFFERENT function does
        # not help this one, which is exactly the trap: the file is full of
        # correct-looking `from mallet_estimator import inventory` lines.
        self.assertEqual(bad, [("broken", "inventory")])

    def test_every_module_in_the_app_is_checked_not_just_api(self):
        # The style is not unique to api.py, so neither is the exposure.
        offenders = []
        for name in sorted(SIBLINGS):
            path = os.path.join(APP_DIR, name + ".py")
            for fn, missing in unreachable_siblings(path):
                offenders.append("%s.%s() uses `%s`" % (name, fn, missing))
        self.assertEqual(offenders, [], "\n".join(offenders))
