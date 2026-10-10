package com.narsing.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.delay

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
                    onStop = { mediaController?.pause() },
                    onNext = { mediaController?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() else if (songs.isNotEmpty()) it.seekTo(0, 0L) } },
                    onPrevious = { mediaController?.let { if (it.currentPosition > 3_000L) it.seekTo(0) else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() else if (songs.isNotEmpty()) it.seekTo(songs.lastIndex, 0L) } },
                    onShuffle = { enabled -> mediaController?.shuffleModeEnabled = enabled },
                    onRepeat = { enabled -> mediaController?.repeatMode = if (enabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF }
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
    onPlay: (Int) -> Unit, onToggle: () -> Unit, onStop: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onShuffle: (Boolean) -> Unit, onRepeat: (Boolean) -> Unit
) {
    var tab by remember { mutableStateOf("HOME") }
    var query by remember { mutableStateOf("") }
    var theme by remember { mutableStateOf("DARK") }
    var showFavorites by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(setOf<Long>()) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    var recentIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var sortMode by remember { mutableStateOf("DEFAULT") }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var sleepMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var preferencesLoaded by remember { mutableStateOf(false) }

    // Keep personal UI preferences on-device. No library files or playback data are moved.
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        theme = prefs.getString("theme", "DARK") ?: "DARK"
        favorites = prefs.getStringSet("favorites", emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
        recentIds = prefs.getString("recent_ids", "")
            ?.split(",")?.mapNotNull { it.toLongOrNull() }?.distinct()?.take(30)
            ?: emptyList()
        sortMode = prefs.getString("sort_mode", "DEFAULT") ?: "DEFAULT"
        preferencesLoaded = true
    }
    LaunchedEffect(sleepTimerMinutes) {
        if (sleepTimerMinutes > 0) {
            delay(sleepTimerMinutes * 60_000L)
            onStop()
            sleepTimerMinutes = 0
        }
    }
    LaunchedEffect(selectedIndex, songs) {
        val song = songs.getOrNull(selectedIndex) ?: return@LaunchedEffect
        recentIds = (listOf(song.id) + recentIds.filter { it != song.id }).take(30)
        context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
            .edit().putString("recent_ids", recentIds.joinToString(",")).apply()
    }
    LaunchedEffect(sortMode, preferencesLoaded) {
        if (preferencesLoaded) context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
            .edit().putString("sort_mode", sortMode).apply()
    }
    LaunchedEffect(theme, favorites, preferencesLoaded) {
        if (preferencesLoaded) {
            context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString("theme", theme)
                .putStringSet("favorites", favorites.map { it.toString() }.toSet())
                .apply()
        }
    }

    val bg = when (theme) { "LIGHT" -> Color(0xFFF4F1E8); "CUSTOM" -> Color(0xFF17111F); else -> Ink }
    val fg = if (theme == "LIGHT") Ink else Paper
    val subtle = if (theme == "LIGHT") Color(0xFF69665F) else Muted
    val sourceSongs = if (tab == "RECENT") recentIds.mapNotNull { id -> songs.find { it.id == id } } else songs
    val filtered = sourceSongs.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
        .filter { !showFavorites || it.id in favorites }
    val sortedFiltered = when (sortMode) {
        "TITLE" -> filtered.sortedBy { it.title.lowercase() }
        "ARTIST" -> filtered.sortedWith(compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        "LONGEST" -> filtered.sortedByDescending { it.durationMs }
        "SHORTEST" -> filtered.sortedBy { it.durationMs }
        else -> filtered
    }
    Surface(Modifier.fillMaxSize(), color = bg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MUSIC PRO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("MUSIC PRO", color = fg, fontSize = 34.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                }
                Box {
                    IconButton(onClick = { sleepMenuExpanded = true }) {
                        Icon(Icons.Default.Bedtime, contentDescription = "Sleep timer", tint = if (sleepTimerMinutes > 0) Accent else fg)
                    }
                    DropdownMenu(expanded = sleepMenuExpanded, onDismissRequest = { sleepMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Off") }, onClick = { sleepTimerMinutes = 0; sleepMenuExpanded = false })
                        listOf(15, 30, 45, 60, 90).forEach { minutes ->
                            DropdownMenuItem(text = { Text("Stop after $minutes minutes") }, onClick = { sleepTimerMinutes = minutes; sleepMenuExpanded = false })
                        }
                    }
                }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Rescan music", tint = fg) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${songs.size} TRACKS", color = subtle, fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text(if (sleepTimerMinutes > 0) "SLEEP TIMER · ${sleepTimerMinutes} MIN" else "OFFLINE LIBRARY", color = if (sleepTimerMinutes > 0) Accent else subtle, fontSize = 10.sp, letterSpacing = 1.sp)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search tracks, artists, albums", color = Muted) }, singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = fg, unfocusedTextColor = fg, focusedBorderColor = Accent, unfocusedBorderColor = if (theme == "LIGHT") Color.LightGray else Color.DarkGray, cursorColor = Accent)
            )
            Spacer(Modifier.height(12.dp))
            Text("QUICK ACCESS", color = subtle, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple("LIBRARY", songs.size.toString(), "TRACKS"),
                    Triple("FAVORITES", favorites.size.toString(), "FAVORITES"),
                    Triple("PLAYER", if (playing) "PLAYING" else "OPEN", "NOW PLAYING")
                ).forEach { item ->
                    Surface(
                        modifier = Modifier.weight(1f).height(72.dp).clickable {
                            tab = item.third
                            showFavorites = item.third == "FAVORITES"
                        },
                        color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(item.first, color = subtle, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, maxLines = 1)
                            Text(item.second, color = fg, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("HOME", "TRACKS", "ARTISTS", "ALBUMS", "RECENT", "FAVORITES", "NOW PLAYING").forEach { label ->
                    val active = tab == label
                    Surface(color = if (active) Accent else if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(50), modifier = Modifier.clickable { tab = label; showFavorites = label == "FAVORITES" }) {
                        Text(label, color = if (active) Color.White else subtle, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            if (tab == "HOME" && hasPermission) {
                Column(Modifier.fillMaxWidth().weight(1f)) {
                    Text("YOUR SOUNDTRACK", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text("Your music, your mood.", color = subtle, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("DARK", "LIGHT", "CUSTOM").forEach { mode ->
                            FilterChip(selected = theme == mode, onClick = { theme = mode }, label = { Text(mode) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = Color.White))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { if (songs.isNotEmpty()) onPlay(kotlin.random.Random.nextInt(songs.size)) }, colors = ButtonDefaults.buttonColors(containerColor = Accent), shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Shuffle, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("SURPRISE ME", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { tab = "TRACKS"; showFavorites = false }, shape = RoundedCornerShape(14.dp)) {
                            Text("ALL TRACKS", color = fg, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("RECENT TRACKS", color = fg, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    if (songs.isEmpty()) {
                        EmptyState("YOUR LIBRARY IS EMPTY", "Allow audio access and add music to your phone.", "RESCAN LIBRARY", onRefresh)
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                            itemsIndexed(songs.take(8), key = { _, song -> song.id }) { _, song ->
                                val ix = songs.indexOfFirst { it.id == song.id }
                                Row(Modifier.fillMaxWidth().clickable { onPlay(ix) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(Modifier.size(58.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = Accent, modifier = Modifier.size(28.dp)) }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(song.title, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, color = subtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { favorites = if (song.id in favorites) favorites - song.id else favorites + song.id }) {
                                        Icon(if (song.id in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (song.id in favorites) Accent else subtle)
                                    }
                                }
                                HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                            }
                        }
                    }
                }
            } else if (!hasPermission) {
                EmptyState("MUSIC ACCESS NEEDED", "Allow audio access to scan tracks stored on this phone.", "GRANT ACCESS", onGrant)
            } else if (tab == "NOW PLAYING") {
                NowPlaying(title, artist, playing, songs.getOrNull(selectedIndex)?.album ?: "LOCAL COLLECTION", fg, subtle, Accent, onToggle, onNext, onPrevious, onShuffle = { shuffle = !shuffle; onShuffle(shuffle) }, onRepeat = { repeat = !repeat; onRepeat(repeat) }, shuffle = shuffle, repeat = repeat)
            } else if (tab == "ARTISTS") {
                val artists = songs.map { it.artist.ifBlank { "Unknown artist" } }.distinct().sortedBy { it.lowercase() }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    item { Text("BROWSE ARTISTS", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp)) }
                    itemsIndexed(artists, key = { _, artistName -> artistName }) { _, artistName ->
                        val artistSongs = songs.filter { (it.artist.ifBlank { "Unknown artist" }) == artistName }
                        Row(Modifier.fillMaxWidth().clickable { artistSongs.firstOrNull()?.let { onPlay(songs.indexOf(it)) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(52.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = CircleShape) {
                                Box(contentAlignment = Alignment.Center) { Text(artistName.take(1).uppercase(), color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(artistName, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${artistSongs.size} tracks", color = subtle, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play artist", tint = Accent)
                        }
                        HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                    }
                }
            } else if (tab == "ALBUMS") {
                val albums = songs.groupBy { it.album.ifBlank { "Unknown album" } }.toList().sortedBy { it.first.lowercase() }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    item { Text("BROWSE ALBUMS", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp)) }
                    itemsIndexed(albums, key = { _, albumEntry -> albumEntry.first }) { _, albumEntry ->
                        val albumName = albumEntry.first
                        val albumSongs = albumEntry.second
                        Row(Modifier.fillMaxWidth().clickable { albumSongs.firstOrNull()?.let { onPlay(songs.indexOf(it)) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(52.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(albumName, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${albumSongs.size} tracks • ${albumSongs.firstOrNull()?.artist ?: "Unknown artist"}", color = subtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play album", tint = Accent)
                        }
                        HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                    }
                }
            } else if (filtered.isEmpty()) {
                EmptyState(if (tab == "RECENT") "NO RECENT TRACKS YET" else "NOTHING IN THE LIBRARY", if (tab == "RECENT") "Play a song and it will appear here." else "Add audio files to your phone, then tap rescan.", "RESCAN LIBRARY", onRefresh)
            } else {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (showFavorites || tab == "FAVORITES") "YOUR FAVORITES" else "YOUR COLLECTION", color = fg, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text("${sortedFiltered.size} tracks • ${songs.map { it.artist }.distinct().size} artists", color = subtle, fontSize = 12.sp)
                    }
                    Box {
                        OutlinedButton(onClick = { sortMenuExpanded = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = null, tint = fg)
                            Spacer(Modifier.width(4.dp))
                            Text(when (sortMode) { "TITLE" -> "Title"; "ARTIST" -> "Artist"; "LONGEST" -> "Longest"; "SHORTEST" -> "Shortest"; else -> "Sort" }, color = fg)
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            listOf("DEFAULT" to "Library order", "TITLE" to "Title A–Z", "ARTIST" to "Artist A–Z", "LONGEST" to "Longest first", "SHORTEST" to "Shortest first").forEach { (mode, label) ->
                                DropdownMenuItem(text = { Text(label) }, onClick = { sortMode = mode; sortMenuExpanded = false })
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    itemsIndexed(sortedFiltered, key = { _, song -> song.id }) { _, song ->
                        val originalIndex = songs.indexOfFirst { it.id == song.id }
                        Row(Modifier.fillMaxWidth().clickable { onPlay(originalIndex) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), color = Panel, shape = RoundedCornerShape(10.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, contentDescription = null, tint = if (originalIndex == selectedIndex) Accent else Muted) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = if (originalIndex == selectedIndex) Accent else fg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = subtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { favorites = if (song.id in favorites) favorites - song.id else favorites + song.id }) { Icon(if (song.id in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (song.id in favorites) Accent else subtle) }
                            Text(formatDuration(song.durationMs), color = subtle, fontSize = 11.sp)
                        }
                        HorizontalDivider(color = Color(0xFF292929))
                    }
                }
            }
            if (selectedIndex >= 0 && songs.getOrNull(selectedIndex) != null) {
                Surface(color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), modifier = Modifier.fillMaxWidth().clickable { tab = "NOW PLAYING" }) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(42.dp), color = Color(0xFF252525), shape = RoundedCornerShape(8.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Accent) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                            Text(artist, color = subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = fg) }
                        IconButton(onClick = onToggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = fg) }
                        IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = fg) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.NowPlaying(title: String, artist: String, playing: Boolean, album: String, foreground: Color, subtleColor: Color, accentColor: Color, onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit, shuffle: Boolean, repeat: Boolean) {
    Column(Modifier.fillMaxWidth().weight(1f).padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(12.dp), contentAlignment = Alignment.Center) {
            Surface(Modifier.fillMaxSize(), color = Color(0xFF202020), shape = CircleShape) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(Modifier.fillMaxSize().padding(18.dp), color = Color(0xFF101010), shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(Modifier.size(92.dp), color = accentColor, shape = CircleShape) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = foreground, modifier = Modifier.size(40.dp)) }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 2)
        Text(artist, color = subtleColor, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        Text(album.uppercase(), color = accentColor, fontSize = 10.sp, letterSpacing = 2.sp, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, modifier = Modifier.size(58.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = foreground, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.width(18.dp))
            Surface(onClick = onToggle, color = foreground, shape = CircleShape, modifier = Modifier.size(76.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Ink, modifier = Modifier.size(42.dp)) }
            }
            Spacer(Modifier.width(18.dp))
            IconButton(onClick = onNext, modifier = Modifier.size(58.dp)) { Icon(Icons.Default.SkipNext, null, tint = foreground, modifier = Modifier.size(34.dp)) }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, null, tint = if (shuffle) accentColor else subtleColor, modifier = Modifier.size(26.dp)) }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, null, tint = foreground, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = onToggle) { Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = foreground, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, null, tint = foreground, modifier = Modifier.size(30.dp)) }
            IconButton(onClick = onRepeat) { Icon(Icons.Default.Repeat, null, tint = if (repeat) accentColor else subtleColor, modifier = Modifier.size(26.dp)) }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ColumnScope.EmptyState(title: String, description: String, action: String, onAction: () -> Unit) {
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
