package com.bennybarak.scoops.rotter_scoops.net

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Recently downloaded thread pages, gzipped (~25–45 KB each instead of
 * 100–400 KB): in memory, least recently used dropped first past
 * [MAX_BYTES], and in the cache directory so they outlive the process (the
 * list reuses its saved card data after a restart instead of re-downloading
 * pages). The list fetches every thread's page for its card metadata; keeping
 * it means opening a thread doesn't wait on rotter's server (150–320 ms
 * before the first byte, measured) before showing anything.
 */
object PageCache {
    private const val MAX_BYTES = 4 * 1024 * 1024
    private const val MAX_DISK_BYTES = 6L * 1024 * 1024

    private class Entry(val at: Long, val gz: ByteArray)

    private val entries = LinkedHashMap<String, Entry>(64, 0.75f, true) // access order
    private var total = 0

    /** Where pages are kept on disk; null keeps them in memory only (tests). */
    var dir: File? = null

    /** Clock, replaceable in tests. */
    var now: () -> Long = System::currentTimeMillis

    /** Call off the main thread: compresses and writes a file. */
    fun put(id: String, page: ByteArray) {
        val gz = ByteArrayOutputStream(page.size / 4).also { out ->
            GZIPOutputStream(out).use { it.write(page) }
        }.toByteArray()
        val at = now()
        synchronized(this) {
            entries.remove(id)?.let { total -= it.gz.size }
            entries[id] = Entry(at, gz)
            total += gz.size
            val it = entries.entries.iterator()
            while (total > MAX_BYTES && it.hasNext()) {
                total -= it.next().value.gz.size
                it.remove()
            }
        }
        writeToDisk(id, gz, at)
    }

    /** The page if it was fetched within [maxAgeMs], else null. Call off the main thread. */
    fun get(id: String, maxAgeMs: Long): ByteArray? {
        val e = synchronized(this) { entries[id] } ?: readFromDisk(id) ?: return null
        if (now() - e.at > maxAgeMs) return null
        return GZIPInputStream(e.gz.inputStream()).use { it.readBytes() }
    }

    fun clear() = synchronized(this) {
        entries.clear()
        total = 0
    }

    private fun file(id: String): File? {
        if (!id.all { it.isDigit() }) return null
        return dir?.let { File(it, "$id.gz") }
    }

    private fun writeToDisk(id: String, gz: ByteArray, at: Long) {
        val f = file(id) ?: return
        try {
            f.parentFile?.mkdirs()
            val tmp = File(f.path + ".tmp")
            tmp.writeBytes(gz)
            tmp.setLastModified(at)
            tmp.renameTo(f)
            trimDisk()
        } catch (_: Exception) { /* best-effort */ }
    }

    private fun readFromDisk(id: String): Entry? {
        val f = file(id) ?: return null
        return try {
            if (!f.exists()) null else Entry(f.lastModified(), f.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    /** Oldest files go first once the folder passes [MAX_DISK_BYTES]. */
    private fun trimDisk() {
        val files = dir?.listFiles { f -> f.name.endsWith(".gz") } ?: return
        var size = files.sumOf { it.length() }
        if (size <= MAX_DISK_BYTES) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (size <= MAX_DISK_BYTES) break
            size -= f.length()
            f.delete()
        }
    }
}
