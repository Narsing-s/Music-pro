package com.narsing.music

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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

private data class MovieMusicCategory(val label: String, val search: String)

@Composable
fun OnlineCatalogScreen(
    foreground: Color,
    muted: Color,
    accent: Color,
    panel: Color,
    onPlayOnline: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val clientId = BuildConfig.JAMENDO_CLIENT_ID.trim()
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf<List<CatalogTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var offset by remember { mutableIntStateOf(0) }
    var canLoadMore by remember { mutableStateOf(false) }
    var cloudSearch by remember { mutableStateOf("popular Indian music") }
    var selectedCategory by remember { mutableStateOf("For You") }
    var message by remember { mutableStateOf(if (clientId.isBlank()) "Online catalogue setup is required for independent music." else "Loading music for you…") }
    val scope = rememberCoroutineScope()
    val movieCategories = remember {
        listOf(
            MovieMusicCategory("Telugu Movies", "latest Telugu movie songs"),
            MovieMusicCategory("Hindi / Bollywood", "latest Bollywood Hindi movie songs"),
            MovieMusicCategory("Tamil Movies", "latest Tamil movie songs"),
            MovieMusicCategory("Malayalam", "latest Malayalam movie songs"),
            MovieMusicCategory("Kannada", "latest Kannada movie songs"),
            MovieMusicCategory("Punjabi", "latest Punjabi movie songs"),
            MovieMusicCategory("Devotional", "popular Indian devotional songs bhajans"),
            MovieMusicCategory("Telugu Devotional", "popular Telugu devotional songs"),
            MovieMusicCategory("Hindi Bhajans", "popular Hindi bhajans devotional songs"),
            MovieMusicCategory("Tamil Devotional", "popular Tamil devotional songs"),
            MovieMusicCategory("Islamic", "popular Islamic devotional nasheed"),
            MovieMusicCategory("Christian", "Christian devotional worship songs")
        )
    }

    fun openMovieMusic(search: String) {
        cloudSearch = search
        message = "Cloud panel results: " + search
    }

    fun loadCatalog(search: String, append: Boolean = false) {
        if (clientId.isBlank()) {
            message = "Independent music is not configured in this build. The publisher must set the JAMENDO_CLIENT_ID GitHub Actions secret and rebuild."
            return
        }
        scope.launch {
            if (append) loadingMore = true else loading = true
            message = if (append) "Loading more tracks…" else "Finding tracks…"
            val startAt = if (append) offset + 30 else 0
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encodedQuery = URLEncoder.encode(search.trim(), "UTF-8")
                    val queryParameter = if (encodedQuery.isBlank()) "" else "&search=$encodedQuery"
                    val url = URL("https://api.jamendo.com/v3.0/tracks/?client_id=${URLEncoder.encode(clientId, "UTF-8")}&format=json&limit=30&offset=$startAt&order=relevance&audioformat=mp31&type=single+albumtrack$queryParameter")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10000
                        readTimeout = 15000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "MusicPro-Android/1.2")
                    }
                    try {
                        val status = connection.responseCode
                        if (status !in 200..299) error("Catalogue request failed (HTTP $status). Check the app configuration and retry.")
                        val root = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        val headers = root.optJSONObject("headers")
                        val apiCode = headers?.optInt("code", 0) ?: 0
                        val apiStatus = headers?.optString("status").orEmpty()
                        val apiError = headers?.optString("error_message").orEmpty()
                        if (apiCode != 0 || apiStatus.equals("error", ignoreCase = true)) {
                            error(apiError.ifBlank { "The provider rejected this request. Verify the publisher's Jamendo Client ID." })
                        }
                        val data = root.optJSONArray("results") ?: error("Unexpected catalogue response. Please retry.")
                        val found = buildList {
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
                        found to (data.length() >= 30)
                    } finally {
                        connection.disconnect()
                    }
                }
            }
            result.onSuccess { (found, more) ->
                tracks = if (append) (tracks + found).distinctBy { it.id } else found
                offset = startAt
                canLoadMore = more
                message = when {
                    found.isEmpty() && tracks.isEmpty() -> "No playable tracks found. Try another search or check the network and app configuration."
                    append -> "Loaded more tracks • ${tracks.size} available"
                    else -> "Ready • ${found.size} tracks loaded"
                }
            }.onFailure { error ->
                if (!append) tracks = emptyList()
                message = when {
                    error is java.net.UnknownHostException || error is java.net.SocketTimeoutException -> "Can't reach the music catalogue. Check your internet connection and retry."
                    error.message?.contains("HTTP 401") == true || error.message?.contains("HTTP 403") == true -> "The provider rejected this app's Client ID. The publisher must check Jamendo configuration."
                    else -> error.message ?: "Unable to load music. Check your connection and retry."
                }
            }
            loading = false
            loadingMore = false
        }
    }

    LaunchedEffect(clientId) { if (clientId.isNotBlank()) loadCatalog("") }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Compact discovery header: one clear search surface instead of stacked panels.
        Surface(
            color = panel,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Discover", color = foreground, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Music for every mood.", color = muted, fontSize = 13.sp)
                    }
                    Surface(
                        color = accent.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = "Online music",
                            tint = accent,
                            modifier = Modifier.padding(11.dp).size(23.dp)
                        )
                    }
                    IconButton(
                        onClick = { loadCatalog(query.ifBlank { cloudSearch }) },
                        enabled = !loading && clientId.isNotBlank()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh catalogue", tint = muted)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(100) },
                    placeholder = { Text("Songs, artists, moods…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = muted) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = {
                                cloudSearch = query.trim()
                                selectedCategory = "Search"
                                loadCatalog(query.trim())
                            }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Search and play results", tint = accent)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = muted.copy(alpha = 0.25f),
                        focusedLabelColor = accent,
                        cursorColor = accent
                    )
                )
                Text(
                    if (clientId.isBlank()) "Online catalogue needs publisher setup" else message,
                    color = muted,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (clientId.isBlank()) {
                    Text(
                        "Set JAMENDO_CLIENT_ID in GitHub Actions and rebuild to enable playable independent tracks.",
                        color = muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Browse", color = foreground, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = listOf(MovieMusicCategory("For You", "popular Indian music")) + movieCategories
                categories.forEach { category ->
                    val selected = selectedCategory == category.label
                    Surface(
                        color = if (selected) accent else panel,
                        contentColor = if (selected) Color.Black else foreground,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable {
                            selectedCategory = category.label
                            cloudSearch = category.search
                            query = category.search
                            loadCatalog(category.search)
                        }
                    ) {
                        Text(
                            category.label,
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    when (selectedCategory) {
                        "For You" -> "Made for discovery"
                        "Search" -> "Search results"
                        else -> selectedCategory
                    },
                    color = foreground,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (tracks.isNotEmpty()) "${tracks.size} playable tracks" else "Independent music catalogue",
                    color = muted,
                    fontSize = 12.sp
                )
            }
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = accent, strokeWidth = 2.dp)
        }

        if (tracks.isNotEmpty()) {
            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(tracks, key = { it.id }) { track ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .clickable { onPlayOnline(track.streamUrl, track.title, track.artist, track.artworkUrl) }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.artworkUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") },
                            contentDescription = "${track.title} artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                .background(panel)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                track.title,
                                color = foreground,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                track.artist,
                                color = muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = {
                            onPlayOnline(track.streamUrl, track.title, track.artist, track.artworkUrl)
                        }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = accent)
                        }
                    }
                }
                if (canLoadMore) {
                    item {
                        OutlinedButton(
                            onClick = { loadCatalog(query.ifBlank { cloudSearch }, append = true) },
                            enabled = !loadingMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (loadingMore) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (loadingMore) "Loading more…" else "Load more tracks")
                        }
                    }
                }
            }
        } else {
            Surface(
                color = panel,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = accent,
                            modifier = Modifier.padding(16.dp).size(30.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (clientId.isBlank()) "Catalogue setup needed" else if (loading) "Finding your next track…" else "Ready when you are",
                        color = foreground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (clientId.isBlank())
                            "Playable online tracks appear after the publisher configures the catalogue."
                        else
                            "Choose a category or search above to find tracks you can play directly in Music Pro.",
                        color = muted,
                        fontSize = 12.sp
                    )
                    if (clientId.isNotBlank() && !loading) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { loadCatalog(query.ifBlank { cloudSearch }) },
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                            shape = RoundedCornerShape(50)
                        ) { Text("Try again") }
                    }
                }
            }
        }
    }
}
