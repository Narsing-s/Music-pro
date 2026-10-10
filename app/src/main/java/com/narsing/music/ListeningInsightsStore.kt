package com.narsing.music

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Local-only listening history. Nothing is uploaded and original audio metadata is never changed.
 * A play is counted only after a track has been actively listened to for 30 seconds.
 */
data class ListeningInsight(
    val songId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val playCount: Int,
    val listenedMs: Long
)

object ListeningInsightsStore {
    private const val PREFIX = "insight_"
    private const val DAY_PREFIX = "insight_day_"
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun key(songId: Long, field: String) = "${PREFIX}${songId}_${field}"
    private fun dayKey(timestamp: Long, songId: Long, field: String) =
        "${DAY_PREFIX}${dayFormat.format(Date(timestamp))}_${songId}_${field}"

    fun addListeningTime(context: Context, song: Song, deltaMs: Long, timestamp: Long = System.currentTimeMillis()) {
        if (deltaMs <= 0L) return
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(key(song.id, "title"), song.title)
            .putString(key(song.id, "artist"), song.artist)
            .putString(key(song.id, "album"), song.album)
            .putLong(key(song.id, "listened_ms"), prefs.getLong(key(song.id, "listened_ms"), 0L) + deltaMs)
            .putLong(dayKey(timestamp, song.id, "listened_ms"), prefs.getLong(dayKey(timestamp, song.id, "listened_ms"), 0L) + deltaMs)
            .apply()
    }

    fun recordPlay(context: Context, song: Song, timestamp: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(key(song.id, "title"), song.title)
            .putString(key(song.id, "artist"), song.artist)
            .putString(key(song.id, "album"), song.album)
            .putInt(key(song.id, "plays"), prefs.getInt(key(song.id, "plays"), 0) + 1)
            .putInt(dayKey(timestamp, song.id, "plays"), prefs.getInt(dayKey(timestamp, song.id, "plays"), 0) + 1)
            .apply()
    }

    fun snapshot(context: Context, songs: List<Song>): List<ListeningInsight> {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        return songs.map { song ->
            ListeningInsight(
                songId = song.id,
                title = prefs.getString(key(song.id, "title"), song.title) ?: song.title,
                artist = prefs.getString(key(song.id, "artist"), song.artist) ?: song.artist,
                album = prefs.getString(key(song.id, "album"), song.album) ?: song.album,
                playCount = prefs.getInt(key(song.id, "plays"), 0),
                listenedMs = prefs.getLong(key(song.id, "listened_ms"), 0L)
            )
        }
    }

    /** Period totals are based on local calendar days; ALL_TIME preserves the existing cumulative view. */
    fun snapshotForPeriod(context: Context, songs: List<Song>, period: String, now: Long = System.currentTimeMillis()): List<ListeningInsight> {
        if (period == "ALL_TIME") return snapshot(context, songs)
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        if (period == "THIS_MONTH") calendar.set(Calendar.DAY_OF_MONTH, 1)
        else {
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.MONTH, Calendar.JANUARY)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDay = calendar.timeInMillis
        val lastDay = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val result = ArrayList<ListeningInsight>(songs.size)
        songs.forEach { song ->
            var plays = 0
            var listened = 0L
            val day = Calendar.getInstance().apply { timeInMillis = firstDay }
            while (day.timeInMillis <= lastDay) {
                val date = dayFormat.format(Date(day.timeInMillis))
                plays += prefs.getInt("${DAY_PREFIX}${date}_${song.id}_plays", 0)
                listened += prefs.getLong("${DAY_PREFIX}${date}_${song.id}_listened_ms", 0L)
                day.add(Calendar.DAY_OF_YEAR, 1)
            }
            val base = snapshot(context, listOf(song)).single()
            result += base.copy(playCount = plays, listenedMs = listened)
        }
        return result
    }

    /** Removes insight-only keys for tracks no longer present in the current local library. */
    fun optimize(context: Context, validSongIds: Set<Long>): Int {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        val staleKeys = prefs.all.keys.filter { name ->
            if (!name.startsWith(PREFIX)) return@filter false
            val parts = name.removePrefix(PREFIX).split("_")
            val id = if (parts.firstOrNull() == "day") parts.getOrNull(2)?.toLongOrNull()
                else parts.firstOrNull()?.toLongOrNull()
            id != null && id !in validSongIds
        }
        if (staleKeys.isNotEmpty()) {
            val editor = prefs.edit()
            staleKeys.forEach(editor::remove)
            editor.apply()
        }
        return staleKeys.size
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(PREFIX) }.forEach(editor::remove)
        editor.apply()
    }
}
