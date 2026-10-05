package com.bennybarak.scoops.rotter_scoops.data

import android.content.Context

/** Where [Prefs] keeps its values — SharedPreferences in the app, a map in tests. */
interface KeyValue {
    fun get(key: String): Any?
    fun put(key: String, value: Any?)
}

/**
 * App preferences, stored EXACTLY where and how the Flutter build's
 * `shared_preferences` plugin kept them — the `FlutterSharedPreferences` file,
 * every key prefixed `flutter.`, doubles as a prefixed string — so an update
 * from the Flutter app keeps the user's settings, read state, saved scoops and
 * drafts with no migration step.
 */
object Prefs {
    private const val FILE = "FlutterSharedPreferences"
    private const val KEY_PREFIX = "flutter."
    private const val DOUBLE_PREFIX = "VGhpcyBpcyB0aGUgcHJlZml4IGZvciBEb3VibGUu"

    lateinit var backend: KeyValue

    fun init(context: Context) {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        // One snapshot at launch, kept in step with every write: `sp.all`
        // copies the whole map, which a read per lookup would do every time.
        val mirror = HashMap<String, Any?>(sp.all)
        backend = object : KeyValue {
            override fun get(key: String): Any? = mirror[key]
            override fun put(key: String, value: Any?) {
                if (value == null) mirror.remove(key) else mirror[key] = value
                val e = sp.edit()
                when (value) {
                    null -> e.remove(key)
                    is Boolean -> e.putBoolean(key, value)
                    is String -> e.putString(key, value)
                    is Long -> e.putLong(key, value)
                    else -> error("unsupported pref type")
                }
                e.apply()
            }
        }
    }

    fun getString(key: String): String? = backend.get(KEY_PREFIX + key) as? String
    fun getBool(key: String): Boolean? = backend.get(KEY_PREFIX + key) as? Boolean
    fun getDouble(key: String): Double? {
        val v = backend.get(KEY_PREFIX + key) as? String ?: return null
        return if (v.startsWith(DOUBLE_PREFIX)) v.substring(DOUBLE_PREFIX.length).toDoubleOrNull() else null
    }

    fun setString(key: String, value: String) = backend.put(KEY_PREFIX + key, value)
    fun setBool(key: String, value: Boolean) = backend.put(KEY_PREFIX + key, value)
    fun setDouble(key: String, value: Double) = backend.put(KEY_PREFIX + key, DOUBLE_PREFIX + value)
    fun remove(key: String) = backend.put(KEY_PREFIX + key, null)
}

/** In-memory [KeyValue], for unit tests. */
class MapKeyValue : KeyValue {
    private val map = HashMap<String, Any?>()
    override fun get(key: String): Any? = map[key]
    override fun put(key: String, value: Any?) {
        if (value == null) map.remove(key) else map[key] = value
    }
}
