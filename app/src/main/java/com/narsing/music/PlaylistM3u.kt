package com.narsing.music

/**
 * Minimal data needed to export a track to an extended M3U playlist.
 */
data class M3uTrackEntry(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uri: String
)

/**
 * Builds portable UTF-8-friendly M3U text without allowing metadata or URI line breaks to
 * inject extra playlist entries. Normal Android content URIs are preserved unchanged.
 */
fun buildM3uPlaylist(tracks: List<M3uTrackEntry>): String = buildList {
    add("#EXTM3U")
    tracks.forEach { track ->
        // Skip malformed URIs instead of merging injected lines into a different URI.
        if (track.uri.isBlank() || track.uri.any { it == '\r' || it == '\n' }) return@forEach
        val durationSeconds = (track.durationMs / 1000L).coerceAtLeast(0L)
        val title = track.title.replace(Regex("[\\r\\n]+"), " ").trim()
        val artist = track.artist.replace(Regex("[\\r\\n]+"), " ").trim()
        add("#EXTINF:$durationSeconds,$title - $artist")
        add(track.uri)
    }
}.joinToString("\n")
