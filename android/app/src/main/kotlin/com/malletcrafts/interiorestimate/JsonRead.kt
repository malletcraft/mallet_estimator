package com.malletcrafts.interiorestimate

import com.malletcrafts.interiorestimate.pano.AlbumWire
import org.json.JSONArray
import org.json.JSONObject

/**
 * org.json's tree read through :pano's [AlbumWire.Read] (port 61c slice PL), so S5's answers and the phone's own stored
 * copies are read by :pano's tested rules rather than by a second reader here: QuoteStageSync reads S5's list rows
 * (QuoteStage.listedOf, S5-L's `work_states` included) and its stored copies (QuoteStage.storedOf) with it, and the
 * album's answers will (QV). The contract AlbumWireTest proves on org.json's shapes: JSON null -- `JSONObject.NULL`,
 * which org.json keeps for a null value -- reads as null, exactly as an absent key does; a number stays the Integer,
 * Long or Double org.json made of it ([AlbumWire] reads every one); only a [JSONObject] is an object.
 */
object JsonRead : AlbumWire.Read {

    override fun field(o: Any?, key: String): Any? =
        (o as? JSONObject)?.opt(key)?.takeIf { it !== JSONObject.NULL }

    override fun items(a: Any?): List<Any?>? =
        (a as? JSONArray)?.let { arr -> List(arr.length()) { i -> arr.opt(i)?.takeIf { it !== JSONObject.NULL } } }

    override fun isObject(v: Any?): Boolean = v is JSONObject
}
