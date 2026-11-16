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
     *  - FILING BY WORK AFTER APPROVAL: a new capture stays the Quote journey's, the Capture 360 sheet's WORK row
     *    takes no tap and its buttons never wait for a pick, a single photo is not handed to S5's album, and the
     *    photo's WORK line re-files nothing ([Album.execution]); the row and the line say where the capture is filed
     *    and nothing more ([Album.workSub]);
     *  - the rooms grid's dot keeps the survey's rule after approval ([QuoteStage.roomDot]);
     *  - the workers that serve only these: the album outbox's drain (AlbumOutbox, AlbumDrainWorker) and the
     *    re-file by work with its sweep and its refusal line (WorkRefile, WorkRefileWorker, AsIsUpload.refileRefused).
     *    What a phone already holds queued STAYS queued, untouched, and goes when this is on again.
     *
     * What it does NOT touch -- the survey to quote journey: survey prep and its wish marks, the wish list, the phase
     * strip and its moves, Approve with its scope sheet, the approved quote view with its APPROVED SCOPE row, every
     * upload, and the album MIRROR's read (AlbumSync), which the Approve and the quote view draw S5's role and scope
     * from. The room screen's headings stay the filing's name, as they are before approval.
     */
    const val SITE_PROGRESS: Boolean = false

    /**
     * THE HELP TOURS (Amit, 15 Nov 2026: "For time being hold help tour as well from development as it can be done
     * once I approve survey to quote journey its functionality etc."). Still ON: the survey tour on the phone since
     * 0.3.318 stays until the measuring screen changes, and is switched off -- code kept -- in the SAME build that
     * ships the new screen (slice M2), so it never teaches a screen that is gone. Nothing reads this yet: slice SP0
     * only gives the switch its home.
     */
    const val HELP_TOURS: Boolean = true
}
