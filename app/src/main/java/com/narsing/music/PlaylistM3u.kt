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
        val durationSeconds = (track.durationMs / 1000L).coerceAtLeast(0L)
        val title = track.title.replace(Regex("[\\r\\n]+"), " ").trim()
        val artist = track.artist.replace(Regex("[\\r\\n]+"), " ").trim()
        add("#EXTINF:$durationSeconds,$title - $artist")
        // A URI is expected to occupy exactly one M3U line; strip line breaks to prevent entry injection.
        add(track.uri.replace(Regex("[\\r\\n]+"), "").trim())
    }
}.joinToString("\n")
