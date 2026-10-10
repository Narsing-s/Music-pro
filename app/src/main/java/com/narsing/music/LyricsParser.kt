package com.narsing.music

import android.util.Xml
import org.xmlpull.v1.XmlPullParser

data class TimedLyric(val timeMs: Long, val text: String)

/**
 * Parses common LRC timestamps and TTML timed paragraphs.
 * LRC [offset:...] metadata is applied in milliseconds; negative results are clamped to zero.
 * The input is only read locally; no lyrics or listening data are uploaded.
 */
fun parseTimedLyrics(raw: String): List<TimedLyric> {
    val lrc = parseLrc(raw)
    if (lrc.isNotEmpty()) return lrc
    return parseTtml(raw)
}

private fun parseLrc(raw: String): List<TimedLyric> {
    val lines = raw.lineSequence().toList()
    val timestampPattern = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")
    val offsetPattern = Regex("(?i)^\\[offset:\\s*([+-]?\\d+)\\]\\s*$")
    val offsetMs = lines.asSequence()
        .mapNotNull { offsetPattern.matchEntire(it)?.groupValues?.get(1)?.toLongOrNull() }
        .lastOrNull()
        ?: 0L

    return lines.asSequence().flatMap { line ->
        val matches = timestampPattern.findAll(line).toList()
        val lyric = timestampPattern.replace(line, "").trim()
        if (matches.isEmpty() || lyric.isBlank()) emptySequence() else matches.asSequence().map { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            val originalTimeMs = (minutes * 60L + seconds) * 1000L + fraction
            TimedLyric((originalTimeMs + offsetMs).coerceAtLeast(0L), lyric)
        }
    }.sortedBy { it.timeMs }.toList()
}

private fun parseTtml(raw: String): List<TimedLyric> {
    if (!raw.contains("<tt", ignoreCase = true) || !raw.contains("<p", ignoreCase = true)) return emptyList()
    return runCatching {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(raw.reader())
        val lines = mutableListOf<TimedLyric>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name.substringAfter(':') == "p") {
                val begin = (0 until parser.attributeCount)
                    .firstNotNullOfOrNull { index ->
                        parser.getAttributeName(index).substringAfter(':').takeIf { it == "begin" }?.let { parser.getAttributeValue(index) }
                    }
                val startMs = begin?.let(::parseTtmlTime)
                val paragraphDepth = parser.depth
                val text = StringBuilder()
                event = parser.next()
                while (!(event == XmlPullParser.END_TAG && parser.depth == paragraphDepth && parser.name.substringAfter(':') == "p") &&
                    event != XmlPullParser.END_DOCUMENT
                ) {
                    if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT) text.append(parser.text)
                    event = parser.next()
                }
                val line = text.toString().replace(Regex("\\s+"), " ").trim()
                if (startMs != null && line.isNotBlank()) lines += TimedLyric(startMs, line)
            } else {
                event = parser.next()
            }
        }
        lines.sortedBy { it.timeMs }
    }.getOrDefault(emptyList())
}

private fun parseTtmlTime(value: String): Long? {
    val match = Regex("^(?:(\\d+):)?(\\d{2}):(\\d{2})(?:[.,](\\d{1,3}))?$|^(\\d+(?:\\.\\d+)?)s$").matchEntire(value.trim())
        ?: return null
    if (match.groupValues[5].isNotBlank()) return ((match.groupValues[5].toDoubleOrNull() ?: return null) * 1000.0).toLong()
    val hours = match.groupValues[1].toLongOrNull() ?: 0L
    val minutes = match.groupValues[2].toLongOrNull() ?: return null
    val seconds = match.groupValues[3].toLongOrNull() ?: return null
    val millis = match.groupValues[4].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
    return ((hours * 60L + minutes) * 60L + seconds) * 1000L + millis
}
