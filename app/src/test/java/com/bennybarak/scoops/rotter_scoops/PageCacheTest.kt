package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.net.PageCache
import com.bennybarak.scoops.rotter_scoops.net.RotterService
import com.bennybarak.scoops.rotter_scoops.net.decodeWin1255
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class PageCacheTest {
    private var clock = 0L

    @After fun tearDown() {
        PageCache.clear()
        PageCache.now = System::currentTimeMillis
    }

    @Test fun roundTripAndExpiry() {
        PageCache.now = { clock }
        val page = "שלום <b>rotter</b>".repeat(5000).toByteArray()
        PageCache.put("1", page)
        assertArrayEquals(page, PageCache.get("1", 60_000))
        clock += 61_000
        assertNull(PageCache.get("1", 60_000))
    }

    @Test fun boundedByCompressedSize() {
        PageCache.now = { clock }
        // Incompressible 1 MB pages: only the most recent few fit in 4 MB.
        val pages = (1..8).map { Random(it).nextBytes(1_000_000) }
        pages.forEachIndexed { i, p -> PageCache.put("$i", p) }
        assertNull(PageCache.get("0", Long.MAX_VALUE))
        assertArrayEquals(pages[7], PageCache.get("7", Long.MAX_VALUE))
    }

    @Test fun cachedThreadParsesTheStoredPage() = runBlocking {
        val bytes = javaClass.classLoader!!.getResourceAsStream("fixtures/thread-960077.html")!!.readBytes()
        PageCache.put("960077", bytes)
        val t = RotterService.cachedThread("960077", 60_000)!!
        assertEquals(RotterService.parseThread(decodeWin1255(bytes), "960077").messages, t.messages)
        assertNull(RotterService.cachedThread("1", 60_000))
    }
}
