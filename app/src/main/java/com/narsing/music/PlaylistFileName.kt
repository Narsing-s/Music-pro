package com.narsing.music

/**
 * Creates a portable M3U export filename without stripping non-Latin letters.
 * Removes path separators, filesystem-reserved punctuation, and control characters.
 */
fun safePlaylistFileName(name: String): String =
    name.filterNot { character ->
        character.code < 32 || character in charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    }.trim().take(120).ifBlank { "Music playlist" }
