package com.narsing.music

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class CatalogTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val streamUrl: String,
    val artworkUrl: String,
    val licenseUrl: String
)

/**
 * Online catalog backed by Jamendo's API. A developer client ID is required; no secret is embedded
 * in the APK. Only tracks returned by the provider are offered, and their license links are shown.
 */
@Composable
fun OnlineCatalogScreen(
    foreground: Color,
    muted: Color,
    accent: Color,
    panel: Color,
    onPlayOnline: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE) }
    var clientId by remember { mutableStateOf(prefs.getString("jamendo_client_id", "") ?: "") }
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<CatalogTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Add your Jamendo API client ID to browse openly licensed music.") }
    val scope = rememberCoroutineScope()

    fun searchCatalog() {
        val key = clientId.trim()
        if (key.isBlank()) {
            message = "A Jamendo client ID is required. Create an app in the Jamendo developer portal, then save its client ID here."
            tracks = emptyList()
            return
        }
        scope.launch {
            loading = true
            message = "Searching the licensed catalog…"
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                    val url = URL("https://api.jamendo.com/v3.0/tracks/?client_id=${URLEncoder.encode(key, "UTF-8")}&format=json&limit=30&order=popularity_total&include=licenses&audioformat=mp31&namesearch=$encodedQuery")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 15000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "MusicPro-Android/1.1")
                    }
                    try {
                        val status = connection.responseCode
                        if (status !in 200..299) error("Catalog request failed (HTTP $status). Check the client ID and try again.")
                        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        if (root.optString("headers").contains("error", true)) error(root.optString("headers"))
                        val data = root.optJSONArray("results") ?: error("The catalog returned no results.")
                        buildList {
                            for (i in 0 until data.length()) {
                                val item = data.optJSONObject(i) ?: continue
                                val stream = item.optString("audio").trim()
                                if (stream.isBlank() || stream == "null") continue
                                add(CatalogTrack(
                                    id = item.optString("id"),
                                    title = item.optString("name").ifBlank { "Untitled track" },
                                    artist = item.optString("artist_name").ifBlank { "Unknown artist" },
                                    album = item.optString("album_name").ifBlank { "Jamendo" },
                                    streamUrl = stream,
                                    artworkUrl = item.optString("album_image").ifBlank { item.optString("image") },
                                    licenseUrl = item.optString("license_ccurl").ifBlank { "https://www.jamendo.com/" }
                                ))
                            }
                        }
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            result.onSuccess { found ->
                tracks = found
                message = if (found.isEmpty()) "No playable tracks found. Try another search or check your Jamendo app configuration." else "Found ${found.size} tracks • open each license link before reuse."
            }.onFailure { error ->
                tracks = emptyList()
                message = error.message ?: "Unable to reach the catalog. Check your connection and client ID."
            }
            loading = false
        }
    }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp).size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("DISCOVER ONLINE", color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Jamendo • independent music catalog", color = muted, fontSize = 12.sp)
                    }
                }
                Text("Stream catalog tracks while keeping your local library and offline playback. A free developer client ID is needed; Music Pro does not bundle credentials.", color = muted, fontSize = 12.sp)
                OutlinedTextField(
                    value = clientId,
                    onValueChange = { clientId = it.take(200) },
                    label = { Text("Jamendo client ID") },
                    placeholder = { Text("Paste your client ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        prefs.edit().putString("jamendo_client_id", clientId.trim()).apply()
                        message = "Client ID saved on this device."
                    }, modifier = Modifier.weight(1f)) { Text("SAVE ID") }
                    TextButton(onClick = { message = "Create an app at developer.jamendo.com, then paste its client ID above." }) { Text("HOW TO GET ID") }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(100) },
                    label = { Text("Search music") },
                    placeholder = { Text("Track or artist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { prefs.edit().putString("jamendo_client_id", clientId.trim()).apply(); searchCatalog() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    else Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (loading) "SEARCHING…" else "BROWSE CATALOG")
                }
                Text(message, color = muted, fontSize = 12.sp)
            }
        }
        if (tracks.isNotEmpty()) {
            Text("OPEN CATALOG • ${tracks.size} RESULTS", color = foreground, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Surface(color = panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = track.artworkUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") },
                                contentDescription = "${track.title} artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF292929))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(track.title, color = foreground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("License: Jamendo • verify terms before reuse", color = muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                TextButton(onClick = { runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(track.licenseUrl))) } }, contentPadding = PaddingValues(0.dp)) {
                                    Text("VIEW TRACK LICENSE", color = accent, fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { onPlayOnline(track.streamUrl, track.title, track.artist, track.artworkUrl) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = accent, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
            Text("Online streams require internet access. Local songs, playlists, favorites, and background playback remain available offline.", color = muted, fontSize = 12.sp)
        }
    }
}
