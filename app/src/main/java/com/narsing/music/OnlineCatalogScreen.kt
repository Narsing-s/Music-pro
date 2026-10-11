package com.narsing.music

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.material.icons.filled.OpenInNew
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
    var cloudSearch by remember { mutableStateOf("popular Indian movie songs devotional music") }
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

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = panel, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = accent.copy(alpha = 0.16f), shape = RoundedCornerShape(13.dp)) {
                        Icon(Icons.Default.Cloud, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp).size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("YOUR MUSIC UNIVERSE", color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("Discover • play • keep exploring", color = muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { loadCatalog(query) }, enabled = !loading && clientId.isNotBlank()) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh online tracks", tint = accent)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(100) },
                    label = { Text("Search cloud music") },
                    placeholder = { Text("Movie songs, devotional, singer, film…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { openMovieMusic(query.ifBlank { "popular Indian movie songs devotional music" }); if (clientId.isNotBlank()) loadCatalog(query) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search cloud music", tint = accent)
                        }
                    }
                )
                Text(message, color = muted, fontSize = 12.sp)
                if (clientId.isBlank()) {
                    Text("ONLINE CATALOG SETUP REQUIRED", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("The app publisher must configure JAMENDO_CLIENT_ID in GitHub Actions and rebuild. Users never need to enter a developer ID.", color = muted, fontSize = 12.sp)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MOVIE & DEVOTIONAL CLOUD", color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("Browse movie songs, devotional music, bhajans and regional releases inside this cloud panel.", color = muted, fontSize = 11.sp)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                movieCategories.forEach { category ->
                    Surface(
                        color = panel,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.clickable { openMovieMusic(category.search) }
                    ) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(category.label, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(5.dp))
                            Icon(Icons.Default.Search, contentDescription = "Search ${category.label}", tint = accent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Surface(color = panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("CLOUD RESULTS • " + cloudSearch, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(270.dp),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = true
                            webViewClient = WebViewClient()
                            loadUrl("https://www.youtube.com/results?search_query=" + URLEncoder.encode(cloudSearch, "UTF-8"))
                        }
                    },
                    update = { view ->
                        val target = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(cloudSearch, "UTF-8")
                        if (view.url != target) view.loadUrl(target)
                    }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("INDEPENDENT MUSIC", color = foreground, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text("Jamendo catalogue • licensed provider streams", color = muted, fontSize = 11.sp)
            }
            Text("${tracks.size} tracks", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        if (loading && tracks.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = accent)
                    Text("Loading music for you…", color = muted, fontSize = 12.sp)
                }
            }
        } else if (tracks.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(tracks, key = { it.id }) { track ->
                    Surface(color = panel, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = track.artworkUrl.takeIf { it.startsWith("https://") || it.startsWith("http://") },
                                contentDescription = "${track.title} artwork",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF292929))
                            )
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(track.title, color = foreground, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Independent release • check license before reuse", color = muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.licenseUrl))) } }, contentPadding = PaddingValues(0.dp)) {
                                    Text("VIEW LICENSE", color = accent, fontSize = 10.sp)
                                }
                            }
                            IconButton(onClick = { onPlayOnline(track.streamUrl, track.title, track.artist, track.artworkUrl) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play ${track.title}", tint = accent, modifier = Modifier.size(30.dp))
                            }
                        }
                    }
                }
                if (canLoadMore) {
                    item {
                        OutlinedButton(
                            onClick = { loadCatalog(query, append = true) },
                            enabled = !loadingMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (loadingMore) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (loadingMore) "LOADING MORE…" else "LOAD MORE TRACKS")
                        }
                    }
                }
            }
        } else {
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = accent, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text(if (clientId.isBlank()) "ONLINE CATALOG NOT CONFIGURED" else "NO TRACKS YET", color = foreground, fontWeight = FontWeight.Bold)
                Text("Movie and devotional results load inside the cloud panel. Independent audio streaming needs a configured Jamendo catalogue.", color = muted, fontSize = 12.sp)
                if (clientId.isNotBlank()) {
                    Button(onClick = { loadCatalog(query) }, colors = ButtonDefaults.buttonColors(containerColor = accent)) { Text("RETRY") }
                }
            }
        }
    }
}
