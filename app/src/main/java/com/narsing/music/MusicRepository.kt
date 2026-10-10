package com.narsing.music

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class MusicRepository(private val context: Context) {
    fun loadSongs(): List<Song> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val metadataPrefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATE_ADDED, MediaStore.Audio.Media.DATA)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val genreBySongId = mutableMapOf<Long, String>()
        runCatching {
            context.contentResolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME), null, null, MediaStore.Audio.Genres.NAME + " COLLATE NOCASE ASC")?.use { genres ->
                val genreIdCol = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
                val genreNameCol = genres.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
                while (genres.moveToNext()) {
                    val genreId = genres.getLong(genreIdCol)
                    val genreName = genres.getString(genreNameCol)?.takeIf { it.isNotBlank() } ?: continue
                    val membersUri = MediaStore.Audio.Genres.Members.getContentUri("external", genreId)
                    context.contentResolver.query(membersUri, arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID), null, null, null)?.use { members ->
                        val audioIdCol = members.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
                        while (members.moveToNext()) genreBySongId.putIfAbsent(members.getLong(audioIdCol), genreName)
                    }
                }
            }
        }
        val songs = mutableListOf<Song>()
        context.contentResolver.query(collection, projection, selection, null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val filePathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown track"
                val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown artist"
                val album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown album"
                songs += Song(
                    id = id,
                    title = metadataPrefs.getString("metadata_${id}_title", title) ?: title,
                    artist = metadataPrefs.getString("metadata_${id}_artist", artist) ?: artist,
                    album = metadataPrefs.getString("metadata_${id}_album", album) ?: album,
                    durationMs = cursor.getLong(durationCol),
                    uri = ContentUris.withAppendedId(collection, id),
                    dateAddedSeconds = cursor.getLong(dateAddedCol),
                    filePath = cursor.getString(filePathCol).orEmpty(),
                    genre = genreBySongId[id] ?: "Unknown genre"
                )
            }
        }
        return songs
    }
}
