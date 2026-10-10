package com.narsing.music

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AlbumMetadataStoreTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val album = "తెలుగు / Night: Drive"

    @Before
    fun clearAlbumOverride() {
        AlbumMetadataStore.clear(context, album)
    }

    @Test
    fun savesAndRestoresOverridesWithoutChangingOtherPreferences() {
        context.getSharedPreferences("music_pro_preferences", 0).edit().putString("recent_ids", "1,2").apply()
        val expected = AlbumMetadataOverride("Night Drive Deluxe", "Artist A, Artist B", "2024", "Electronic, Ambient", true)

        AlbumMetadataStore.save(context, album, expected)

        assertEquals(expected, AlbumMetadataStore.get(context, album))
        assertEquals("1,2", context.getSharedPreferences("music_pro_preferences", 0).getString("recent_ids", null))
    }

    @Test
    fun clearRemovesOnlyAlbumOverride() {
        AlbumMetadataStore.save(context, album, AlbumMetadataOverride(name = "Temporary"))
        AlbumMetadataStore.clear(context, album)

        assertEquals(AlbumMetadataOverride(), AlbumMetadataStore.get(context, album))
        assertFalse(context.getSharedPreferences("music_pro_preferences", 0).all.keys.any { it.startsWith("album_override_") })
    }

    @Test
    fun invalidYearIsDiscarded() {
        AlbumMetadataStore.save(context, album, AlbumMetadataOverride(year = "20xx"))
        assertEquals("", AlbumMetadataStore.get(context, album).year)
    }
}
