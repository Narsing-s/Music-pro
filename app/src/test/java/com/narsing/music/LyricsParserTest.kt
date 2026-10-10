package com.narsing.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LyricsParserTest {
    @Test
    fun parsesLrcTimestampsAndMultipleTimestampsOnOneLine() {
        val parsed = parseTimedLyrics("[00:01.20]First line\n[00:02.5][00:03.00]Repeated line")

        assertEquals(
            listOf(
                TimedLyric(1_200L, "First line"),
                TimedLyric(2_500L, "Repeated line"),
                TimedLyric(3_000L, "Repeated line")
            ),
            parsed
        )
    }

    @Test
    fun parsesTtmlParagraphsAndNestedSpans() {
        val ttml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml"><body><div>
              <p begin="00:00:01.200" end="00:00:02.000">First <span>line</span></p>
              <p begin="3.5s">Second line</p>
            </div></body></tt>
        """.trimIndent()

        assertEquals(
            listOf(TimedLyric(1_200L, "First line"), TimedLyric(3_500L, "Second line")),
            parseTimedLyrics(ttml)
        )
    }

    @Test
    fun malformedOrUntimedLyricsReturnEmptyList() {
        assertTrue(parseTimedLyrics("not timed lyrics").isEmpty())
        assertTrue(parseTimedLyrics("<tt><body><p>no timestamp</p></body></tt>").isEmpty())
    }
}
