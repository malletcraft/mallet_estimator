package com.malletcrafts.interiorestimate.pano

/**
 * THE FOUR-STEP ROUTE'S SCOPES, KEPT FOR THE PLACING RULE'S OWN TESTS (port 63 slice I1b). The screen has no scope any
 * more -- no `all`, no 3 · Measure: every mark shows every figure it has been read and the picked one everything it
 * asks (PhotoFigures, DimLayout.placeShown) -- so [Scope] and `place` left :pano with it. The placing rule itself
 * ([DimLayout.placeBy], the level-plate form [DimLayout.placeShown] shares) is what these tests pin, case by case, and
 * they were written in scopes: this restates the three, as they were, in the tests' own source.
 */
object OldPlace {
    /** Which marks showed which figures: 2 · Mark (the picked mark), 3 · Measure (every size and the picked mark's
     *  position), and 3 · Measure with `all` on. */
    enum class Scope { PICKED, FACE, ALL }

    /** Whether a figure of [role] was drawn in [scope], on the [picked] mark or on another. */
    fun shows(scope: Scope, picked: Boolean, role: MarkDims.Role): Boolean = when (scope) {
        Scope.PICKED -> picked
        Scope.FACE -> picked || DimLayout.isSize(role)
        Scope.ALL -> true
    }

    /** The four-step route's `place`: [DimLayout.placeBy] with [scope]'s figures, the picked mark's handles kept clear
     *  in 2 · Mark. */
    fun place(marks: List<DimLayout.Mark>, scope: Scope, width: Double, height: Double, keep: List<SurveyPrepTouch.Box>,
              u: Double, dp: Double, times: Double = 0.0): DimLayout.Out =
        DimLayout.placeBy(marks, width, height, keep, u, dp, times, { mk, f -> shows(scope, mk.picked, f.role) },
            { mk -> scope == Scope.PICKED && mk.picked && (mk.tabs || mk.corners != null) })
}
