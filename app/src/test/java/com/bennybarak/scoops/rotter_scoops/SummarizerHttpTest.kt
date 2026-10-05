package com.bennybarak.scoops.rotter_scoops

import com.bennybarak.scoops.rotter_scoops.net.DigestItem
import com.bennybarak.scoops.rotter_scoops.net.ThreadSummarizer
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress

class SummarizerHttpTest {
    private val ok = """{"choices":[{"message":{"content":"## נושא\n- **עיקר**"}}]}"""

    /** A local stand-in for the chat-completions endpoint. */
    private fun server(handle: (body: String) -> Pair<Int, String>): Pair<HttpServer, String> {
        val srv = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        srv.createContext("/v1/chat/completions") { ex ->
            val body = ex.requestBody.readBytes().decodeToString()
            val (code, resp) = handle(body)
            val bytes = resp.toByteArray()
            ex.sendResponseHeaders(code, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        srv.start()
        return srv to "http://127.0.0.1:${srv.address.port}/v1"
    }

    private fun summarizer(base: String) = ThreadSummarizer("k", "gpt-5.4-mini", base, "Hebrew")
    private val items = listOf(DigestItem("12:00", "כותרת", "<p>גוף</p>"))

    // The model answers only after thinking: longer than OkHttp's default 10s.
    @Test fun slowAnswerIsWaitedFor() = runBlocking {
        val (srv, base) = server { Thread.sleep(12_000); 200 to ok }
        try {
            assertTrue(summarizer(base).summarizeDigest(items, "60 minutes").contains("עיקר"))
        } finally {
            srv.stop(0)
        }
    }

    @Test fun reasoningEffortIsDroppedWhenRejected() = runBlocking {
        val sent = ArrayList<String>()
        val (srv, base) = server { body ->
            sent.add(body)
            if (body.contains("reasoning_effort")) {
                400 to """{"error":{"message":"Unsupported parameter: 'reasoning_effort'"}}"""
            } else {
                200 to ok
            }
        }
        try {
            assertTrue(summarizer(base).summarizeDigest(items, "60 minutes").contains("עיקר"))
            assertEquals(2, sent.size)
            assertTrue(sent[0].contains("\"reasoning_effort\":\"low\""))
            assertFalse(sent[1].contains("reasoning_effort"))
        } finally {
            srv.stop(0)
        }
    }
}
