package com.narsing.music

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistM3uTest {
    @Test
    fun exportsHeaderDurationMetadataAndUri() {
        val result = buildM3uPlaylist(
            listOf(M3uTrackEntry("Ocean Lights", "Example Artist", 245_900L, "content://media/audio/42"))
        )

        assertEquals(
            "#EXTM3U\n#EXTINF:245,Ocean Lights - Example Artist\ncontent://media/audio/42",
            result
        )
    }

    @Test
    fun metadataNewlinesCannotInjectPlaylistEntries() {
        val result = buildM3uPlaylist(
            listOf(M3uTrackEntry("Song\n#EXTINF:999,Injected", "Artist\r\n#EXTM3U", 1_000L, "content://media/audio/1"))
        )

        assertEquals(
            "#EXTM3U\n#EXTINF:1,Song #EXTINF:999,Injected - Artist #EXTM3U\ncontent://media/audio/1",
            result
        )
        assertEquals(3, result.lines().size)
    }

    @Test
    fun uriNewlinesCannotInjectAdditionalPlaylistEntries() {
        val result = buildM3uPlaylist(
            listOf(M3uTrackEntry("Song", "Artist", 1_000L, "content://media/audio/1\n#EXTM3U\ncontent://injected"))
        )

        assertEquals(3, result.lines().size)
        assertTrue(result.endsWith("content://media/audio/1#EXTM3Ucontent://injected"))
    }

    @Test
    fun preservesUnicodeAndHandlesEmptyOrNegativeValues() {
        assertEquals("#EXTM3U", buildM3uPlaylist(emptyList()))
        val result = buildM3uPlaylist(listOf(M3uTrackEntry("తెలుగు పాట", "కళాకారుడు", -1L, "content://audio/1")))
        assertTrue(result.contains("0,తెలుగు పాట - కళాకారుడు"))
    }
}
