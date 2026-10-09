package com.narsing.music

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

private val Ink = Color(0xFF090909)
private val Panel = Color(0xFF171717)
private val Paper = Color(0xFFF4F1E8)
private val Muted = Color(0xFF9B9B95)
private val Accent = Color(0xFFE84B35)

class MainActivity : ComponentActivity() {
    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var songs by mutableStateOf<List<Song>>(emptyList())
    private var selectedIndex by mutableIntStateOf(-1)
    private var isPlaying by mutableStateOf(false)
    private var currentTitle by mutableStateOf("Choose a track")
    private var currentArtist by mutableStateOf("Your offline library")
    private var hasPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        hasPermission = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        setContent {
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasPermission = granted
                if (granted) refreshLibrary()
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Ink, surface = Panel, onBackground = Paper, onSurface = Paper)) {
                LaunchedEffect(Unit) {
                    if (!hasPermission) permissionLauncher.launch(permission) else refreshLibrary()
                    connectController()
                }
                MusicHome(
                    songs, selectedIndex, isPlaying, currentTitle, currentArtist, hasPermission,
                    onGrant = { permissionLauncher.launch(permission) },
                    onRefresh = { refreshLibrary() },
                    onPlay = { playAt(it) },
                    onToggle = { togglePlayback() },
                    onNext = { if (songs.isNotEmpty()) playAt(if (selectedIndex + 1 < songs.size) selectedIndex + 1 else 0) },
                    onPrevious = { if (songs.isNotEmpty()) playAt(if (selectedIndex > 0) selectedIndex - 1 else songs.lastIndex) }
                )
            }
        }
    }

    private fun refreshLibrary() {
        if (hasPermission) songs = runCatching { MusicRepository(this).loadSongs() }.getOrDefault(emptyList())
    }

    private fun connectController() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, token).buildAsync().also { future ->
            future.addListener({
                runCatching {
                    mediaController = future.get()
                    mediaController?.addListener(object : androidx.media3.common.Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                            currentTitle = item?.mediaMetadata?.title?.toString() ?: "Choose a track"
                            currentArtist = item?.mediaMetadata?.artist?.toString() ?: "Your offline library"
                            selectedIndex = songs.indexOfFirst { it.title == currentTitle && it.artist == currentArtist }
                        }
                    })
                }
            }, mainExecutor)
        }
    }

    private fun playAt(index: Int) {
        if (songs.isEmpty() || index !in songs.indices) return
        selectedIndex = index
        val items = songs.map { song ->
            MediaItem.Builder().setUri(song.uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album).build()
            ).build()
        }
        mediaController?.apply { setMediaItems(items, index, 0L); prepare(); play() }
        currentTitle = songs[index].title
        currentArtist = songs[index].artist
        isPlaying = true
    }

    private fun togglePlayback() { mediaController?.let { if (it.isPlaying) it.pause() else it.play() } }

    override fun onDestroy() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
        super.onDestroy()
    }
}

@Composable
private fun MusicHome(
    songs: List<Song>, selectedIndex: Int, playing: Boolean, title: String, artist: String,
    hasPermission: Boolean, onGrant: () -> Unit, onRefresh: () -> Unit,
    onPlay: (Int) -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit
) {
    var tab by remember { mutableStateOf("TRACKS") }
    var query by remember { mutableStateOf("") }
    val filtered = songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
    Surface(Modifier.fillMaxSize(), color = Ink) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NARSING / AUDIO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("MUSIC", color = Paper, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Rescan music", tint = Paper) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${songs.size} TRACKS", color = Muted, fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text("OFFLINE LIBRARY", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tracks, artists, albums", color = Muted) }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Paper, unfocusedTextColor = Paper, focusedBorderColor = Accent, unfocusedBorderColor = Color.DarkGray, cursorColor = Accent)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TRACKS", "NOW PLAYING").forEach { label ->
                    val active = tab == label
                    Surface(color = if (active) Paper else Panel, shape = RoundedCornerShape(50), modifier = Modifier.clickable { tab = label }) {
                        Text(label, color = if (active) Ink else Muted, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!hasPermission) {
                EmptyState("MUSIC ACCESS NEEDED", "Allow audio access to scan tracks stored on this phone.", "GRANT ACCESS", onGrant)
            } else if (tab == "NOW PLAYING") {
                NowPlaying(title, artist, playing, songs.getOrNull(selectedIndex)?.album ?: "LOCAL COLLECTION", onToggle, onNext, onPrevious)
            } else if (filtered.isEmpty()) {
                EmptyState("NOTHING IN THE LIBRARY", "Add audio files to your phone, then tap rescan.", "RESCAN LIBRARY", onRefresh)
            } else {
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    itemsIndexed(filtered, key = { _, song -> song.id }) { _, song ->
                        val originalIndex = songs.indexOfFirst { it.id == song.id }
                        Row(Modifier.fillMaxWidth().clickable { onPlay(originalIndex) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), color = Panel, shape = RoundedCornerShape(10.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, contentDescription = null, tint = if (originalIndex == selectedIndex) Accent else Muted) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = if (originalIndex == selectedIndex) Accent else Paper, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text(formatDuration(song.durationMs), color = Muted, fontSize = 11.sp)
                        }
                        HorizontalDivider(color = Color(0xFF292929))
                    }
                }
            }
            if (selectedIndex >= 0 && songs.getOrNull(selectedIndex) != null) {
                Surface(color = Panel, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), modifier = Modifier.fillMaxWidth().clickable { tab = "NOW PLAYING" }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), color = Color(0xFF252525), shape = RoundedCornerShape(8.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Accent) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = Paper, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            Text(artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = Paper) }
                        IconButton(onClick = onToggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Paper) }
                        IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = Paper) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlaying(title: String, artist: String, playing: Boolean, album: String, onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.fillMaxSize(), color = Color(0xFF202020), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(Modifier.fillMaxSize().padding(18.dp), color = Color(0xFF101010), shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(Modifier.size(92.dp), color = Accent, shape = CircleShape) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Paper, modifier = Modifier.size(40.dp)) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Paper, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 2)
        Text(artist, color = Muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        Text(album.uppercase(), color = Accent, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = Paper, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.width(18.dp))
            Surface(onClick = onToggle, color = Paper, shape = CircleShape, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Ink, modifier = Modifier.size(42.dp)) }
            }
            Spacer(Modifier.width(18.dp))
            IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) { Icon(Icons.Default.SkipNext, null, tint = Paper, modifier = Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EmptyState(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = Paper, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = Paper, contentColor = Ink)) { Text(action, fontWeight = FontWeight.Bold) }
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    return "%d:%02d".format(seconds / 60, seconds % 60)
}
