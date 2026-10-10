package com.narsing.music

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistFileNameTest {
    @Test
    fun preservesUnicodeAndSpaces() {
        assertEquals("తెలుగు పాటలు", safePlaylistFileName("తెలుగు పాటలు"))
        assertEquals("My Playlist", safePlaylistFileName("My Playlist"))
    }

    @Test
    fun removesPathSeparatorsAndReservedCharacters() {
        assertEquals("Road Trip", safePlaylistFileName("../Road: Trip|"))
    }

    @Test
    fun usesFallbackForBlankOrUnsafeNames() {
        assertEquals("Music playlist", safePlaylistFileName("///:::"))
        assertEquals("Music playlist", safePlaylistFileName("   "))
    }

    @Test
    fun limitsLengthForPortableFilenames() {
        assertEquals(120, safePlaylistFileName("A".repeat(150)).length)
    }
}
