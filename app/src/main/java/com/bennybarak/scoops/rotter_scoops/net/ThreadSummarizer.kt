package com.bennybarak.scoops.rotter_scoops.net

import com.bennybarak.scoops.rotter_scoops.data.Thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class SummaryException(override val message: String) : Exception(message)

/** One scoop for the digest: its posting time (HH:MM), headline and main post. */
class DigestItem(
    val time: String,
    val title: String,
    val bodyHtml: String?,
    val id: String = "", // the scoop's thread id
    val published: Long? = null,
)

/** A scoop a digest drew on: its reference number in the transcript ([n]) and thread. */
data class DigestSource(val ref: Int, val id: String, val title: String, val published: Long?)

/** `[3]` / `[3, 7]` references the model puts at the end of a digest bullet. */
private val refsRe = Regex("""\s*\[(\d+(?:\s*,\s*\d+)*)\]""")

/** A digest line's text without its scoop references, and those references. */
fun splitRefs(text: String): Pair<String, List<Int>> {
    val refs = refsRe.findAll(text).flatMap { m -> m.groupValues[1].split(',').mapNotNull { it.trim().toIntOrNull() } }
        .distinct().toList()
    return text.replace(refsRe, "").trim() to refs
}

/**
 * Summarizes a thread (original post + comments) with an OpenAI-compatible
 * chat-completions model. Entirely opt-in: it only runs when the user turned it
 * on and supplied their own key, because it sends the thread's text to a third
 * party.
 */
class ThreadSummarizer(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String,
    /** English name of the answer language for the prompt ("Hebrew"/"English"). */
    private val language: String,
) {
    companion object {
        /**
         * The model sends nothing until it has finished writing, which for a
         * digest of many scoops takes well over OkHttp's default 10s read
         * timeout — that cut every larger request off. Wait up to 3 minutes.
         */
        private val aiClient = Http.client.newBuilder()
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(180, TimeUnit.SECONDS)
            .build()

        /**
         * Threads run to hundreds of comments; the transcript is capped and the
         * OLDEST comments kept — they carry the substance, later ones tend to argue.
         */
        private const val MAX_TRANSCRIPT_CHARS = 12000

        private fun systemPrompt(language: String) = """
You summarize discussion threads from rotter.net, an Israeli news forum. You will be given the original post followed by its comments.

Write the summary in $language.

Format it as Markdown with exactly these three sections, each introduced by a `## ` heading translated into $language:

## The post
Two or three sentences on what the original post claims.

## The responses
Bullet points, one per position taken in the thread — grouped by argument rather than listed comment by comment. Note where commenters disagree with the post or with each other.

## What's new
Bullet points for anything presented as new information, each saying whether the thread corroborates it.

Use `-` for bullets and `**bold**` for the few phrases that matter most. Do not use tables, code blocks or headings other than the three above. Keep the whole summary under 250 words.

Do not invent details that are not in the text. rotter posts are often unverified rumor — describe claims as claims, and say so plainly when the thread itself disputes them.""".removePrefix("\n")

        private val ws = Regex("""\s+""")
        private fun plain(html: String) =
            Jsoup.parseBodyFragment(html).body().wholeText().replace(ws, " ").trim()

        private fun digestPrompt(language: String, window: String) = """
You summarize the latest scoops (breaking-news posts) from rotter.net, an Israeli news forum. You will be given the main posts published in the last $window, newest first, each with its posting time, headline and text. Comments are not included.

Write the summary in $language.

Format it as Markdown. Group related scoops into topics, at most six. Give each topic a `## ` heading in $language, then `-` bullets, one per distinct development, newest first, starting with its posting time (HH:MM). Use `**bold**` for the key fact of each bullet. Merge scoops that report the same thing. Each scoop in the input starts with a reference number in square brackets, like [3]. End every bullet with the reference numbers of the scoops it is based on, in square brackets, e.g. [3] or [3, 7], and use them nowhere else. Do not use tables, code blocks or other headings. Keep the whole digest under 400 words, not counting the references.

Do not invent details that are not in the posts. rotter scoops are often unverified first reports — describe them as reports, and say so plainly when posts contradict each other.""".removePrefix("\n")

        /**
         * The transcript budget, shared out evenly so every headline makes it
         * in: a few scoops get up to 700 characters of post each, a day's worth
         * get less (never under 150).
         */
        private const val MAX_DIGEST_CHARS = 24000

        /** Items are numbered from 1 in order; [n] is how the model refers back to them. */
        fun digestTranscript(items: List<DigestItem>): String {
            val out = StringBuilder()
            val perItem = (MAX_DIGEST_CHARS / maxOf(1, items.size) - 60).coerceIn(150, 700)
            for ((i, it) in items.withIndex()) {
                val body = it.bodyHtml?.let(::plain)?.take(perItem) ?: ""
                val entry = "[${i + 1}] ${it.time} ${it.title}\n$body".trim() + "\n\n"
                if (out.length + entry.length > MAX_DIGEST_CHARS) break
                out.append(entry)
            }
            return out.toString().trim()
        }

        fun transcript(thread: Thread, title: String): String {
            val lines = arrayListOf("POST: $title")
            thread.root?.let { root ->
                lines.add("BY: ${root.author}")
                root.bodyHtml?.let { lines.add(plain(it)) }
            }
            lines.add("\nCOMMENTS:")
            var budget = MAX_TRANSCRIPT_CHARS
            for (c in thread.comments) {
                val entry = "- ${c.author}: ${c.title ?: ""} ${c.bodyHtml?.let(::plain) ?: ""}".trim()
                if (entry.length >= budget) break
                budget -= entry.length
                lines.add(entry)
            }
            return lines.joinToString("\n")
        }
    }

    suspend fun summarize(thread: Thread, title: String): String =
        complete(systemPrompt(language), transcript(thread, title))

    /** A digest of the scoops posted in the last [window] (main posts only). */
    suspend fun summarizeDigest(items: List<DigestItem>, window: String): String =
        complete(digestPrompt(language, window), digestTranscript(items))

    private suspend fun complete(system: String, user: String): String = withContext(Dispatchers.IO) {
        try {
            request(system, user, lowEffort = true)
        } catch (e: SummaryException) {
            // An endpoint or model that doesn't take `reasoning_effort` says so
            // in its error: ask once more without it.
            if (!e.message.contains("reasoning", ignoreCase = true)) throw e
            request(system, user, lowEffort = false)
        }
    }

    private suspend fun request(system: String, user: String, lowEffort: Boolean): String {
        val root = baseUrl.trim().replaceFirst(Regex("/+$"), "")
        val url = "$root/chat/completions"
        val payload = JSONObject()
            .put("model", model)
            // No `temperature`: reasoning models reject it.
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", system))
                    .put(JSONObject().put("role", "user").put("content", user)),
            )
        // A summary needs little deliberation; this is most of the latency.
        if (lowEffort) payload.put("reasoning_effort", "low")
        val req = try {
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
        } catch (_: IllegalArgumentException) {
            throw SummaryException("Invalid endpoint")
        }
        aiClient.newCall(req).await().use { res ->
            val json = try {
                JSONObject(res.body.string())
            } catch (_: Exception) {
                null
            }
            if (res.code != 200) {
                // A bad key or unknown model comes back as a JSON error body — its
                // message beats a bare status, since the model name is user-editable.
                val msg = json?.optJSONObject("error")?.opt("message") as? String
                throw SummaryException(msg ?: "HTTP ${res.code}")
            }
            val text = json?.optJSONArray("choices")?.optJSONObject(0)
                ?.optJSONObject("message")?.opt("content") as? String
            if (text.isNullOrBlank()) throw SummaryException("")
            return text.trim()
        }
    }
}
