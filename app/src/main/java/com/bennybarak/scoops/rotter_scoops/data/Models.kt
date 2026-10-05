package com.bennybarak.scoops.rotter_scoops.data

import org.json.JSONObject

/** A scoop thread as it appears in the RSS list. [published] is epoch millis. */
data class Scoop(
    val id: String,
    val title: String,
    val url: String,
    val published: Long? = null,
) {
    // Persisted so the list paints from disk at launch, and so saved/followed
    // threads outlive the rolling RSS feed. Same shape as the Flutter build's.
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("url", url)
        if (published != null) put("published", published)
    }

    companion object {
        fun fromJson(j: JSONObject) = Scoop(
            id = j.getString("id"),
            title = j.getString("title"),
            url = j.getString("url"),
            published = if (j.isNull("published")) null else j.getLong("published"),
        )
    }
}

/** A single message in a thread (root = num 0, comments otherwise). */
data class Message(
    val num: Int,
    val author: String,
    val rating: Int? = null, // member rank, 1..5 stars (registered users), or null
    val profileUrl: String? = null, // rotter member page (ratings/details), or null
    // Member stats shown beside the author (parsed from the thread HTML).
    val joinDate: String? = null, // e.g. "10.11.23"
    val messages: Int? = null, // total posts on rotter (הודעות)
    val raters: Int? = null, // how many members rated them (מדרגים)
    val points: Int? = null, // reputation points (נקודות)
    val title: String? = null,
    val bodyHtml: String? = null, // cleaned rich HTML; null for headline-only posts
    val date: String? = null, // gregorian DD.MM.YY as shown
    val time: String? = null, // HH:MM
    val timestamp: Long? = null, // parsed from date+time (comments only), epoch millis
    val parent: Int? = null, // null on root; 0 = replies to root; else another num
) {
    val isRoot get() = num == 0
}

/** A parsed thread: its messages plus a children index for tree rendering. */
class Thread(val id: String, val messages: List<Message>) {
    val root: Message? = messages.firstOrNull { it.isRoot }
    val comments: List<Message> = messages.filter { !it.isRoot }

    // Built once: the flatten walk asks for every node's children.
    private val children: Map<Int, List<Message>> =
        comments.groupBy { it.parent ?: 0 }.mapValues { (_, v) -> v.sortedBy { it.num } }

    /** Children of a given message num, ordered by num. */
    fun childrenOf(parentNum: Int): List<Message> = children[parentNum] ?: emptyList()
}
