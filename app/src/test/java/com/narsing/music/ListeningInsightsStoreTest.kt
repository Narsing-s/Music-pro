package com.narsing.music

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ListeningInsightsStoreTest {
    private val context: Application = RuntimeEnvironment.getApplication()
    private val song = Song(
        id = 991L,
        title = "Night Drive",
        artist = "Example Artist",
        album = "After Hours",
        durationMs = 210_000L,
        uri = Uri.parse("content://media/external/audio/media/991")
    )

    @Before
    fun clearInsights() {
        ListeningInsightsStore.clear(context)
    }

    @Test
    fun listeningTimeAndPlaysAreSavedLocally() {
        ListeningInsightsStore.addListeningTime(context, song, 15_000L)
        ListeningInsightsStore.addListeningTime(context, song, 5_000L)
        ListeningInsightsStore.recordPlay(context, song)

        val insight = ListeningInsightsStore.snapshot(context, listOf(song)).single()
        assertEquals(20_000L, insight.listenedMs)
        assertEquals(1, insight.playCount)
        assertEquals("Night Drive", insight.title)
    }

    @Test
    fun clearRemovesOnlyInsightData() {
        context.getSharedPreferences("music_pro_preferences", 0).edit()
            .putString("recent_ids", "991")
            .apply()
        ListeningInsightsStore.addListeningTime(context, song, 1_000L)

        ListeningInsightsStore.clear(context)

        assertEquals(0L, ListeningInsightsStore.snapshot(context, listOf(song)).single().listenedMs)
        assertEquals("991", context.getSharedPreferences("music_pro_preferences", 0).getString("recent_ids", null))
    }
}
