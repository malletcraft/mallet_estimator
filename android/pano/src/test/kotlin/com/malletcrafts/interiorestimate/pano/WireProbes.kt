package com.malletcrafts.interiorestimate.pano

import com.malletcrafts.interiorestimate.pano.i18n.Msg
import com.malletcrafts.interiorestimate.pano.i18n.Tx

// Fixtures for MastersTest's wire check, OUTSIDE the i18n package so a class's own name is never a hit.
// WireLeakProbe holds the two shapes a parameter-type check on erased types missed: a Msg parameter
// (Function1 once erased) and a body that reads Tx.MR with no i18n parameter at all.

internal object WireLeakProbe {
    fun shown(m: Msg): String = m(Tx.EN)
    fun face(): String = Tx.MR.app_display
}

internal object WireCleanProbe {
    fun body(name: String): String = "{\"name\":\"" + name.trim() + "\"}"
}
