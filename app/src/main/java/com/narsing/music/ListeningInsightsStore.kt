package com.narsing.music

import android.content.Context

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
    private fun key(songId: Long, field: String) = "${PREFIX}${songId}_${field}"

    fun addListeningTime(context: Context, song: Song, deltaMs: Long) {
        if (deltaMs <= 0L) return
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(key(song.id, "title"), song.title)
            .putString(key(song.id, "artist"), song.artist)
            .putString(key(song.id, "album"), song.album)
            .putLong(key(song.id, "listened_ms"), prefs.getLong(key(song.id, "listened_ms"), 0L) + deltaMs)
            .apply()
    }

    fun recordPlay(context: Context, song: Song) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(key(song.id, "title"), song.title)
            .putString(key(song.id, "artist"), song.artist)
            .putString(key(song.id, "album"), song.album)
            .putInt(key(song.id, "plays"), prefs.getInt(key(song.id, "plays"), 0) + 1)
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

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith(PREFIX) }.forEach(editor::remove)
        editor.apply()
    }
}
