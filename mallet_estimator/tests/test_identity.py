# The whoami classification rule, without a bench.
#
# python -m unittest mallet_estimator.tests.test_identity
#
# S5 (MCFT Interior Estimate on Cloudflare) decides what a caller may see
# purely from this answer, so the rule is pinned here and the bench test
# (test_whoami) checks the reads that feed it.
import unittest

from mallet_estimator.identity import classify


class TestClassify(unittest.TestCase):

    def test_system_user_is_staff(self):
        out = classify("a@b.com", "A B", "System User", [])
        self.assertEqual(out["kind"], "staff")
        self.assertEqual(out["suppliers"], [])
        self.assertEqual(out["customers"], [])

    def test_staff_stays_staff_even_when_linked(self):
        out = classify("a@b.com", "A", "System User", [("Supplier", "SUP-1")])
        self.assertEqual(out["kind"], "staff")
        self.assertEqual(out["suppliers"], ["SUP-1"])

    def test_website_user_linked_to_supplier_is_vendor(self):
        out = classify("v@x.com", "V", "Website User", [("Supplier", "SUP-2")])
        self.assertEqual(out["kind"], "vendor")
        self.assertEqual(out["suppliers"], ["SUP-2"])

    def test_website_user_linked_to_customer_is_client(self):
        out = classify("c@x.com", "C", "Website User", [("Customer", "CUST-9")])
        self.assertEqual(out["kind"], "client")
        self.assertEqual(out["customers"], ["CUST-9"])

    def test_both_is_vendor_with_both_lists(self):
        out = classify("b@x.com", "B", "Website User",
                       [("Customer", "CUST-1"), ("Supplier", "SUP-1"),
                        ("Supplier", "SUP-1")])
        self.assertEqual(out["kind"], "vendor")
        self.assertEqual(out["suppliers"], ["SUP-1"])
        self.assertEqual(out["customers"], ["CUST-1"])

    def test_unlinked_website_user_is_none(self):
        out = classify("n@x.com", "", "Website User",
                       [("Lead", "L-1"), ("Supplier", ""), ("Customer", None)])
        self.assertEqual(out["kind"], "none")
        self.assertEqual(out["full_name"], "n@x.com")

    def test_never_raises_on_junk(self):
        out = classify(None, None, None, [None, (), ("Supplier",)])
        self.assertEqual(out["kind"], "none")
        self.assertEqual(sorted(out), ["customers", "full_name", "kind",
                                       "suppliers", "user"])


if __name__ == "__main__":
    unittest.main()
