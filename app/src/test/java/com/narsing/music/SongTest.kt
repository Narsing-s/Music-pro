package com.narsing.music

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongTest {
    @Test
    fun missingGenreDefaultsToUnknownGenre() {
        val song = Song(
            id = 7L,
            title = "Night Drive",
            artist = "Unknown artist",
            album = "Unknown album",
            durationMs = 180_000L,
            uri = Uri.parse("content://media/external/audio/media/7")
        )

        assertEquals("Unknown genre", song.genre)
        assertEquals(listOf("Unknown genre"), song.genres)
    }

    @Test
    fun suppliedGenresAndMetadataArePreserved() {
        val song = Song(
            id = 42L,
            title = "Ocean Lights",
            artist = "Example Artist",
            album = "After Hours",
            durationMs = 245_000L,
            uri = Uri.parse("content://media/external/audio/media/42"),
            dateAddedSeconds = 1_700_000_000L,
            filePath = "/music/ocean-lights.mp3",
            genre = "Electronic; Ambient",
            genres = listOf("Electronic", "Ambient")
        )

        assertEquals(42L, song.id)
        assertEquals("Ocean Lights", song.title)
        assertEquals("Example Artist", song.artist)
        assertEquals("After Hours", song.album)
        assertEquals(245_000L, song.durationMs)
        assertEquals(1_700_000_000L, song.dateAddedSeconds)
        assertEquals("/music/ocean-lights.mp3", song.filePath)
        assertEquals("Electronic; Ambient", song.genre)
        assertEquals(listOf("Electronic", "Ambient"), song.genres)
    }

    @Test
    fun explicitSingleGenreIsUsedAsDefaultGenreList() {
        val song = Song(
            id = 9L,
            title = "Warm Rain",
            artist = "Example Artist",
            album = "Singles",
            durationMs = 90_000L,
            uri = Uri.parse("content://media/external/audio/media/9"),
            genre = "Jazz"
        )

        assertEquals("Jazz", song.genre)
        assertEquals(listOf("Jazz"), song.genres)
    }
}
