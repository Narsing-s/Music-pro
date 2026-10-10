package com.narsing.music

import android.net.Uri

data class Song(val id: Long, val title: String, val artist: String, val album: String, val durationMs: Long, val uri: Uri, val dateAddedSeconds: Long = 0L, val filePath: String = "")
