package com.malletcrafts.interiorestimate.pano

/**
 * THE APP'S FEATURE SWITCHES (port 63 slice SP0, 16 Nov 2026): one constant per feature, and every gate of that
 * feature reads THAT constant -- in :app directly at the feature's entry, in :pano as the default of a pure rule's
 * last parameter, so both sides of the rule stay tested whichever way the switch stands. Turning a feature back on
 * is the ONE line here (and the one test that pins the line, FeaturesTest); nothing is deleted while it is off.
 *
 * Why constants and not a preference, a build flavour or a remote flag: a switch somebody can set on a phone is a
 * second answer to "is this feature on" (the rule this app keeps paying for breaking), a flavour would be a second
 * build to keep green, and there is nobody to flip a remote flag for one phone. `const val` is inlined, so the code
 * behind a switch that is off costs nothing at run time and is still compiled, by CI, on every build.
 */
object Features {

    /**
     * SITE PROGRESS -- everything that belongs to EXECUTION, after the client approves the estimate -- IS OFF (Amit,
     * 15 Nov 2026, after his first use of the app on a client site: "Lets focus on survey to quote approval workflow
     * first. Site progress will do post I confirm that survey to quote looks good. Do not delete any site progress
     * code but toggle it as feature flag.").
     *
     * What it hides, each at its entry (FeaturesTest lists and pins every one):
     *  - the project ALBUM: its tile in the rooms grid, and with it the screen, the chapters by work, the works'
     *    states, the Project Manager's writes, Accept / Not for client, the change requests and the progress shot
     *    (MainActivity's `albumTile`: no tile, so the grid closes up and nothing else opens the album);
     *  - the EXECUTION LINE: at Client approved the project's one line reads `Quote · Client approved` over `since …`
     *    instead of `Execution · 3 in progress · 2 done` -- on the project screen, the project list's rows and the
     *    Wish list's bar ([QuoteStage.projectLine]) -- and survey prep's band reads `Client approved` alone
     *    ([QuoteStage.bandLine]);
     *  - FILING BY WORK: a new capture stays the Quote journey's after approval, the Capture 360 sheet's buttons
     *    never wait for a pick, a single photo is not handed to S5's album, and the photo's WORK line re-files
     *    nothing ([Album.execution]). The sheet's WORK ROW IS NOT DRAWN, at any stage, and the photo's WORK LINE only
     *    for a capture already filed to one of the sixteen works ([Album.workRowShown]; SP0 review: the row could
     *    only ever read `Quote journey`, and before approval its `locked until the client approves the estimate`
     *    promised a pick that never came). Where the line is drawn it says where the capture is filed and nothing
     *    more, on both sides of approval: neither the lock nor `tap to change` ([Album.workSub]);
     *  - the rooms grid's dot keeps the survey's rule after approval ([QuoteStage.roomDot]);
     *  - the workers that serve only these: the album outbox's drain (AlbumOutbox, AlbumDrainWorker) and the
     *    re-file by work with its sweep and its refusal line (WorkRefile, WorkRefileWorker, AsIsUpload.refileRefused).
     *    What a phone already holds queued STAYS queued, untouched, and goes when this is on again.
     *
     * What it does NOT touch -- the survey to quote journey: survey prep and its wish marks, the wish list, the phase
     * strip and its moves, Approve with its scope sheet, the approved quote view with its APPROVED SCOPE row, every
     * upload, and the album MIRROR's read (AlbumSync), which the Approve and the quote view draw S5's role and scope
     * from. The room screen's headings stay the filing's name, as they are before approval.
     *
     * STILL SAID WHILE IT IS OFF, AND OPEN FOR AMIT'S RULING (SP0 review, finding 2 -- nothing here decides it): the
     * approval lock's own words name the change requests, which are raised and answered only in the album and so
     * cannot be reached from the phone while this is off -- the Wish list's banner and the Mark sheet's line at
     * Client approved (`Approved — changes go through change requests`, [WishList.lockedLine], [PhaseTools.gateText]),
     * the scope sheet's three notes ([QuoteView.note]) and the approved chip's TalkBack label (`… in execution`,
     * [QuoteView.approvedChipLabel]). They are left as they are, by no rule's oversight: the banner's words are the
     * ones Amit chose for the lock (28 Oct 2026) and S5's APPROVED_FINAL verbatim, the notes are S5 /projects' own,
     * and S5's 409 goes on saying the same whatever the phone draws -- so a second wording is his to rule on, and
     * any new text would be English only (android/i18n/mr-pending.tsv) until he has verified the journey.
     */
    const val SITE_PROGRESS: Boolean = false

    /**
     * THE HELP TOURS ARE OFF (Amit, 15 Nov 2026: "For time being hold help tour as well from development as it can be
     * done once I approve survey to quote journey its functionality etc."). The survey tour, on the phone since
     * 0.3.318, stayed until the measuring screen changed, and went off -- code kept -- in the SAME build that ships
     * the new screen (port 63 slice M2, 16 Nov 2026), so it never teaches a screen that is gone: its step 11 teaches
     * the list, which that build removes.
     *
     * What it hides, each at its entry (FeaturesTest pins every one):
     *  - the SHIPPED SET: [TourFlows.shipped] gives nothing, so no tour can be listed or started;
     *  - the drawer's HELP ROW (MainActivity's settings groups): it would read "0 tours", so its group is not drawn,
     *    and with it the Help screen has no way in;
     *  - the DEMO CLIENT's seeding on a fresh install (TourSeed.ensure at MainActivity's start): the demo flat exists
     *    only for the tours. A Demo Client a phone already holds is left alone.
     * Nothing is deleted: the seven flows and their texts, TourMachine, TourOverlay, the anchors on the screens,
     * survey prep's TourBindSurvey and the Help screen are all still there, and compiled on every build. The survey
     * flow's `mode:list` anchor names a step the screen no longer has; it is for whoever rewrites the tour once Amit
     * has approved the journey (DESIGN-port63 section 2).
     */
    const val HELP_TOURS: Boolean = false

    /**
     * THE CHAIN TOOL IS OFF (Amit, 16 Nov 2026, with build 0.3.337 -- the first with port 63's measuring screen -- on
     * his phone: "chain mark is rarely used. get rid of it."; port 63 slice F1). No way to DRAW A NEW CHAIN is offered
     * anywhere on the phone. Every mark type stays: he asked to drop this one tool.
     *
     * What it hides, each at its entry (FeaturesTest pins every one; all three are pure rules of :pano that take this
     * constant as the default of their last parameter, and :app asks them with no answer of its own):
     *  - the PALETTE'S SLOT: [SurveyPrepPalette.offered] leaves Chain out, so 2 · Mark's row is Select, Tape, Line,
     *    three types and More ▸ -- seven slots sharing the width, no gap where the eighth stood ([SurveyPrepPalette.slots]).
     *    More ▸ lists mark types only and never held it;
     *  - THE TOOL THAT IS ON: [PhaseTools.toolFor] answers Select for the Chain's key, whatever was last picked -- so
     *    on 2 · Mark and on 4 · 360 no chip counts its points, no tap adds one, no ✓ ends one, and none of the words
     *    that belong to drawing one is ever said;
     *  - DUPLICATE ON A CHAIN: [PhaseTools.copyOk] refuses it and [SurveyPrepEdit.menuItemsOf] hides the row -- a copy
     *    of a chain is a new chain.
     *
     * What it does NOT touch -- A ROOM THAT ALREADY HAS A CHAIN KEEPS IT. The file reads and writes it unchanged
     * (SurveyPrepFile); it is drawn on the photo, the 360 and the Plan; a tap picks it; its points still move; its
     * segments stay in 3 · Measure's walk and in every count, and take the D2 and the pad as before; it locks, and it
     * can be DELETED (one ↶ brings it back). Hiding a surveyor's existing readings would be a second answer about the
     * room. Nothing is deleted from the code either: SurveyChain, the chain's branches of the two gesture loops, its
     * drawing and its texts are all still there, compiled on every build, and reached again by the ONE line here.
     *
     * STILL WRITTEN, AND NOT SHOWN: the guided tours' flow `existing` lists the palette as "Select, Tape, Line, Chain,
     * …" (tour.existing.1.body). The tours are off ([HELP_TOURS]) and that flow never shipped; whoever rewrites the
     * tours once Amit has approved the journey drops the word.
     */
    const val CHAINS: Boolean = false

    /**
     * THE TAPE TOOL IS OFF (Amit, 23 Nov 2026, ~22:00 IST, with build 0.3.350 on his phone: "does tape tool make any
     * sense now? as we don't want any foto measured dimensions on fone."; port 63 slice F3). A Tape's figure IS what the
     * photo says -- that was the tool -- so it has no place on a phone that shows no figure nobody measured
     * (DESIGN-imagemeter §18, whose rule 4 kept the Tape "until he rules"; he has now asked, and the answer here is
     * CLAUDE'S, to be turned round by this one line: §24). A length the surveyor wants is a Line, which takes a reading.
     *
     * What it hides, each at its entry (FeaturesTest pins every one; every rule is a pure function of :pano that takes
     * this constant as the default of its last parameter, and :app asks them with no answer of its own):
     *  - NO WAY TO LAY A NEW TAPE: [SurveyPrepPalette.offered] leaves the Tape out, so 2 · Mark's row is Line, three
     *    types, More ▸ and `▸` -- six slots sharing the width ([SurveyPrepPalette.row]); More ▸ lists mark types only
     *    and never held it; [PhaseTools.toolFor] answers Select for the Tape's key, whatever was last picked, so on
     *    2 · Mark and on 3 · 360 no drag pulls one, no chip names one and none of the words of pulling one is said; and
     *    Duplicate on a Tape is refused and its act not drawn ([PhaseTools.copyOk], [SurveyPrepEdit.menuItemsOf],
     *    [MarkSheet.acts]) -- a copy of a Tape is a new Tape;
     *  - NOTHING MEASURED FROM THE PHOTO IS DRAWN ON A TAPE A ROOM ALREADY HAS, on the screen's own painters -- the
     *    editing photo and the 360: no length figure ([PhotoFigures.tape], [SurveyMeas.tapeFigure]: nothing is handed
     *    to the figures' layout, [PhotoFigures.items]) and no graduations ([SurveyMeas.ticksOf]: a tick every 100 mm
     *    and "500", "1000" beside it are the photo's figures as much as the length is). What is left is its plain
     *    band; its code is where every picked item's is, on its chip.
     *
     * What it does NOT touch -- A ROOM THAT ALREADY HAS A TAPE KEEPS IT. The file reads and writes it unchanged
     * (SurveyPrepFile); it is drawn; a tap picks it; it moves by its band and by its ends, and under Fine move's pad;
     * its panel opens (Lock, Lock all, Fine move, Delete); it locks; it is DELETED, and one ↶ brings it back. Nothing
     * is deleted from the code: SurveyMeas' Tape, its drag, its ticks, its tag, its icon and its texts are all still
     * there, compiled on every build, and reached again by the ONE line here.
     *
     * NOT CHANGED BY THIS SWITCH: a SAVED PICTURE'S old painter (the `dims` mode of drawMeas: band, graduations and
     * "F-T1 1234 mm", [SurveyMeas.tapeTicks] and [SurveyMeas.tapeTag] as they were) -- it goes with the saved pictures'
     * own slice, M3 ("Measured figures only"); and S5, which draws a Tape's band and its photo length for the office.
     *
     * STILL WRITTEN, AND NOT SHOWN: the guided tours' flow `existing` lists the palette as "Select, Tape, Line, Chain,
     * …" (tour.existing.1.body). The tours are off ([HELP_TOURS]) and that flow never shipped.
     */
    const val TAPES: Boolean = false
}
