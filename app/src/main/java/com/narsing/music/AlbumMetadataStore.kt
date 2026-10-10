package com.narsing.music

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64

/** Display-only album metadata overrides. Source audio files and MediaStore tags are never edited. */
data class AlbumMetadataOverride(
    val name: String = "",
    val artists: String = "",
    val year: String = "",
    val genres: String = "",
    val isEp: Boolean = false
)

object AlbumMetadataStore {
    private const val PREFS = "music_pro_preferences"
    private const val PREFIX = "album_override_"

    private fun key(originalAlbumName: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(originalAlbumName.toByteArray(StandardCharsets.UTF_8))
        return PREFIX + encoded
    }

    fun get(context: Context, originalAlbumName: String): AlbumMetadataOverride {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(originalAlbumName), null)
            ?: return AlbumMetadataOverride()
        return runCatching {
            val obj = org.json.JSONObject(json)
            AlbumMetadataOverride(
                name = obj.optString("name"),
                artists = obj.optString("artists"),
                year = obj.optString("year"),
                genres = obj.optString("genres"),
                isEp = obj.optBoolean("isEp", false)
            )
        }.getOrDefault(AlbumMetadataOverride())
    }

    fun save(context: Context, originalAlbumName: String, value: AlbumMetadataOverride) {
        val json = org.json.JSONObject().apply {
            put("name", value.name.trim().take(200))
            put("artists", value.artists.trim().take(300))
            put("year", value.year.takeIf { it.length == 4 && it.toIntOrNull() in 1000..9999 }.orEmpty())
            put("genres", value.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ").take(300))
            put("isEp", value.isEp)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key(originalAlbumName), json.toString()).apply()
    }

    fun clear(context: Context, originalAlbumName: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(key(originalAlbumName)).apply()
    }
}
