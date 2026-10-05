package com.bennybarak.scoops.rotter_scoops.net

/**
 * Windows-1255 (Hebrew) codec. rotter.net serves and reads everything as cp1255;
 * this is the exact table the Flutter build used (bytes it leaves undefined
 * decode to U+FFFD), so text decodes byte-for-byte the same.
 */
fun decodeWin1255(bytes: ByteArray): String {
    val out = CharArray(bytes.size)
    for (i in bytes.indices) {
        val b = bytes[i].toInt() and 0xFF
        out[i] = if (b < 0x80) b.toChar() else HIGH[b - 0x80].toChar()
    }
    return String(out)
}

/**
 * Decode a cp1255 percent-encoded value (rotter encodes usernames in URLs this
 * way, e.g. `%E0%F8%E8` → Hebrew), not UTF-8.
 */
fun decodeWin1255Percent(s: String): String {
    val bytes = java.io.ByteArrayOutputStream()
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (c == '%' && i + 2 < s.length) {
            val h = s.substring(i + 1, i + 3).toIntOrNull(16)
            if (h != null) {
                bytes.write(h)
                i += 3
                continue
            }
        }
        bytes.write(if (c == '+') 0x20 else c.code)
        i++
    }
    return decodeWin1255(bytes.toByteArray())
}

/**
 * Encode a string to windows-1255 bytes. Characters with no cp1255 mapping
 * become '?'. Needed to POST Hebrew form data the way rotter expects.
 */
fun encodeWin1255(s: String): IntArray {
    val out = ArrayList<Int>(s.length)
    var i = 0
    while (i < s.length) {
        val cp = s.codePointAt(i)
        out.add(if (cp < 0x80) cp else REVERSE[cp] ?: 0x3F)
        i += Character.charCount(cp)
    }
    return out.toIntArray()
}

/**
 * cp1255 `application/x-www-form-urlencoded` component encoding: spaces → '+',
 * unreserved bytes pass through, everything else → %XX over the cp1255 bytes.
 */
private fun formComponent(s: String): String {
    val sb = StringBuilder()
    for (b in encodeWin1255(s)) {
        when {
            b == 0x20 -> sb.append('+')
            b in 0x30..0x39 || b in 0x41..0x5A || b in 0x61..0x7A ||
                b == 0x2D || b == 0x2E || b == 0x5F || b == 0x7E -> sb.append(b.toChar())
            else -> sb.append('%').append(Integer.toHexString(b).uppercase().padStart(2, '0'))
        }
    }
    return sb.toString()
}

/** Build a cp1255 urlencoded request body from form fields (in order). */
fun encodeWin1255Form(fields: List<Pair<String, String>>): String =
    fields.joinToString("&") { (k, v) -> "${formComponent(k)}=${formComponent(v)}" }

private const val R = 0xFFFD // replacement char for undefined slots

// Unicode code points for bytes 0x80..0xFF (index 0 == 0x80).
private val HIGH = intArrayOf(
    0x20AC, R, 0x201A, 0x0192, 0x201E, 0x2026, 0x2020, 0x2021, // 80-87
    0x02C6, 0x2030, R, 0x2039, R, R, R, R, // 88-8F
    R, 0x2018, 0x2019, 0x201C, 0x201D, 0x2022, 0x2013, 0x2014, // 90-97
    0x02DC, 0x2122, R, 0x203A, R, R, R, R, // 98-9F
    0x00A0, 0x00A1, 0x00A2, 0x00A3, 0x20AA, 0x00A5, 0x00A6, 0x00A7, // A0-A7 (A4=₪)
    0x00A8, 0x00A9, 0x00D7, 0x00AB, 0x00AC, 0x00AD, 0x00AE, 0x00AF, // A8-AF (AA=×)
    0x00B0, 0x00B1, 0x00B2, 0x00B3, 0x00B4, 0x00B5, 0x00B6, 0x00B7, // B0-B7
    0x00B8, 0x00B9, 0x00F7, 0x00BB, 0x00BC, 0x00BD, 0x00BE, 0x00BF, // B8-BF (BA=÷)
    0x05B0, 0x05B1, 0x05B2, 0x05B3, 0x05B4, 0x05B5, 0x05B6, 0x05B7, // C0-C7 niqqud
    0x05B8, 0x05B9, R, 0x05BB, 0x05BC, 0x05BD, 0x05BE, 0x05BF, // C8-CF
    0x05C0, 0x05C1, 0x05C2, 0x05C3, 0x05F0, 0x05F1, 0x05F2, 0x05F3, // D0-D7
    0x05F4, R, R, R, R, R, R, R, // D8-DF
    0x05D0, 0x05D1, 0x05D2, 0x05D3, 0x05D4, 0x05D5, 0x05D6, 0x05D7, // E0-E7 alef..
    0x05D8, 0x05D9, 0x05DA, 0x05DB, 0x05DC, 0x05DD, 0x05DE, 0x05DF, // E8-EF
    0x05E0, 0x05E1, 0x05E2, 0x05E3, 0x05E4, 0x05E5, 0x05E6, 0x05E7, // F0-F7
    0x05E8, 0x05E9, 0x05EA, R, R, 0x200E, 0x200F, R, // F8-FF ..tav, LRM, RLM
)

/** Unicode → cp1255 byte, derived from [HIGH] (the decode table). */
private val REVERSE: Map<Int, Int> = buildMap {
    for (i in HIGH.indices) if (HIGH[i] != R) put(HIGH[i], i + 0x80)
}
