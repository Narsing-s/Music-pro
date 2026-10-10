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
    fun periodRecapCountsOnlyEventsInsideSelectedMonth() {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(2026, java.util.Calendar.JANUARY, 15, 12, 0, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val january = calendar.timeInMillis
        calendar.set(2026, java.util.Calendar.FEBRUARY, 15, 12, 0, 0)
        val february = calendar.timeInMillis

        ListeningInsightsStore.recordPlay(context, song, january)
        ListeningInsightsStore.addListeningTime(context, song, 30_000L, january)
        ListeningInsightsStore.recordPlay(context, song, february)
        ListeningInsightsStore.addListeningTime(context, song, 45_000L, february)

        val recap = ListeningInsightsStore.snapshotForPeriod(context, listOf(song), "THIS_MONTH", february).single()
        assertEquals(1, recap.playCount)
        assertEquals(45_000L, recap.listenedMs)
        assertEquals(2, ListeningInsightsStore.snapshot(context, listOf(song)).single().playCount)
    }

    @Test
    fun optimizeRemovesOnlyStaleInsightEntries() {
        context.getSharedPreferences("music_pro_preferences", 0).edit()
            .putString("recent_ids", "991")
            .apply()
        ListeningInsightsStore.recordPlay(context, song, 1_800_000_000_000L)
        val otherSong = song.copy(id = 992L, title = "Removed Track")
        ListeningInsightsStore.recordPlay(context, otherSong, 1_800_000_000_000L)

        val removedKeys = ListeningInsightsStore.optimize(context, setOf(song.id))

        assertEquals(true, removedKeys > 0)
        assertEquals(1, ListeningInsightsStore.snapshot(context, listOf(song)).single().playCount)
        assertEquals("991", context.getSharedPreferences("music_pro_preferences", 0).getString("recent_ids", null))
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
