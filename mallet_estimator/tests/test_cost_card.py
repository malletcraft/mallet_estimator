# The plugin's cost card. ERP is the only authority for money, and every
# number it hands over has to say so — that is the whole contract.
import frappe

try:
    from frappe.tests import IntegrationTestCase as MalletTestCase
except Exception:
    from frappe.tests.utils import FrappeTestCase as MalletTestCase

from mallet_estimator import api, estimator, inventory


class TestCostCard(MalletTestCase):

    def test_all_seventeen_operations_in_process_order(self):
        # Amit, 2026-08-22: "keep all 17 operations as is . no drop." The
        # plugin quotes the same process the floor works, or it is not a gauge
        # of anything.
        card = api.cost_card()
        ops = card["operations"]
        self.assertEqual(len(ops), len(estimator.OPERATION_STANDARDS))
        self.assertEqual(len(ops), 17)
        self.assertEqual(ops[0]["name"], "Sheet Lamination")   # pasting
        self.assertEqual(ops[-2]["name"], "Installation")      # …to installation
        self.assertEqual([o["seq"] for o in ops], list(range(1, len(ops) + 1)))

    def test_every_operation_carries_a_workstation_and_a_rate_source(self):
        for o in api.cost_card()["operations"]:
            self.assertTrue(o["workstation"], f"{o['name']} has no workstation")
            self.assertIn(o["rate_source"], ("erp:Workstation", "unset"))
            self.assertIn(o["min_source"],
                          ("erp:Operation", "code default", "no standard"))

    def test_the_envelope_names_erp_as_the_authority(self):
        # The plugin stores its own material rates and must override them with
        # these. A payload that did not say where it came from would leave a
        # person unable to tell one from the other, which is exactly what Amit
        # asked to be able to see.
        card = api.cost_card()
        self.assertEqual(card["authority"], "erp")
        self.assertEqual(card["price_list"], inventory.ESTIMATION_PRICE_LIST)
        self.assertIn("post-tax", card["rates_are"])
        self.assertEqual(card["productive_min_per_day"],
                         estimator.PRODUCTIVE_MIN_PER_DAY)

    def test_the_assembly_rule_is_declared_not_hardcoded_on_the_plugin(self):
        rule = api.cost_card()["assembly_rule"]
        self.assertEqual(rule["operation"], "Assembly")
        self.assertEqual(rule["component_prefix"], "ASMBL")
        self.assertTrue(rule["editable"])
        self.assertIn("Assembly", estimator.OPERATION_STANDARDS)

    def test_a_material_erp_has_never_heard_of_is_named_not_hidden(self):
        # The failure that matters: the plugin has its own rate for this and
        # would happily quote it. Coming back "not in erp" and NOT quotable is
        # what stops a client being shown a number from the wrong place.
        card = api.cost_card(codes="SG_PLY_V9_NOSUCHTHING_99mm")
        row = card["materials"][0]
        self.assertEqual(row["source"], "not in erp")
        self.assertFalse(row["quotable"])
        self.assertEqual(row["landed_rate"], 0.0)

    def test_a_known_material_comes_back_post_tax_with_its_source(self):
        item = frappe.db.get_value("Item", {"item_group": ["like", "%"]}, "name")
        if not item:
            self.skipTest("no items on this bench")
        row = api.cost_card(codes=item)["materials"][0]
        self.assertEqual(row["item_code"], item)
        self.assertTrue(row["source"].startswith("erp:"))
        # landed = base grossed up by the item's GST; never quietly equal when
        # a GST percentage applies.
        if row["base_rate"] and row["gst_pct"]:
            self.assertGreater(row["landed_rate"], row["base_rate"])

    def test_create_missing_mints_the_item_but_never_a_rate(self):
        # Amit, 2026-08-22: "ADD material TO ERP." ERP is the master, so a
        # material the model uses and ERP has not seen is a gap in ERP. What
        # gets created is the Item — a fact about the model. The RATE is a
        # decision somebody has to make, and stays a human act on the
        # Estimation (Assumed) price list.
        code = "SG_PLY_V1_zzt_zzt_18mm"
        frappe.db.delete("Item", {"item_code": ["like", "%zzt_zzt%"]})
        before = api.cost_card(codes=code)["materials"][0]
        self.assertEqual(before["source"], "not in erp")

        after = api.cost_card(codes=code, create_missing=1)
        row = after["materials"][0]
        self.assertTrue(row["item_code"], "no Item was created")
        self.assertIn(row["item_code"], after["created_items"])
        # Created, and deliberately NOT quotable — it has no rate yet, and
        # saying so is the whole point.
        self.assertFalse(row["quotable"])
        self.assertEqual(row["landed_rate"], 0.0)

        # …and it is idempotent: a second call finds it rather than minting
        # a second one.
        again = api.cost_card(codes=code, create_missing=1)
        self.assertEqual(again["created_items"], [])
        self.assertEqual(again["materials"][0]["item_code"], row["item_code"])

    def test_create_missing_will_not_mint_an_item_from_a_typo(self):
        # The OpenCutList grammar is the gate. A component somebody named
        # "Group#3" must never become an Item.
        junk = "Group#3 copy"
        out = api.cost_card(codes=junk, create_missing=1)
        self.assertEqual(out["created_items"], [])
        self.assertEqual(out["materials"][0]["source"], "not in erp")

    def test_the_card_states_the_wastage_rule_both_sides_must_use(self):
        # "wastage treatment - charge full board" — whole boards consumed, not
        # the area the parts occupy. Stated in the payload so the plugin and
        # the bench cannot drift into quoting two different numbers.
        self.assertIn("full board", api.cost_card()["wastage"])

    def test_codes_accept_a_list_a_csv_string_or_nothing(self):
        self.assertEqual(api.cost_card()["materials"], [])
        self.assertEqual(len(api.cost_card(codes="A_ONE,A_TWO")["materials"]), 2)
        self.assertEqual(len(api.cost_card(codes='["A_ONE","A_TWO"]')["materials"]), 2)
        self.assertEqual(len(api.cost_card(codes=["A_ONE"])["materials"]), 1)


class TestEstimatePreview(MalletTestCase):
    """The on-the-fly estimate. Priced by ERP end to end, saving nothing."""

    # The EXACT header set the plugin sends (McftPushWorker::CSV_HEADERS) —
    # part-list columns, a Quantity column, and NO "Area - final". The first
    # version of these tests invented a CSV with an area column, which no
    # caller produces, and so tested a code path the plugin never reaches.
    CSV = (
        "No.;Designation;Quantity;Length;Width;Thickness;Material type;"
        "Material name;Edge Length 1;Edge Length 2;Edge Width 1;Edge Width 2;"
        "Frontside;Backside;Tags\n"
        "1;ASMBL_Carcass;2;2100;600;16;Sheet Goods;SG_PLY_V0_a_a;"
        "EB_PVC_IN_a (1 mm x 22 mm);;;;;;carcass_vert\n"
        "2;ASMBL_Drawer;3;1140;580;16;Sheet Goods;SG_PLY_V0_a_a;"
        "EB_PVC_IN_a (1 mm x 22 mm);;;;SG_LAM_V0_a_a;;drawer_side\n"
        "3;HWD_AH_SC_0;4;;;;Hardware;HWD_Hinge;;;;;;;\n"
    )

    def test_it_prices_material_and_all_seventeen_operations(self):
        out = api.estimate_preview(self.CSV)
        self.assertEqual(out["authority"], "erp")
        self.assertEqual(len(out["labour"]), 17)
        self.assertTrue(out["materials"], "no material lines")
        # total is the two halves, not something else
        self.assertAlmostEqual(
            out["total"], out["material_total"] + out["labour_total"], places=2)

    def test_every_line_says_where_its_number_came_from(self):
        out = api.estimate_preview(self.CSV)
        for m in out["materials"]:
            self.assertTrue(m["source"], f"{m['code']} has no source")
        for l in out["labour"]:
            self.assertIn(l["min_source"], ("erp:Operation", "code default",
                                            "no standard", "plugin:edited"))

    def test_the_catch_all_row_can_actually_be_used(self):
        """Amit, 2026-08-23: "not able to key in quantity. so result will
        always be zero."

        Miscellaneous - extra exists to carry work the other sixteen do not
        name, so the model has nothing to infer a quantity from and returns 0.
        With the quantity locked, no minutes typed beside it could ever make
        the row worth anything — it was decoration in the shape of a line.

        And its badge said "seed default", blaming a fallback for a number
        nobody had ever set. There is no standard time for unnamed work; the
        row says so now instead.
        """
        out = api.estimate_preview(self.CSV)
        row = [r for r in out["labour"] if r["name"] == "Miscellaneous - extra"]
        self.assertEqual(len(row), 1)
        row = row[0]
        self.assertTrue(row["qty_editable"], "the catch-all needs a typeable quantity")
        self.assertTrue(row["min_editable"])
        self.assertEqual(row["min_source"], "no standard")

        # And a typed quantity plus typed minutes actually reaches the total.
        priced = api.estimate_preview(
            self.CSV,
            overrides={"Miscellaneous - extra": {"qty": 3, "min": 20}})
        extra = [r for r in priced["labour"]
                 if r["name"] == "Miscellaneous - extra"][0]
        self.assertEqual(extra["qty"], 3.0)
        self.assertEqual(extra["hours"], 1.0)          # 3 x 20 min
        self.assertGreater(priced["labour_total"], out["labour_total"])

    def test_unpriced_lines_are_counted_loudly(self):
        # A total that quietly omits boards ERP cannot price looks like an
        # answer and is not one.
        out = api.estimate_preview(self.CSV)
        self.assertIn("unpriced_lines", out)
        self.assertEqual(
            out["unpriced_lines"],
            sum(1 for m in out["materials"] if not m["quotable"]))

    def test_the_assembly_count_comes_from_the_model_when_given(self):
        # Amit, 2026-08-22: "aggregate number of ASMBL components into that
        # line and then let me modify how much time assembly can take."
        out = api.estimate_preview(self.CSV, assembly_count=7)
        self.assertEqual(out["assembly_count"], 7)
        self.assertEqual(out["assembly_source"], "plugin:ASMBL count")
        asm = next(l for l in out["labour"] if l["name"] == "Assembly")
        self.assertEqual(asm["qty"], 7)
        # …and the chain that follows assemblies follows it too, or the
        # downstream lines quietly contradict the one above them.
        for name in ("Disassembly", "Packing", "Installation"):
            self.assertEqual(
                next(l for l in out["labour"] if l["name"] == name)["qty"], 7)

    def test_the_assembly_minutes_are_editable_and_say_so(self):
        base = api.estimate_preview(self.CSV, assembly_count=2)
        edited = api.estimate_preview(self.CSV, assembly_count=2, assembly_min=99)
        a0 = next(l for l in base["labour"] if l["name"] == "Assembly")
        a1 = next(l for l in edited["labour"] if l["name"] == "Assembly")
        self.assertEqual(a1["min_per_unit"], 99)
        self.assertEqual(a1["min_source"], "plugin:edited")
        self.assertNotEqual(a0["min_per_unit"], a1["min_per_unit"])

    def test_an_empty_csv_is_refused_rather_than_priced_at_zero(self):
        with self.assertRaises(frappe.ValidationError):
            api.estimate_preview("")

    def test_it_saves_nothing(self):
        before = frappe.db.count("Estimate SKU")
        api.estimate_preview(self.CSV)
        self.assertEqual(frappe.db.count("Estimate SKU"), before)

    def test_a_zero_assembly_count_is_not_an_override(self):
        """A model with no ASMBL component sends 0 — that is an absence, not
        an instruction. Badging it "plugin:ASMBL count" while pricing the line
        off ERP's own rule puts a contradiction on one screen."""
        out = api.estimate_preview(self.CSV, assembly_count=0)
        self.assertIn("erp:", out["assembly_source"])
        self.assertIn("no ASMBL", out["assembly_source"])
        # The count shown is the count PRICED, so it agrees with the row.
        asm = next(l for l in out["labour"] if l["name"] == "Assembly")
        self.assertEqual(out["assembly_count"], asm["qty"])

    def test_the_headline_count_always_matches_the_assembly_row(self):
        """Whichever of the three sources answered, the number printed beside
        the header is the number the Assembly line was costed at."""
        for kwargs in ({}, {"assembly_count": 0}, {"assembly_count": 5}):
            out = api.estimate_preview(self.CSV, **kwargs)
            asm = next(l for l in out["labour"] if l["name"] == "Assembly")
            self.assertEqual(out["assembly_count"], asm["qty"], msg=str(kwargs))

    # No ASMBL designation anywhere, deliberately: with one present the CSV
    # fallback supplies the assembly count and OVERRIDES the 1-plus-rails
    # arithmetic, so a test written on the base CSV would assert 2 and get 2
    # from the wrong source entirely. Stripping ASMBL is what leaves _hw as
    # the only thing that can answer.
    CSV_HARDWARE = (
        "No.;Designation;Quantity;Length;Width;Thickness;Material type;"
        "Material name;Edge Length 1;Edge Length 2;Edge Width 1;Edge Width 2;"
        "Frontside;Backside;Tags\n"
        "1;Side;1;600;400;16;Sheet Goods;SG_PLY_V0_a_a;;;;;;;\n"
        "2;HWD_AH_SC_0;1;;;;Hardware;HWD_Hinge;;;;;;;\n"
        "3;HWD_DR_TANDEM;1;;;;Hardware;HWD_Rail;;;;;;;\n"
    )

    def test_hardware_is_recognised_through_the_shape_adapter(self):
        """operation_quantities reads m["name"]; aggregate() writes "material".
        estimate_preview adapts between them, and if that adapter ever stops
        carrying the hardware NAME the quantities go quietly to zero rather
        than raising — _hw matches substrings, and a name it cannot read simply
        matches nothing. So assert on quantities only a readable name yields."""
        out = api.estimate_preview(self.CSV_HARDWARE)
        # Nothing overrode it, so this IS 1 + drawer rails ...
        self.assertEqual(out["assembly_source"], "erp:1 + drawer rails")
        asm = next(l for l in out["labour"] if l["name"] == "Assembly")
        self.assertEqual(asm["qty"], 2, "the HWD_Rail line was not recognised")
        # ... and Install Hardware is hinges + rails + handles + shelf, which
        # is 2 here and 0 if the names did not survive the adapter.
        ih = next(l for l in out["labour"] if l["name"] == "Install Hardware")
        self.assertEqual(ih["qty"], 2)

    def test_sheet_operations_count_whole_boards(self):
        """The other half of the same adapter: kind must survive too, or the
        three sheet operations silently cost nothing."""
        out = api.estimate_preview(self.CSV)
        for name in ("Sheet Cutting", "Sheet Lamination", "Sheet Tape Removal"):
            row = next(l for l in out["labour"] if l["name"] == name)
            self.assertGreater(row["qty"], 0, "%s saw no sheets" % name)

    def test_boards_are_nested_not_divided_by_area(self):
        """THE bug this endpoint shipped with, 2026-08-22.

        opencutlist.aggregate() derives sheets from an "Area - final" column.
        The plugin's part-list CSV does not have one, so every sheet measured
        0 m², every board count came out 0, and the screen showed material
        lines priced at nothing beside OpenCutList's own table saying 2 + 2
        boards. Nothing raised — a zero quantity is a perfectly valid number.

        So this asserts on the one thing the area path could never produce:
        a board count above zero from a CSV that carries no area at all."""
        out = api.estimate_preview(self.CSV)
        sheets = [m for m in out["materials"] if m["kind"] == "sheet"]
        self.assertTrue(sheets, "no sheet lines at all")
        for m in sheets:
            self.assertGreater(m["qty"], 0, "%s nested to zero boards" % m["code"])

    def test_a_row_stands_for_its_quantity_not_for_one_piece(self):
        """The same failure's quieter half. OpenCutList groups identical parts
        onto ONE row with the count in `Quantity`; aggregate() incremented by
        one per row, so 24 MiniFix on a single row priced as one. That is not
        a visibly broken number the way a zero is — it is just a cheap
        estimate, which is worse."""
        out = api.estimate_preview(self.CSV)
        hw = [m for m in out["materials"] if m["kind"] == "hardware"]
        self.assertTrue(hw, "no hardware lines")
        # The CSV says Quantity 4 on the hinge row.
        self.assertEqual(sum(m["qty"] for m in hw), 4)

    def test_edge_banding_is_priced_by_the_roll_it_is_bought_in(self):
        """Edge banding is stocked in metres and bought in whole rolls. The
        line quantity is rolls, so the rate must be scaled by the roll length
        or the amount lands 50x under."""
        from mallet_estimator import inventory
        out = api.estimate_preview(self.CSV)
        edge = [m for m in out["materials"] if m["kind"] == "edge"]
        self.assertTrue(edge, "the CSV bands three edges — no edge line appeared")
        for m in edge:
            self.assertEqual(m["uom"], "Roll")
            if m["quotable"]:
                self.assertAlmostEqual(
                    m["amount"], m["rate"] * m["qty"], places=2)
                # rate is per ROLL, i.e. metre-rate x roll length
                self.assertGreater(m["rate"], 0)
        self.assertGreater(inventory.EDGE_ROLL_METERS, 1)

    def test_every_number_rounds_up_to_one_decimal(self):
        """Amit, 2026-08-22: "all number should be rounded to one decimal like
        3.333 should 3.4." 3.333 to one decimal is 3.3 by arithmetic — what he
        described is rounding UP, which is the right way round for a quote:
        the fraction lands on the shop's side, never the client's."""
        out = api.estimate_preview(self.CSV)
        for l in out["labour"]:
            for k in ("qty", "min_per_unit", "hours"):
                v = l[k]
                self.assertAlmostEqual(v, round(v, 1), places=9,
                                       msg="%s %s = %r is not one decimal" % (l["name"], k, v))
        self.assertAlmostEqual(out["days"], round(out["days"], 1), places=9)

    def test_days_come_off_a_six_hour_working_day(self):
        """Amit: "Also need number of days required to make that happen.
        assume 6 hours working day." Same six-hour productive day the bench
        already costs with (est_days = minutes / 360)."""
        out = api.estimate_preview(self.CSV)
        self.assertEqual(out["hours_per_day"], 6)
        hours = sum(l["hours"] for l in out["labour"])
        self.assertGreater(out["days"], 0)
        # rounded UP off the same hours the rows show
        self.assertLess(abs(out["days"] - hours / 6.0), 0.1)

    def test_grooving_takes_both_a_count_and_a_time(self):
        """"7. Grooving - quanity along with time will be decided by designer
        as article decide how many grooving are required.\""""
        out = api.estimate_preview(self.CSV, overrides={"Grooving": {"qty": 5, "min": 12}})
        g = next(l for l in out["labour"] if l["name"] == "Grooving")
        self.assertEqual(g["qty"], 5)
        self.assertEqual(g["min_per_unit"], 12)
        self.assertEqual(g["qty_source"], "plugin:edited")
        self.assertEqual(g["min_source"], "plugin:edited")

    def test_steps_eight_to_seventeen_take_a_time_but_not_a_count(self):
        """"8 to 17 steps number should be editable for time as its carpenters
        judgment how much time it takes to that operation but quantity should
        not be editable." Install Hardware (9) is the one that matters most:
        its count is hinges + rails + handles + shelf supports off the model,
        and a hand-typed number would unhook it silently."""
        out = api.estimate_preview(self.CSV, overrides={"Installation": {"min": 45}})
        i = next(l for l in out["labour"] if l["name"] == "Installation")
        self.assertEqual(i["min_per_unit"], 45)
        with self.assertRaises(frappe.ValidationError):
            api.estimate_preview(self.CSV, overrides={"Install Hardware": {"qty": 99}})

    def test_the_first_six_steps_refuse_a_hand_typed_time(self):
        """They are computed from the cut list itself; a person overruling
        them is overruling the model, which is what step 17 exists for."""
        with self.assertRaises(frappe.ValidationError):
            api.estimate_preview(self.CSV, overrides={"Sheet Cutting": {"min": 99}})

    def test_the_screen_is_told_which_cells_are_editable(self):
        """The rule lives on the server; the plugin renders what it is told,
        so the two cannot drift into offering an input that is refused."""
        out = api.estimate_preview(self.CSV)
        by = {l["name"]: l for l in out["labour"]}
        self.assertTrue(by["Grooving"]["qty_editable"])
        self.assertTrue(by["Grooving"]["min_editable"])
        self.assertFalse(by["Install Hardware"]["qty_editable"])
        self.assertTrue(by["Install Hardware"]["min_editable"])
        self.assertFalse(by["Sheet Cutting"]["min_editable"])
        self.assertFalse(by["Sheet Cutting"]["qty_editable"])

    def test_loading_is_costed_in_the_factory(self):
        """Amit: "12. Loading On-Site should be in factory as loading is done
        at factory for packed articles." The workstation is what carries the
        rate, so this is a price change, not a label."""
        out = api.estimate_preview(self.CSV)
        loading = next(l for l in out["labour"] if l["name"] == "Loading")
        self.assertNotEqual(loading["workstation"], "On-Site")
        unloading = next(l for l in out["labour"] if l["name"] == "Unloading")
        self.assertEqual(unloading["workstation"], "On-Site",
                         "unloading genuinely happens at the site")

    def test_no_printable_line_mentions_markup_or_margin(self):
        """A RULE, Amit 2026-08-22: "never mention about any markup or profit
        margin on any printable document." Naming it in a list of exclusions
        was the worst place for it — it tells a client a markup exists and
        that this figure is not the whole of it."""
        out = api.estimate_preview(self.CSV)
        printable = " ".join(out["excludes"]) + " " + out["wastage"] + " " + out["rates_are"]
        for word in ("markup", "mark-up", "margin", "profit"):
            self.assertNotIn(word, printable.lower(), "%r reaches a printed page" % word)

    # Three sizes, the convention Amit types in SketchUp.
    CSV_SIZED = (
        "No.;Designation;Quantity;Length;Width;Thickness;Material type;"
        "Material name;Edge Length 1;Edge Length 2;Edge Width 1;Edge Width 2;"
        "Frontside;Backside;Tags\n"
        "1;ASMBL_L_WAR;2;2100;600;16;Sheet Goods;SG_PLY_V0_a_a;;;;;;;\n"
        "2;ASMBL_M_DRW;2;600;400;16;Sheet Goods;SG_PLY_V0_a_a;;;;;;;\n"
        "3;ASMBL_S_SHELF;1;900;300;16;Sheet Goods;SG_PLY_V0_a_a;;;;;;;\n"
    )

    def test_assemblies_are_counted_by_their_size_token(self):
        """Amit, 2026-08-23: "I will use ASMBL_L_ WAR ASMBL_M_DRW and
        ASMBL_S_SHELF ASMBL_L_BED like these convention.\""""
        out = api.estimate_preview(self.CSV_SIZED)
        self.assertEqual(out["assembly_sizes"],
                         {"large": 1, "medium": 1, "small": 1})
        self.assertEqual(out["assembly_count"], 3)

    def test_only_large_assemblies_are_disassembled(self):
        """"Only large assemblies should participate in disassembly." A
        carcass comes apart to leave the works; a drawer or a shelf travels
        assembled."""
        out = api.estimate_preview(self.CSV_SIZED)
        d = next(l for l in out["labour"] if l["name"] == "Disassembly")
        self.assertEqual(d["qty"], 1, "disassembly counted more than the large one")
        # everything else downstream still follows the whole set
        for name in ("Packing", "Loading", "Installation"):
            row = next(l for l in out["labour"] if l["name"] == name)
            self.assertEqual(row["qty"], 3, "%s should cover every assembly" % name)

    def test_each_size_takes_its_own_minutes(self):
        out = api.estimate_preview(
            self.CSV_SIZED,
            assembly_min_by_size={"large": 120, "medium": 30, "small": 10})
        a = next(l for l in out["labour"] if l["name"] == "Assembly")
        # 1x120 + 1x30 + 1x10 = 160 min = 2.67 h, rounded up to one decimal
        self.assertEqual(a["hours"], 2.7)
        self.assertEqual(a["min_source"], "plugin:edited")
        self.assertEqual(out["assembly_min_by_size"]["large"], 120)

    def test_assembly_splits_into_one_child_row_per_size(self):
        """Amit, 2026-08-23: "this should split into child rows and user should
        be able to key in directly minutes against the quantity, quantity is
        inferred from our size model which should not get altered but minutes
        should be changeable in line only.\""""
        out = api.estimate_preview(self.CSV_SIZED)
        a = next(l for l in out["labour"] if l["name"] == "Assembly")
        kids = a["children"]
        self.assertEqual([c["size"] for c in kids], ["large", "medium", "small"])
        for c in kids:
            self.assertTrue(c["min_editable"], "%s cannot take a time" % c["name"])
            self.assertFalse(c["qty_editable"], "%s offers an editable count" % c["name"])
        # the counts are the model's answer, not anyone's input
        self.assertEqual([c["qty"] for c in kids], [1, 1, 1])
        # THE PARENT IS ITS CHILDREN, exactly — hours and money both. Each
        # child rounds up on its own, so a parent computed from the true total
        # would print three rows that do not add up to the line above them.
        # On a screen shared with a client that is indefensible whatever the
        # arithmetic behind it.
        self.assertEqual(round(sum(c["hours"] for c in kids), 4), a["hours"])
        self.assertEqual(round(sum(c["amount"] for c in kids), 2), a["amount"])

    def test_a_child_row_takes_its_own_minutes_in_line(self):
        out = api.estimate_preview(
            self.CSV_SIZED,
            overrides={"Assembly": {"min_large": 120, "min_medium": 30, "min_small": 10}})
        a = next(l for l in out["labour"] if l["name"] == "Assembly")
        got = {c["size"]: c["min_per_unit"] for c in a["children"]}
        self.assertEqual(got, {"large": 120, "medium": 30, "small": 10})
        self.assertEqual(a["hours"], 2.7)      # (120+30+10)/60, rounded up
        self.assertEqual(a["min_source"], "plugin:edited")

    def test_only_assembly_carries_children(self):
        """Every other row sends none, so the screen renders one shape and
        never branches on an operation's name."""
        out = api.estimate_preview(self.CSV_SIZED)
        for l in out["labour"]:
            if l["name"] != "Assembly":
                self.assertIsNone(l["children"], "%s grew children" % l["name"])

    def test_a_nameless_assembly_is_treated_as_large(self):
        """Every model drawn before this convention says plain ASMBL_WAR, and
        those are carcasses. Reading them as small would quietly shrink the
        estimate of every existing model."""
        out = api.estimate_preview(self.CSV)      # designations ASMBL_Carcass etc
        self.assertGreater(out["assembly_unsized"], 0)
        self.assertEqual(out["assembly_sizes"]["large"], out["assembly_count"])
        d = next(l for l in out["labour"] if l["name"] == "Disassembly")
        self.assertEqual(d["qty"], out["assembly_count"],
                         "an unsized model lost its disassembly")


class TestEndpointsAreReachableOverHttp(MalletTestCase):
    """Every test in this file calls its endpoint as a plain Python function,
    where @frappe.whitelist() is irrelevant. The plugin and the phone call it
    over HTTP, where it is the only thing that matters.

    That gap is not hypothetical. On 2026-08-23 a helper was inserted directly
    above `def estimate_preview`, which put it BETWEEN the decorator and the
    function — valid Python, so ast parsed it, and the whole suite stayed
    green while the decorator quietly moved onto the helper. The live site
    answered "Function mallet_estimator.api.estimate_preview is not
    whitelisted" to the first real request after the deploy.

    So: assert the decorator is on the function, for every endpoint something
    outside this process calls."""

    ENDPOINTS = ("estimate_preview", "cost_card", "import_parts_csv")

    def test_the_endpoints_the_plugin_calls_are_whitelisted(self):
        for name in self.ENDPOINTS:
            fn = getattr(api, name, None)
            self.assertIsNotNone(fn, "api.%s has gone" % name)
            self.assertIn(fn, frappe.whitelisted,
                          "api.%s is not whitelisted — an HTTP caller gets 403 "
                          "while every in-process test still passes" % name)
