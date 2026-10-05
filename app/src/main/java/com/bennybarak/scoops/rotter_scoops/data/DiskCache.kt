package com.bennybarak.scoops.rotter_scoops.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONTokener
import java.io.File

/**
 * A small JSON file under the app's cache directory, for data the app can
 * always refetch (the last feed, per-card metadata).
 *
 * Not [Prefs], where the rest of the app's state lives: that is loaded whole
 * into memory at launch, the wrong home for ~100KB of metadata that only exists
 * to save a network round trip. The cache directory is also the one the OS may
 * purge under storage pressure — right for regenerable data. Same directory and
 * file names as the Flutter build, so its cache carries over.
 */
object DiskCache {
    var dir: File? = null

    fun readNow(name: String): Any? = try {
        val f = File(dir ?: return null, name)
        if (!f.exists()) null else JSONTokener(f.readText()).nextValue()
    } catch (_: Exception) {
        null // missing / corrupt → just refetch
    }

    suspend fun read(name: String): Any? = withContext(Dispatchers.IO) { readNow(name) }

    suspend fun write(name: String, json: String) = withContext(Dispatchers.IO) {
        try {
            val f = File(dir ?: return@withContext, name)
            val tmp = File(f.path + ".tmp")
            tmp.writeText(json)
            tmp.renameTo(f) // atomic replace
        } catch (_: Exception) { /* best-effort */ }
    }
}
