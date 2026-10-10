package com.narsing.music

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.app.Activity
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
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
import kotlin.math.PI
import kotlin.math.atan2

private val Ink = Color(0xFF090909)
private val Panel = Color(0xFF171717)
private val Paper = Color(0xFFF4F1E8)
private val Muted = Color(0xFF9B9B95)
private val Accent = Color(0xFFE84B35)

private data class QueueEntry(val index: Int, val title: String, val artist: String, val isCurrent: Boolean)
private fun splitArtists(value: String): List<String> = value.split(Regex("""\s*(?:;|,|\s&\s|\bfeat\.?\s+|\bft\.?\s+|\bfeaturing\s+)\s*""", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
private data class TimedLyric(val timeMs: Long, val text: String)
private fun parseTimedLyrics(raw: String): List<TimedLyric> {
    val pattern = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")
    return raw.lineSequence().flatMap { line ->
        val matches = pattern.findAll(line).toList()
        val lyric = pattern.replace(line, "").trim()
        if (matches.isEmpty() || lyric.isBlank()) emptySequence() else matches.asSequence().map { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            TimedLyric((minutes * 60L + seconds) * 1000L + fraction, lyric)
        }
    }.sortedBy { it.timeMs }.toList()
}

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
            var fontChoice by remember { mutableStateOf("SANS") }
            LaunchedEffect(Unit) { fontChoice = getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).getString("font_choice", "SANS") ?: "SANS" }
            val appFontFamily = when (fontChoice) {
                "SERIF" -> FontFamily.Serif
                "MONOSPACE" -> FontFamily.Monospace
                "CURSIVE" -> FontFamily.Cursive
                else -> FontFamily.SansSerif
            }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasPermission = granted
                if (granted) refreshLibrary()
            }
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Ink, surface = Panel, onBackground = Paper, onSurface = Paper)) {
                LaunchedEffect(Unit) {
                    if (!hasPermission) permissionLauncher.launch(permission) else refreshLibrary()
                    connectController()
                }
                val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                    if (result.resultCode == Activity.RESULT_OK) refreshLibrary()
                }
                CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = appFontFamily)) {
                MusicHome(
                    songs, selectedIndex, isPlaying, currentTitle, currentArtist, hasPermission,
                    onFontChoiceChange = { choice -> fontChoice = choice; getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).edit().putString("font_choice", choice).apply() },
                    onGrant = { permissionLauncher.launch(permission) },
                    onRefresh = { refreshLibrary() },
                    onPlay = { playAt(it) },
                    onPlayCollection = { indices, shuffled -> playCollection(indices, shuffled) },
                    onPlayNext = { index -> playNext(index) },
                    onShare = { song -> shareSong(song) },
                    onToggle = { togglePlayback() },
                    onStop = { mediaController?.pause() },
                    onNext = { mediaController?.let { if (it.hasNextMediaItem()) it.seekToNextMediaItem() else if (songs.isNotEmpty()) it.seekTo(0, 0L) } },
                    onPrevious = { mediaController?.let { if (it.currentPosition > 3_000L) it.seekTo(0) else if (it.hasPreviousMediaItem()) it.seekToPreviousMediaItem() else if (songs.isNotEmpty()) it.seekTo(songs.lastIndex, 0L) } },
                    onShuffle = { enabled -> mediaController?.shuffleModeEnabled = enabled },
                    onRepeat = { enabled -> mediaController?.repeatMode = if (enabled) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF },
                    onProgress = { playbackProgress() },
                    onSeek = { position -> mediaController?.seekTo(position) },
                    onSpeed = { speed -> mediaController?.setPlaybackSpeed(speed) },
                    onDelete = { song -> requestDeleteSong(song, deleteLauncher) },
                    onGetQueue = { mediaController?.let { controller -> (0 until controller.mediaItemCount).map { index ->
                        val item = controller.getMediaItemAt(index)
                        QueueEntry(index, item.mediaMetadata.title?.toString().orEmpty().ifBlank { "Unknown track" }, item.mediaMetadata.artist?.toString().orEmpty().ifBlank { "Unknown artist" }, index == controller.currentMediaItemIndex)
                    } } ?: emptyList() },
                    onPlayQueueItem = { index -> mediaController?.let { it.seekTo(index, 0L); it.play() } },
                    onRemoveQueueItem = { index -> mediaController?.let { controller -> if (index in 0 until controller.mediaItemCount) controller.removeMediaItem(index) } },
                    onMoveQueueItem = { from, to -> mediaController?.let { controller -> if (from in 0 until controller.mediaItemCount && to in 0 until controller.mediaItemCount) controller.moveMediaItem(from, to) } }
                )
                }
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
                            // Match by URI first so duplicate titles/artists don't desync the UI
                            // when the player automatically advances through its queued tracks.
                            val transitionedUri = item?.localConfiguration?.uri
                            selectedIndex = songs.indexOfFirst { song ->
                                (transitionedUri != null && song.uri == transitionedUri) ||
                                    (song.title == currentTitle && song.artist == currentArtist)
                            }
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

    private fun playCollection(indices: List<Int>, shuffled: Boolean) {
        val validIndices = indices.filter { it in songs.indices }.distinct()
        if (validIndices.isEmpty()) return
        val order = if (shuffled) validIndices.shuffled() else validIndices
        val items = order.map { index ->
            val song = songs[index]
            MediaItem.Builder().setUri(song.uri).setMediaMetadata(
                MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album).build()
            ).build()
        }
        mediaController?.apply { setMediaItems(items, 0, 0L); prepare(); play() }
        selectedIndex = order.first()
        currentTitle = songs[selectedIndex].title
        currentArtist = songs[selectedIndex].artist
        isPlaying = true
    }

    private fun playNext(index: Int) {
        if (index !in songs.indices) return
        val song = songs[index]
        val item = MediaItem.Builder().setUri(song.uri).setMediaMetadata(
            MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).setAlbumTitle(song.album).build()
        ).build()
        mediaController?.let { controller ->
            val insertAt = if (controller.mediaItemCount == 0) 0 else (controller.currentMediaItemIndex + 1).coerceAtLeast(0)
            controller.addMediaItem(insertAt, item)
            if (controller.mediaItemCount == 1 && !controller.isPlaying) controller.prepare()
        }
    }

    private fun requestDeleteSong(song: Song, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                val request = MediaStore.createDeleteRequest(contentResolver, listOf(song.uri))
                launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
            }.onFailure {
                Toast.makeText(this, "Unable to request deletion for this audio file", Toast.LENGTH_LONG).show()
            }
        } else {
            try {
                val deleted = contentResolver.delete(song.uri, null, null)
                if (deleted > 0) refreshLibrary()
                else Toast.makeText(this, "The file could not be deleted. Check storage access.", Toast.LENGTH_LONG).show()
            } catch (security: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && security is android.app.RecoverableSecurityException) {
                    runCatching { launcher.launch(IntentSenderRequest.Builder(security.userAction.actionIntent.intentSender).build()) }
                        .onFailure { Toast.makeText(this, "Deletion permission was not granted", Toast.LENGTH_LONG).show() }
                } else {
                    Toast.makeText(this, "Android denied deletion. Use your file manager to remove this file.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun shareSong(song: Song) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, song.uri)
            putExtra(Intent.EXTRA_SUBJECT, song.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { startActivity(Intent.createChooser(sendIntent, "Share ${song.title}")) }
    }

    private fun playbackProgress(): Pair<Long, Long> {
        val controller = mediaController ?: return 0L to 0L
        val duration = controller.duration.takeIf { it > 0L && it != androidx.media3.common.C.TIME_UNSET } ?: 0L
        return controller.currentPosition.coerceAtLeast(0L) to duration
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
    hasPermission: Boolean, onFontChoiceChange: (String) -> Unit, onGrant: () -> Unit, onRefresh: () -> Unit,
    onPlay: (Int) -> Unit, onPlayCollection: (List<Int>, Boolean) -> Unit, onPlayNext: (Int) -> Unit, onShare: (Song) -> Unit, onToggle: () -> Unit, onStop: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
    onShuffle: (Boolean) -> Unit, onRepeat: (Boolean) -> Unit,
    onProgress: () -> Pair<Long, Long>, onSeek: (Long) -> Unit, onSpeed: (Float) -> Unit,
    onDelete: (Song) -> Unit, onGetQueue: () -> List<QueueEntry>, onPlayQueueItem: (Int) -> Unit, onRemoveQueueItem: (Int) -> Unit, onMoveQueueItem: (Int, Int) -> Unit
) {
    var tab by remember { mutableStateOf("HOME") }
    var query by remember { mutableStateOf("") }
    var durationFilter by remember { mutableStateOf("ALL") }
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
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var fontMenuExpanded by remember { mutableStateOf(false) }
    var fontChoice by remember { mutableStateOf("SANS") }
    var equalizerLevels by remember { mutableStateOf(listOf(0f, 0f, 0f, 0f, 0f)) }
    var playlists by remember { mutableStateOf<Map<String, List<Long>>>(emptyMap()) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var activePlaylist by remember { mutableStateOf<String?>(null) }
    var addToPlaylistSong by remember { mutableStateOf<Song?>(null) }
    var detailsSong by remember { mutableStateOf<Song?>(null) }
    var editMetadataSong by remember { mutableStateOf<Song?>(null) }
    var metadataDraftTitle by remember { mutableStateOf("") }
    var metadataDraftArtist by remember { mutableStateOf("") }
    var metadataDraftAlbum by remember { mutableStateOf("") }
    var lyricsDialogSong by remember { mutableStateOf<Song?>(null) }
    var lyricsDraft by remember { mutableStateOf("") }
    var lyricsFetchLoading by remember { mutableStateOf(false) }
    var lyricsFetchMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var selectedTrackIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var deleteSong by remember { mutableStateOf<Song?>(null) }
    val context = LocalContext.current
    var preferencesLoaded by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var playbackDuration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var showQueueDialog by remember { mutableStateOf(false) }
    var miniPlayerDismissed by remember { mutableStateOf(false) }
    var queueEntries by remember { mutableStateOf<List<QueueEntry>>(emptyList()) }
    var pendingExportPlaylistName by remember { mutableStateOf<String?>(null) }
    var artworkTargetKey by remember { mutableStateOf<String?>(null) }
    var artworkTargetLabel by remember { mutableStateOf("") }
    var artworkUris by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val artworkPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selectedUri ->
        val targetKey = artworkTargetKey
        val targetLabel = artworkTargetLabel
        artworkTargetKey = null
        if (selectedUri != null && targetKey != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            artworkUris = artworkUris + (targetKey to selectedUri.toString())
            context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).edit()
                .putString("artwork_$targetKey", selectedUri.toString()).apply()
            Toast.makeText(context, "Artwork updated for $targetLabel", Toast.LENGTH_SHORT).show()
        }
    }

    val lyricsImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { inputUri ->
        if (inputUri != null) {
            runCatching {
                lyricsDraft = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readText().take(20000) }
                    ?: error("Could not read the selected lyrics file")
                Toast.makeText(context, "Lyrics file loaded", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(context, "Lyrics import failed: ${it.message ?: "unknown error"}", Toast.LENGTH_LONG).show() }
        }
    }

    val exportPlaylistLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { outputUri ->
        val playlistName = pendingExportPlaylistName
        pendingExportPlaylistName = null
        if (outputUri != null && playlistName != null) {
            runCatching {
                val entries = playlists[playlistName].orEmpty().mapNotNull { id -> songs.firstOrNull { it.id == id } }
                val lines = mutableListOf("#EXTM3U")
                entries.forEach { song ->
                    lines += "#EXTINF:${(song.durationMs / 1000L).coerceAtLeast(0L)},${song.title} - ${song.artist}"
                    lines += song.uri.toString()
                }
                context.contentResolver.openOutputStream(outputUri)?.bufferedWriter()?.use { it.write(lines.joinToString("\n")) }
                    ?: error("Could not open the selected file")
                Toast.makeText(context, "Exported ${entries.size} tracks", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(context, "Playlist export failed: ${it.message ?: "unknown error"}", Toast.LENGTH_LONG).show() }
        }
    }
    val importPlaylistLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { inputUri ->
        if (inputUri != null) {
            runCatching {
                val lines = context.contentResolver.openInputStream(inputUri)?.bufferedReader()?.use { it.readLines() }
                    ?: error("Could not read the selected playlist")
                var pendingTitle: String? = null
                val matchedIds = mutableListOf<Long>()
                lines.forEach { raw ->
                    val line = raw.trim()
                    when {
                        line.startsWith("#EXTINF:", ignoreCase = true) -> pendingTitle = line.substringAfter(',', "").trim()
                        line.isNotEmpty() && !line.startsWith("#") -> {
                            val exact = songs.firstOrNull { it.uri.toString() == line || it.filePath.equals(line.removePrefix("file://"), ignoreCase = true) || (line.startsWith("/") && it.filePath.endsWith(line, ignoreCase = true)) }
                            val titleAndArtist = pendingTitle?.split(" - ", limit = 2)
                            val match = exact ?: songs.firstOrNull { song ->
                                titleAndArtist?.getOrNull(0)?.equals(song.title, ignoreCase = true) == true &&
                                    (titleAndArtist.getOrNull(1).isNullOrBlank() || titleAndArtist?.getOrNull(1)?.equals(song.artist, ignoreCase = true) == true)
                            } ?: songs.firstOrNull { song -> pendingTitle?.equals(song.title, ignoreCase = true) == true }
                            if (match != null && match.id !in matchedIds) matchedIds += match.id
                            pendingTitle = null
                        }
                    }
                }
                if (matchedIds.isEmpty()) error("No playlist tracks matched music on this device")
                val suggestedName = inputUri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "Imported playlist"
                var name = suggestedName.take(40)
                var suffix = 2
                while (name in playlists) { name = "${suggestedName.take(34)} (${suffix++})" }
                playlists = playlists + (name to matchedIds)
                activePlaylist = name
                tab = "PLAYLISTS"
                Toast.makeText(context, "Imported ${matchedIds.size} tracks", Toast.LENGTH_SHORT).show()
            }.onFailure { Toast.makeText(context, "Playlist import failed: ${it.message ?: "unknown error"}", Toast.LENGTH_LONG).show() }
        }
    }

    LaunchedEffect(selectedIndex) { miniPlayerDismissed = false }

    LaunchedEffect(selectedIndex, playing) {
        while (selectedIndex >= 0) {
            val progress = onProgress()
            playbackPosition = progress.first.coerceAtMost(progress.second.takeIf { it > 0L } ?: Long.MAX_VALUE)
            playbackDuration = progress.second
            delay(500L)
        }
    }

    LaunchedEffect(songs) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        artworkUris = prefs.all.entries.filter { it.key.startsWith("artwork_") && it.value is String }
            .associate { it.key.removePrefix("artwork_") to (it.value as String) }
    }

    // Keep personal UI preferences on-device. No library files or playback data are moved.
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
        theme = prefs.getString("theme", "DARK") ?: "DARK"
        fontChoice = prefs.getString("font_choice", "SANS") ?: "SANS"
        playbackSpeed = prefs.getFloat("playback_speed", 1f).coerceIn(0.5f, 2f)
        equalizerLevels = (prefs.getString("equalizer_levels", "0,0,0,0,0") ?: "0,0,0,0,0").split(",").mapNotNull { it.toFloatOrNull() }.take(5).let { values -> (values + List((5 - values.size).coerceAtLeast(0)) { 0f }).take(5) }
        shuffle = prefs.getBoolean("shuffle_enabled", false)
        repeat = prefs.getBoolean("repeat_enabled", false)
        favorites = prefs.getStringSet("favorites", emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()
        recentIds = prefs.getString("recent_ids", "")
            ?.split(",")?.mapNotNull { it.toLongOrNull() }?.distinct()?.take(30)
            ?: emptyList()
        sortMode = prefs.getString("sort_mode", "DEFAULT") ?: "DEFAULT"
        playlists = prefs.getStringSet("playlists", emptySet())?.mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) parts[0] to parts[1].split(",").mapNotNull { it.toLongOrNull() }.distinct() else null
        }?.toMap() ?: emptyMap()
        preferencesLoaded = true
    }
    LaunchedEffect(shuffle, preferencesLoaded) {
        if (preferencesLoaded) {
            onShuffle(shuffle)
            context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                .edit().putBoolean("shuffle_enabled", shuffle).apply()
        }
    }
    LaunchedEffect(repeat, preferencesLoaded) {
        if (preferencesLoaded) {
            onRepeat(repeat)
            context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                .edit().putBoolean("repeat_enabled", repeat).apply()
        }
    }
    LaunchedEffect(favorites, preferencesLoaded) {
        if (preferencesLoaded) context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
            .edit().putStringSet("favorites", favorites.map { it.toString() }.toSet()).apply()
    }
    LaunchedEffect(playbackSpeed, preferencesLoaded) {
        if (preferencesLoaded) {
            onSpeed(playbackSpeed)
            context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                .edit().putFloat("playback_speed", playbackSpeed).apply()
        }
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
    LaunchedEffect(playlists, preferencesLoaded) {
        if (preferencesLoaded) context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
            .edit().putStringSet("playlists", playlists.map { (name, ids) -> "$name::${ids.joinToString(",")}" }.toSet()).apply()
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
    val sourceSongs = when (tab) {
        "RECENT" -> recentIds.mapNotNull { id -> songs.find { it.id == id } }
        "PLAYLISTS" -> activePlaylist?.let { name -> playlists[name].orEmpty().mapNotNull { id -> songs.find { it.id == id } } } ?: songs
        else -> songs
    }
    val filtered = sourceSongs.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
        .filter { !showFavorites || it.id in favorites }
        .filter { song -> when (durationFilter) { "SHORT" -> song.durationMs < 180_000L; "MEDIUM" -> song.durationMs in 180_000L..360_000L; "LONG" -> song.durationMs > 360_000L; else -> true } }
    val sortedFiltered = when (sortMode) {
        "TITLE" -> filtered.sortedBy { it.title.lowercase() }
        "ARTIST" -> filtered.sortedWith(compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() })
        "LONGEST" -> filtered.sortedByDescending { it.durationMs }
        "SHORTEST" -> filtered.sortedBy { it.durationMs }
        "NEWEST" -> filtered.sortedByDescending { it.dateAddedSeconds }
        else -> filtered
    }
    Surface(Modifier.fillMaxSize(), color = bg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            if (tab != "NOW PLAYING") {
            Row(Modifier.fillMaxWidth().padding(top = if (tab == "HOME") 24.dp else 8.dp, bottom = if (tab == "HOME") 18.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MUSIC PRO", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text(if (tab == "HOME") "MUSIC PRO" else when (tab) { "TRACKS" -> "All songs"; "ALBUMS" -> "Albums"; "ARTISTS" -> "Artists"; "PLAYLISTS" -> "Playlists"; "FAVORITES" -> "Favorites"; "RECENT" -> "Recently played"; else -> tab.lowercase().replaceFirstChar { it.uppercase() } }, color = fg, fontSize = if (tab == "HOME") 34.sp else 25.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.6).sp)
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
                Box {
                    TextButton(onClick = { fontMenuExpanded = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)) { Text("Aa", color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    DropdownMenu(expanded = fontMenuExpanded, onDismissRequest = { fontMenuExpanded = false }) {
                        listOf("SANS" to "Sans serif", "SERIF" to "Serif", "MONOSPACE" to "Monospace", "CURSIVE" to "Cursive").forEach { (choice, label) ->
                            DropdownMenuItem(text = { Text(if (fontChoice == choice) "✓ $label" else label) }, onClick = { fontChoice = choice; onFontChoiceChange(choice); fontMenuExpanded = false })
                        }
                    }
                }
                IconButton(onClick = { showEqualizerDialog = true }) { Icon(Icons.Default.Tune, contentDescription = "Equalizer", tint = if (equalizerLevels.any { it != 0f }) Accent else fg) }
                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Narsing Music")
                        putExtra(Intent.EXTRA_TEXT, "🎧 Discover Narsing Music — a modern Android music player for music stored on your phone. Explore the project and try it: https://github.com/Narsing-s/Music-pro")
                    }
                    runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share Narsing Music")) }
                }) { Icon(Icons.Default.Share, contentDescription = "Share Narsing Music", tint = Accent) }
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "Rescan music", tint = fg) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { value ->
                    query = value
                    if (value.isNotBlank() && tab == "HOME") {
                        tab = "TRACKS"
                        showFavorites = false
                        activePlaylist = null
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                placeholder = { Text("Search tracks, artists, albums", color = Muted) },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = subtle)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = fg, unfocusedTextColor = fg,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = if (theme == "LIGHT") Color.LightGray else Color.DarkGray,
                    cursorColor = Accent
                )
            )
            if (tab == "TRACKS") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("ALL" to "All", "SHORT" to "<3 min", "MEDIUM" to "3–6 min", "LONG" to ">6 min").forEach { (filter, label) ->
                        val active = durationFilter == filter
                        Surface(color = if (active) Accent else if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(50), modifier = Modifier.clickable { durationFilter = filter }) {
                            Text(label, color = if (active) Color.White else fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
            if (tab == "HOME") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${songs.size} TRACKS", color = subtle, fontSize = 11.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.weight(1f))
                Text(if (sleepTimerMinutes > 0) "SLEEP TIMER · ${sleepTimerMinutes} MIN" else "OFFLINE LIBRARY", color = if (sleepTimerMinutes > 0) Accent else subtle, fontSize = 10.sp, letterSpacing = 1.sp)
            }
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
                        modifier = Modifier.weight(1f).height(92.dp).clickable {
                            tab = item.third
                            showFavorites = item.third == "FAVORITES"
                        },
                        color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Text(item.first, color = subtle, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp, maxLines = 1)
                            Text(item.second, color = fg, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            }
            Spacer(Modifier.height(4.dp))
            }
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
                    Spacer(Modifier.height(12.dp))
                    val quickListenIndices = songs.indices.filter { songs[it].durationMs in 1L..179_999L }
                    val longSessionIndices = songs.indices.filter { songs[it].durationMs > 360_000L }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (theme == "LIGHT") Color(0xFFF0ECE2) else Color(0xFF111111),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("QUICK MIXES", color = fg, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 13.sp)
                                    Text("Pick a mood by track length", color = subtle, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Accent)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    Triple("QUICK LISTEN", "Under 3 min", quickListenIndices),
                                    Triple("LONG SESSION", "Over 6 min", longSessionIndices)
                                ).forEach { mix ->
                                    Surface(
                                        onClick = {
                                            if (mix.third.isNotEmpty()) {
                                                query = ""
                                                showFavorites = false
                                                activePlaylist = null
                                                tab = "TRACKS"
                                                onPlayCollection(mix.third.shuffled(), true)
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(Modifier.padding(12.dp)) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Accent)
                                            Spacer(Modifier.height(6.dp))
                                            Text(mix.first, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 0.4.sp)
                                            Text(mix.second, color = subtle, fontSize = 10.sp)
                                            Text("${mix.third.size} tracks", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    val recentlyAddedIndices = songs.indices
                        .sortedByDescending { songs[it].dateAddedSeconds }
                        .take(20)
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = {
                            if (recentlyAddedIndices.isNotEmpty()) {
                                query = ""
                                showFavorites = false
                                activePlaylist = null
                                tab = "TRACKS"
                                onPlayCollection(recentlyAddedIndices, false)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Accent.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Accent, modifier = Modifier.padding(9.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("JUST ADDED", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                                Text("Play your newest music first", color = subtle, fontSize = 11.sp)
                            }
                            Text("${recentlyAddedIndices.size} tracks", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    val dailyMixIndices = (
                        recentIds.mapNotNull { id ->
                            songs.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                        } +
                            songs.indices.filter { songs[it].id in favorites } +
                            songs.indices.sortedByDescending { songs[it].dateAddedSeconds }.take(10)
                        ).distinct()
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = {
                            if (dailyMixIndices.isNotEmpty()) {
                                query = ""
                                showFavorites = false
                                activePlaylist = null
                                tab = "TRACKS"
                                onPlayCollection(dailyMixIndices.shuffled(), true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Accent.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Shuffle, contentDescription = null, tint = Accent, modifier = Modifier.padding(9.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("DAILY MIX", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                                Text("Recent plays + favorites + new tracks", color = subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${dailyMixIndices.size} tracks", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    val radioArtist = songs.getOrNull(selectedIndex)?.artist?.trim().orEmpty()
                    val artistRadioIndices = if (radioArtist.isNotBlank()) {
                        songs.indices.filter { songs[it].artist.trim().equals(radioArtist, ignoreCase = true) }
                    } else emptyList()
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (artistRadioIndices.isNotEmpty()) {
                                query = ""
                                showFavorites = false
                                activePlaylist = null
                                tab = "TRACKS"
                                onPlayCollection(artistRadioIndices.shuffled(), true)
                            }
                        },
                        enabled = artistRadioIndices.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text("ARTIST RADIO", color = fg, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(if (radioArtist.isNotBlank()) "Shuffle songs by $radioArtist" else "Play a song to start a station", color = subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${artistRadioIndices.size}", color = Accent, fontWeight = FontWeight.Bold)
                    }
                    val currentAlbum = songs.getOrNull(selectedIndex)?.album?.trim().orEmpty()
                    val albumFlowIndices = if (currentAlbum.isNotBlank()) {
                        songs.indices.filter { songs[it].album.trim().equals(currentAlbum, ignoreCase = true) }
                            .sortedBy { songs[it].dateAddedSeconds }
                    } else emptyList()
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (albumFlowIndices.isNotEmpty()) {
                                query = ""
                                showFavorites = false
                                activePlaylist = null
                                tab = "TRACKS"
                                onPlayCollection(albumFlowIndices, false)
                            }
                        },
                        enabled = albumFlowIndices.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Album, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text("ALBUM FLOW", color = fg, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(if (currentAlbum.isNotBlank()) "Play through $currentAlbum" else "Play a song to start an album", color = subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("${albumFlowIndices.size}", color = Accent, fontWeight = FontWeight.Bold)
                    }
                    val focusMixIndices = songs.indices.filter { songs[it].durationMs in 180_000L..360_000L }
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = {
                            if (focusMixIndices.isNotEmpty()) {
                                query = ""
                                showFavorites = false
                                activePlaylist = null
                                tab = "TRACKS"
                                onPlayCollection(focusMixIndices.shuffled(), true)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Accent.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)) {
                                Icon(Icons.Default.Bedtime, contentDescription = null, tint = Accent, modifier = Modifier.padding(9.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("FOCUS MIX", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 0.7.sp)
                                Text("3–6 minute tracks for a steady session", color = subtle, fontSize = 11.sp)
                            }
                            Text("${focusMixIndices.size} tracks", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { tab = "TRACKS"; showFavorites = false }, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.LibraryMusic, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("ALL TRACKS", color = fg, fontWeight = FontWeight.Bold)
                        }
                    }
                    val recentTrackIndices = recentIds.mapNotNull { recentId -> songs.indexOfFirst { it.id == recentId }.takeIf { it >= 0 } }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            if (recentTrackIndices.isNotEmpty()) {
                                tab = "TRACKS"
                                showFavorites = false
                                activePlaylist = null
                                onPlayCollection(recentTrackIndices, false)
                            } else if (selectedIndex >= 0) {
                                onPlay(selectedIndex)
                            }
                        },
                        enabled = recentTrackIndices.isNotEmpty() || selectedIndex >= 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("PICK UP WHERE YOU LEFT OFF", color = fg, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(if (recentTrackIndices.isNotEmpty()) "${recentTrackIndices.size} recently played tracks" else "Resume your current track", color = subtle, fontSize = 11.sp)
                        }
                    }
                    val favoriteTrackIndices = songs.indices.filter { songs[it].id in favorites }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            if (favoriteTrackIndices.isNotEmpty()) {
                                query = ""
                                tab = "TRACKS"
                                showFavorites = false
                                activePlaylist = null
                                onPlayCollection(favoriteTrackIndices.shuffled(), true)
                            }
                        },
                        enabled = favoriteTrackIndices.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("FAVORITES MIX", color = fg, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text(if (favoriteTrackIndices.isNotEmpty()) "${favoriteTrackIndices.size} saved tracks • shuffled for you" else "Save songs with the heart icon to build your mix", color = subtle, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (selectedIndex >= 0 && songs.getOrNull(selectedIndex) != null) {
                        val currentSong = songs[selectedIndex]
                        Surface(
                            color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().clickable { tab = "NOW PLAYING" }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(Modifier.size(48.dp), color = Accent.copy(alpha = 0.16f), shape = RoundedCornerShape(12.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(26.dp)) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("CONTINUE LISTENING", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                                    Text(currentSong.title, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(currentSong.artist, color = subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Open player", tint = Accent, modifier = Modifier.size(28.dp))
                            }
                        }
                        val miniProgress = if (playbackDuration > 0L) (playbackPosition.toFloat() / playbackDuration.toFloat()).coerceIn(0f, 1f) else 0f
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth().height(3.dp).background(if (theme == "LIGHT") Color(0xFFD2CCBE) else Color(0xFF333333), RoundedCornerShape(50))
                        ) {
                            Box(Modifier.fillMaxWidth(miniProgress).fillMaxHeight().background(Accent, RoundedCornerShape(50)))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Text("RECENTLY PLAYED", color = fg, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Pick up where you left off", color = subtle, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp, bottom = 4.dp))
                    if (songs.isEmpty()) {
                        EmptyState("YOUR LIBRARY IS EMPTY", "Allow audio access and add music to your phone.", "RESCAN LIBRARY", onRefresh)
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                            itemsIndexed(recentIds.mapNotNull { id -> songs.firstOrNull { it.id == id } }.ifEmpty { songs }.take(8), key = { _, song -> song.id }) { _, song ->
                                val ix = songs.indexOfFirst { it.id == song.id }
                                Row(Modifier.fillMaxWidth().clickable { tab = "NOW PLAYING"; onPlay(ix) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(Modifier.size(58.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, null, tint = Accent, modifier = Modifier.size(28.dp)) }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(song.title, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(song.artist, color = subtle, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                NowPlaying(title, artist, songs.getOrNull(selectedIndex)?.album ?: "LOCAL COLLECTION", playing, fg, subtle, Accent, artworkUris["track_${songs.getOrNull(selectedIndex)?.id}"], { tab = "HOME" }, { songs.getOrNull(selectedIndex)?.let { artworkTargetKey = "track_${it.id}"; artworkTargetLabel = it.title; artworkPickerLauncher.launch(arrayOf("image/*")) } }, onShareTrack = {
                    val currentSong = songs.getOrNull(selectedIndex)
                    if (currentSong != null) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Now playing: ${currentSong.title}")
                            putExtra(Intent.EXTRA_TEXT, "🎶 NOW PLAYING\n${currentSong.title} — ${currentSong.artist}\nShared from Narsing Music\nhttps://github.com/Narsing-s/Music-pro")
                        }
                        runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share listening card")) }
                    }
                }, onToggle, onNext, onPrevious, onShuffle = { shuffle = !shuffle; onShuffle(shuffle) }, onRepeat = { repeat = !repeat; onRepeat(repeat) }, shuffle = shuffle, repeat = repeat, positionMs = playbackPosition, durationMs = playbackDuration, onSeek = onSeek, playbackSpeed = playbackSpeed, onSpeedChange = { playbackSpeed = it }, onOpenQueue = { queueEntries = onGetQueue(); showQueueDialog = true }, isFavorite = songs.getOrNull(selectedIndex)?.id in favorites, onToggleFavorite = { songs.getOrNull(selectedIndex)?.let { song -> favorites = if (song.id in favorites) favorites - song.id else favorites + song.id } }, onOpenLyrics = { songs.getOrNull(selectedIndex)?.let { song -> lyricsDraft = context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).getString("lyrics_${song.id}", "") ?: ""; lyricsDialogSong = song } }, hasLyrics = songs.getOrNull(selectedIndex)?.let { context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).contains("lyrics_${it.id}") } == true, lyricsText = songs.getOrNull(selectedIndex)?.let { context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).getString("lyrics_${it.id}", "") }.orEmpty())
            } else if (tab == "ARTISTS") {
                val artists = songs.flatMap { splitArtists(it.artist.ifBlank { "Unknown artist" }) }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    item { Text("BROWSE ARTISTS", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp)) }
                    itemsIndexed(artists, key = { _, artistName -> artistName }) { _, artistName ->
                        val artistSongs = songs.filter { artistName in splitArtists(it.artist.ifBlank { "Unknown artist" }) }
                        Row(Modifier.fillMaxWidth().clickable { artistSongs.firstOrNull()?.let { tab = "NOW PLAYING"; onPlay(songs.indexOf(it)) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(60.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = CircleShape) {
                                val art = artworkUris["artist_$artistName"]
                                if (art != null) AsyncImage(model = art, contentDescription = "$artistName artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                else Box(contentAlignment = Alignment.Center) { Text(artistName.take(1).uppercase(), color = Accent, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(artistName, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${artistSongs.size} tracks", color = subtle, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play artist", tint = Accent)
                            IconButton(onClick = { artworkTargetKey = "artist_$artistName"; artworkTargetLabel = artistName; artworkPickerLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Default.Image, contentDescription = "Change artist artwork", tint = subtle) }
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
                        Row(Modifier.fillMaxWidth().clickable { albumSongs.firstOrNull()?.let { tab = "NOW PLAYING"; onPlay(songs.indexOf(it)) } }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(60.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                val art = artworkUris["album_$albumName"]
                                if (art != null) AsyncImage(model = art, contentDescription = "$albumName artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Album, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(albumName, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${albumSongs.size} tracks • ${albumSongs.firstOrNull()?.artist ?: "Unknown artist"}", color = subtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play album", tint = Accent)
                            IconButton(onClick = { artworkTargetKey = "album_$albumName"; artworkTargetLabel = albumName; artworkPickerLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Default.Image, contentDescription = "Change album artwork", tint = subtle) }
                        }
                        HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                    }
                }
            } else if (tab == "GENRES") {
                val genres = songs.flatMap { song -> song.genres.ifEmpty { listOf(song.genre.ifBlank { "Unknown genre" }) }.map { genreName -> genreName to song } }.groupBy({ it.first }, { it.second }).toList().sortedBy { it.first.lowercase() }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    item { Text("BROWSE GENRES", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp)) }
                    itemsIndexed(genres, key = { _, item -> item.first }) { _, item ->
                        Row(Modifier.fillMaxWidth().clickable { val indices = item.second.mapNotNull { track -> songs.indexOfFirst { it.id == track.id }.takeIf { it >= 0 } }; if (indices.isNotEmpty()) onPlayCollection(indices, false) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(60.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                val art = artworkUris["genre_${item.first}"]
                                if (art != null) AsyncImage(model = art, contentDescription = "${item.first} artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(26.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) { Text(item.first, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(item.second.size.toString() + " tracks", color = subtle, fontSize = 12.sp) }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play genre", tint = Accent)
                            IconButton(onClick = { artworkTargetKey = "genre_${item.first}"; artworkTargetLabel = item.first; artworkPickerLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Default.Image, contentDescription = "Change genre artwork", tint = subtle) }
                        }
                        HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                    }
                }
            } else if (tab == "FOLDERS") {
                val folders = songs.groupBy { song ->
                    song.filePath.substringBeforeLast('/', "").substringAfterLast('/').ifBlank { "Unknown folder" }
                }.toList().sortedBy { it.first.lowercase() }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    item { Text("BROWSE FOLDERS", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(vertical = 10.dp)) }
                    itemsIndexed(folders, key = { _, folder -> folder.first }) { _, folder ->
                        Row(Modifier.fillMaxWidth().clickable {
                            val indices = folder.second.mapNotNull { song -> songs.indexOfFirst { it.id == song.id }.takeIf { it >= 0 } }
                            if (indices.isNotEmpty()) onPlayCollection(indices, false)
                        }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(52.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Folder, contentDescription = null, tint = Accent, modifier = Modifier.size(26.dp)) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(folder.first, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${folder.second.size} tracks", color = subtle, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play folder", tint = Accent)
                        }
                        HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                    }
                }
            } else if (tab == "PLAYLISTS" && activePlaylist == null) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("YOUR PLAYLISTS", color = fg, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { importPlaylistLauncher.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/octet-stream", "text/plain")) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("IMPORT M3U", color = fg, fontSize = 10.sp) }
                        Button(onClick = { newPlaylistName = ""; showCreatePlaylist = true }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("CREATE") }
                    }
                    if (playlists.isEmpty()) EmptyState("NO PLAYLISTS YET", "Create a playlist and add your favorite tracks.", "CREATE PLAYLIST", { showCreatePlaylist = true }) else LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                        itemsIndexed(playlists.keys.sorted(), key = { _, name -> name }) { _, name ->
                            Row(Modifier.fillMaxWidth().clickable { activePlaylist = name }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                                                                Surface(Modifier.size(60.dp), color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(12.dp)) {
                                    val art = artworkUris["playlist_$name"]
                                    if (art != null) AsyncImage(model = art, contentDescription = "$name artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)))
                                    else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.QueueMusic, null, tint = Accent, modifier = Modifier.size(28.dp)) }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = fg, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    Text("${playlists[name].orEmpty().size} tracks", color = subtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { artworkTargetKey = "playlist_$name"; artworkTargetLabel = name; artworkPickerLauncher.launch(arrayOf("image/*")) }) { Icon(Icons.Default.Image, contentDescription = "Change playlist artwork", tint = subtle) }
                                TextButton(onClick = { pendingExportPlaylistName = name; exportPlaylistLauncher.launch("${name.replace(Regex("[^A-Za-z0-9 _-]"), "").ifBlank { "Music playlist" }}.m3u") }) { Text("Export", color = Accent) }
                                TextButton(onClick = { playlists = playlists - name; if (activePlaylist == name) activePlaylist = null }) { Text("Delete", color = Accent) }
                            }
                            HorizontalDivider(color = if (theme == "LIGHT") Color(0xFFE1DCCF) else Color(0xFF292929))
                        }
                    }
                }
            } else if (tab == "PLAYLISTS" && activePlaylist != null) {
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { activePlaylist = null }) { Text("‹ PLAYLISTS", color = Accent) }
                        Text(activePlaylist ?: "", modifier = Modifier.weight(1f), color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(
                            onClick = {
                                val ids = playlists[activePlaylist].orEmpty()
                                val indices = ids.mapNotNull { id -> songs.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
                                if (indices.isNotEmpty()) onPlayCollection(indices, false)
                            },
                            enabled = playlists[activePlaylist].orEmpty().isNotEmpty()
                        ) { Icon(Icons.Default.PlayArrow, contentDescription = "Play playlist", tint = Accent) }
                        IconButton(
                            onClick = {
                                val ids = playlists[activePlaylist].orEmpty()
                                val indices = ids.mapNotNull { id -> songs.indexOfFirst { it.id == id }.takeIf { it >= 0 } }
                                if (indices.isNotEmpty()) onPlayCollection(indices, true)
                            },
                            enabled = playlists[activePlaylist].orEmpty().isNotEmpty()
                        ) { Icon(Icons.Default.Shuffle, contentDescription = "Shuffle playlist", tint = Accent) }
                        IconButton(onClick = {
                            val name = activePlaylist ?: "Music playlist"
                            val trackLines = playlists[name].orEmpty().mapNotNull { id -> songs.find { it.id == id } }
                                .mapIndexed { index, song -> "${index + 1}. ${song.title} — ${song.artist}" }
                            val message = (listOf("$name • Music Pro", "${trackLines.size} tracks", "") + trackLines).joinToString("\n")
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, name)
                                putExtra(Intent.EXTRA_TEXT, message)
                            }
                            runCatching { context.startActivity(Intent.createChooser(shareIntent, "Share playlist")) }
                        }) { Icon(Icons.Default.Share, contentDescription = "Share playlist", tint = Accent) }
                    }
                    if (filtered.isEmpty()) {
                        EmptyState(
                            if (query.isNotBlank()) "NO MATCHING TRACKS" else "PLAYLIST IS EMPTY",
                            if (query.isNotBlank()) "No song matched \"$query\" in this view. Try another title, artist, or album." else "Add tracks from your library using the track menu.",
                            if (query.isNotBlank()) "CLEAR SEARCH" else "BROWSE TRACKS",
                            { if (query.isNotBlank()) query = "" else tab = "TRACKS" }
                        )
                    } else LazyColumn(contentPadding = PaddingValues(bottom = 12.dp)) {
                        itemsIndexed(sortedFiltered, key = { _, song -> song.id }) { _, song ->
                            Row(Modifier.fillMaxWidth().clickable { tab = "NOW PLAYING"; onPlay(songs.indexOfFirst { it.id == song.id }) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(song.title, color = fg, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    Text(song.artist, color = subtle, fontSize = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                                TextButton(onClick = { activePlaylist?.let { name -> playlists = playlists + (name to playlists[name].orEmpty().filterNot { it == song.id }) } }) { Text("Remove", color = Accent) }
                            }
                        }
                    }
                }
            } else if (filtered.isEmpty()) {
                if (query.isNotBlank() && songs.isNotEmpty()) {
                    EmptyState(
                        "NO MATCHING TRACKS",
                        "No song matched \"$query\". Try a different title, artist, or album, or clear your search.",
                        "CLEAR SEARCH",
                        { query = ""; durationFilter = "ALL"; showFavorites = false; activePlaylist = null }
                    )
                } else if (tab == "RECENT") {
                    EmptyState("NO RECENT TRACKS YET", "Play a song and it will appear here.", "BROWSE TRACKS", { tab = "TRACKS"; query = "" })
                } else {
                    EmptyState("NOTHING IN THE LIBRARY", "Add audio files to your phone, then tap rescan.", "RESCAN LIBRARY", onRefresh)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    // Keep the collection heading on its own row so action buttons never squeeze it into vertical text.
                    Text(
                        when { showFavorites || tab == "FAVORITES" -> "YOUR FAVORITES"; tab == "TRACKS" -> "LOCAL SONGS"; tab == "ALBUMS" -> "ALBUM TRACKS"; tab == "PLAYLISTS" -> "PLAYLIST TRACKS"; else -> "YOUR COLLECTION" },
                        modifier = Modifier.fillMaxWidth(),
                        color = fg,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${sortedFiltered.size} songs on this device • ${songs.map { it.artist.trim().ifBlank { "Unknown artist" } }.distinct().size} artists",
                        color = subtle,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (showFavorites || tab == "FAVORITES") "YOUR FAVORITES" else if (tab == "RECENT") "RECENTLY PLAYED" else "LOCAL TRACKS", color = fg, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text("${sortedFiltered.size} songs visible · ${songs.size} in your library", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { selectedTrackIds = if (selectedTrackIds.size == sortedFiltered.size) emptySet() else sortedFiltered.map { it.id }.toSet() }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) { Text(if (selectedTrackIds.size == sortedFiltered.size && sortedFiltered.isNotEmpty()) "DESELECT" else "SELECT", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        if (selectedTrackIds.isNotEmpty()) {
                            Text(selectedTrackIds.size.toString(), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { val ids = sortedFiltered.map { it.id }.filter { it in selectedTrackIds }; val indices = ids.mapNotNull { id -> songs.indexOfFirst { it.id == id }.takeIf { it >= 0 } }; if (indices.isNotEmpty()) onPlayCollection(indices, false); selectedTrackIds = emptySet(); tab = "NOW PLAYING" }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) { Text("PLAY", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            TextButton(onClick = { val ids = sortedFiltered.map { it.id }.filter { it in selectedTrackIds }; var name = "Selection " + (System.currentTimeMillis() % 100000); while (name in playlists) name += " 2"; playlists = playlists + (name to ids); activePlaylist = name; selectedTrackIds = emptySet(); tab = "PLAYLISTS" }, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)) { Text("MAKE PLAYLIST", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        }
                        if (tab == "RECENT") {
                            TextButton(
                                onClick = {
                                    recentIds = emptyList()
                                    context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                                        .edit().putString("recent_ids", "").apply()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) { Text("CLEAR", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
                        }
                        Button(
                            onClick = { onPlayCollection(sortedFiltered.map { song -> songs.indexOfFirst { it.id == song.id } }.filter { it >= 0 }, false) },
                            enabled = sortedFiltered.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) { Icon(Icons.Default.PlayArrow, contentDescription = "Play all filtered tracks", modifier = Modifier.size(18.dp)); Spacer(Modifier.width(3.dp)); Text("PLAY", fontSize = 10.sp, maxLines = 1) }
                        IconButton(onClick = { onPlayCollection(sortedFiltered.map { song -> songs.indexOfFirst { it.id == song.id } }.filter { it >= 0 }, true) }, enabled = sortedFiltered.isNotEmpty()) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Shuffle filtered tracks", tint = Accent)
                        }
                        Spacer(Modifier.weight(1f))
                        Box {
                            OutlinedButton(onClick = { sortMenuExpanded = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) {
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = fg)
                                Spacer(Modifier.width(2.dp))
                                Text(when (sortMode) { "TITLE" -> "Title"; "ARTIST" -> "Artist"; "LONGEST" -> "Longest"; "SHORTEST" -> "Shortest"; "NEWEST" -> "Newest"; else -> "Sort" }, color = fg, maxLines = 1)
                            }
                            DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                                listOf("DEFAULT" to "Library order", "NEWEST" to "Recently added", "TITLE" to "Title A–Z", "ARTIST" to "Artist A–Z", "LONGEST" to "Longest first", "SHORTEST" to "Shortest first").forEach { (mode, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { sortMode = mode; sortMenuExpanded = false })
                                }
                            }
                        }
                    }
                }
                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                    itemsIndexed(sortedFiltered, key = { _, song -> song.id }) { _, song ->
                        val originalIndex = songs.indexOfFirst { it.id == song.id }
                        Row(Modifier.fillMaxWidth().clickable { if (selectedTrackIds.isNotEmpty()) selectedTrackIds = if (song.id in selectedTrackIds) selectedTrackIds - song.id else selectedTrackIds + song.id else { tab = "NOW PLAYING"; onPlay(originalIndex) } } .padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(Modifier.size(48.dp), color = if (song.id in selectedTrackIds) Accent.copy(alpha = 0.22f) else Panel, shape = RoundedCornerShape(10.dp)) {
                                Box(contentAlignment = Alignment.Center) { if (song.id in selectedTrackIds) Text("✓", color = Accent, fontWeight = FontWeight.Black) else Icon(Icons.Default.MusicNote, contentDescription = null, tint = if (originalIndex == selectedIndex) Accent else Muted) }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(song.title, color = if (originalIndex == selectedIndex) Accent else fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = subtle, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { favorites = if (song.id in favorites) favorites - song.id else favorites + song.id }) { Icon(if (song.id in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (song.id in favorites) Accent else subtle) }
                            Box {
                                IconButton(onClick = { addToPlaylistSong = song }) { Icon(Icons.Default.Add, contentDescription = "Add to playlist", tint = subtle) }
                                DropdownMenu(expanded = addToPlaylistSong?.id == song.id, onDismissRequest = { addToPlaylistSong = null }) {
                                    DropdownMenuItem(text = { Text("Play next") }, onClick = { onPlayNext(originalIndex); addToPlaylistSong = null })
                                    DropdownMenuItem(text = { Text("Track details") }, leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }, onClick = { detailsSong = song; addToPlaylistSong = null })
                                    DropdownMenuItem(text = { Text("Edit metadata") }, leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }, onClick = { metadataDraftTitle = song.title; metadataDraftArtist = song.artist; metadataDraftAlbum = song.album; editMetadataSong = song; addToPlaylistSong = null })
                                    DropdownMenuItem(text = { Text(if (artworkUris.containsKey("track_${song.id}")) "Change artwork" else "Set custom artwork") }, leadingIcon = { Icon(Icons.Default.Album, contentDescription = null) }, onClick = { artworkTargetKey = "track_${song.id}"; artworkTargetLabel = song.title; addToPlaylistSong = null; artworkPickerLauncher.launch(arrayOf("image/*")) })
                                    if (artworkUris.containsKey("track_${song.id}")) DropdownMenuItem(text = { Text("Remove custom artwork") }, onClick = { artworkUris = artworkUris - "track_${song.id}"; context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).edit().remove("artwork_track_${song.id}").apply(); addToPlaylistSong = null })
                                    DropdownMenuItem(text = { Text("Share audio") }, leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }, onClick = { onShare(song); addToPlaylistSong = null })
                                    DropdownMenuItem(text = { Text("Delete from device", color = Color(0xFFE84B35)) }, onClick = { deleteSong = song; addToPlaylistSong = null })
                                    if (playlists.isEmpty()) DropdownMenuItem(text = { Text("Create playlist first") }, onClick = { addToPlaylistSong = null; tab = "PLAYLISTS"; showCreatePlaylist = true })
                                    playlists.keys.sorted().forEach { name -> DropdownMenuItem(text = { Text(if (song.id in playlists[name].orEmpty()) "✓ $name" else name) }, onClick = { val ids = playlists[name].orEmpty(); playlists = playlists + (name to if (song.id in ids) ids else ids + song.id); addToPlaylistSong = null }) }
                                }
                            }
                            Text(formatDuration(song.durationMs), color = subtle, fontSize = 11.sp)
                        }
                        HorizontalDivider(color = Color(0xFF292929))
                    }
                }
            }
            if (tab != "NOW PLAYING") {
                Spacer(Modifier.height(4.dp))
                Surface(color = if (theme == "LIGHT") Color(0xFFF4F1E8) else Color(0xFF111111), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 2.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf("HOME", "TRACKS", "ALBUMS", "PLAYLISTS", "ARTISTS", "GENRES", "FOLDERS", "RECENT", "FAVORITES").forEach { label ->
                            val active = tab == label
                            Surface(color = if (active) Accent else Color.Transparent, shape = RoundedCornerShape(11.dp), modifier = Modifier.clickable {
                                tab = label
                                showFavorites = label == "FAVORITES"
                                if (label != "PLAYLISTS") activePlaylist = null
                                if (label == "TRACKS") query = ""
                            }) {
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(when (label) { "HOME" -> Icons.Default.Home; "TRACKS" -> Icons.Default.MusicNote; "ALBUMS" -> Icons.Default.Album; "PLAYLISTS" -> Icons.Default.QueueMusic; "ARTISTS" -> Icons.Default.MusicNote; "GENRES" -> Icons.Default.MusicNote; "FOLDERS" -> Icons.Default.Folder; "RECENT" -> Icons.Default.MusicNote; else -> Icons.Default.Favorite }, contentDescription = null, tint = if (active) Color.White else subtle, modifier = Modifier.size(17.dp))
                                    Text(if (label == "FAVORITES") "Saved" else label.lowercase().replaceFirstChar { it.uppercase() }, color = if (active) Color.White else subtle, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
            if (tab != "NOW PLAYING" && !miniPlayerDismissed && selectedIndex >= 0 && songs.getOrNull(selectedIndex) != null) {
                Surface(color = if (theme == "LIGHT") Color(0xFFE5E0D4) else Panel, shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp), modifier = Modifier.fillMaxWidth().clickable { tab = "NOW PLAYING" }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(Modifier.size(52.dp), color = Color(0xFF252525), shape = RoundedCornerShape(8.dp)) {
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
                        IconButton(onClick = { miniPlayerDismissed = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss mini player", tint = subtle, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
    if (showQueueDialog) AlertDialog(
        onDismissRequest = { showQueueDialog = false },
        title = { Text("Up next · ${queueEntries.size} tracks") },
        text = {
            if (queueEntries.isEmpty()) Text("Your playback queue is empty.")
            else LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                itemsIndexed(queueEntries, key = { _, item -> item.index }) { _, entry ->
                    Row(Modifier.fillMaxWidth().clickable {
                        onPlayQueueItem(entry.index)
                        queueEntries = onGetQueue()
                    }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (entry.isCurrent) "▶  ${entry.title}" else entry.title, color = if (entry.isCurrent) Accent else fg, fontWeight = if (entry.isCurrent) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(entry.artist, color = subtle, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row {
                                TextButton(enabled = entry.index > 0, onClick = { onMoveQueueItem(entry.index, entry.index - 1); queueEntries = onGetQueue() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text("↑", color = fg) }
                                TextButton(enabled = entry.index < queueEntries.lastIndex, onClick = { onMoveQueueItem(entry.index, entry.index + 1); queueEntries = onGetQueue() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text("↓", color = fg) }
                            }
                            TextButton(onClick = { onRemoveQueueItem(entry.index); queueEntries = onGetQueue() }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) { Text("REMOVE", color = Accent, fontSize = 9.sp) }
                        }
                    }
                    HorizontalDivider(color = Color(0xFF292929))
                }
            }
        },
        confirmButton = { TextButton(onClick = { showQueueDialog = false }) { Text("Done") } }
    )
    deleteSong?.let { song ->
        AlertDialog(
            onDismissRequest = { deleteSong = null },
            title = { Text("Delete this song?") },
            text = { Text("“${song.title}” will be removed from your device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(song); deleteSong = null }) {
                    Text("Delete song", color = Color(0xFFE84B35))
                }
            },
            dismissButton = { TextButton(onClick = { deleteSong = null }) { Text("Cancel") } }
        )
    }
    detailsSong?.let { song ->
        AlertDialog(
            onDismissRequest = { detailsSong = null },
            title = { Text(song.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ARTIST", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(song.artist.ifBlank { "Unknown artist" })
                    Text("ALBUM", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(song.album.ifBlank { "Unknown album" })
                    Text("DURATION", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(formatDuration(song.durationMs))
                }
            },
            confirmButton = {
                TextButton(onClick = { metadataDraftTitle = song.title; metadataDraftArtist = song.artist; metadataDraftAlbum = song.album; editMetadataSong = song; detailsSong = null }) { Text("Edit metadata") }
            },
            dismissButton = { TextButton(onClick = { detailsSong = null }) { Text("Cancel") } }
        )
    }
    editMetadataSong?.let { song ->
        AlertDialog(
            onDismissRequest = { editMetadataSong = null },
            title = { Text("Edit track metadata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Changes are saved privately on this device. The original audio file is not modified.", color = subtle, fontSize = 12.sp)
                    OutlinedTextField(value = metadataDraftTitle, onValueChange = { metadataDraftTitle = it.take(180) }, label = { Text("Track title") }, singleLine = true)
                    OutlinedTextField(value = metadataDraftArtist, onValueChange = { metadataDraftArtist = it.take(180) }, label = { Text("Artist") }, singleLine = true)
                    OutlinedTextField(value = metadataDraftAlbum, onValueChange = { metadataDraftAlbum = it.take(180) }, label = { Text("Album") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).edit()
                        .putString("metadata_${song.id}_title", metadataDraftTitle.trim().ifBlank { "Unknown track" })
                        .putString("metadata_${song.id}_artist", metadataDraftArtist.trim().ifBlank { "Unknown artist" })
                        .putString("metadata_${song.id}_album", metadataDraftAlbum.trim().ifBlank { "Unknown album" })
                        .apply()
                    editMetadataSong = null
                    onRefresh()
                    Toast.makeText(context, "Track details saved on this device", Toast.LENGTH_SHORT).show()
                }) { Text("Save changes") }
            },
            dismissButton = { TextButton(onClick = { editMetadataSong = null }) { Text("Cancel") } }
        )
    }
    lyricsDialogSong?.let { song ->
        AlertDialog(
            onDismissRequest = { lyricsDialogSong = null },
            title = { Text("Lyrics · ${song.title}", maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste your own lyrics or LRC-timestamped lyrics. Saved only on this device.", color = subtle, fontSize = 12.sp)
                    OutlinedButton(
                        enabled = !lyricsFetchLoading,
                        onClick = {
                            coroutineScope.launch {
                                lyricsFetchLoading = true
                                lyricsFetchMessage = null
                                val result = runCatching {
                                    withContext(Dispatchers.IO) {
                                        fun enc(value: String) = URLEncoder.encode(value, "UTF-8")
                                        val query = "track_name=${enc(song.title)}&artist_name=${enc(song.artist)}" +
                                            (song.album.takeIf { it.isNotBlank() }?.let { "&album_name=${enc(it)}" } ?: "") +
                                            "&duration=${(song.durationMs / 1000L).coerceAtLeast(0L)}"
                                        val connection = (URL("https://lrclib.net/api/get?$query").openConnection() as HttpURLConnection).apply {
                                            requestMethod = "GET"
                                            connectTimeout = 8000
                                            readTimeout = 10000
                                            setRequestProperty("User-Agent", "MusicPro-Android/1.0")
                                            setRequestProperty("Accept", "application/json")
                                        }
                                        try {
                                            val code = connection.responseCode
                                            if (code !in 200..299) error(if (code == 404) "No lyrics found for this track" else "Lyrics service returned HTTP $code")
                                            val payload = connection.inputStream.bufferedReader().use { it.readText() }
                                            val json = JSONObject(payload)
                                            val synced = json.optString("syncedLyrics").trim()
                                            val plain = json.optString("plainLyrics").trim()
                                            (synced.ifBlank { plain }).takeIf { it.isNotBlank() }
                                                ?: error("No lyrics were provided for this track")
                                        } finally {
                                            connection.disconnect()
                                        }
                                    }
                                }
                                result.onSuccess {
                                    lyricsDraft = it.take(20000)
                                    lyricsFetchMessage = "Lyrics found. Review them, then tap Save lyrics."
                                }.onFailure {
                                    lyricsFetchMessage = it.message ?: "Could not fetch lyrics"
                                }
                                lyricsFetchLoading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (lyricsFetchLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("SEARCHING ONLINE…")
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("FIND LYRICS ONLINE")
                        }
                    }
                    lyricsFetchMessage?.let { message ->
                        Text(message, color = if (message.startsWith("Lyrics found")) Accent else subtle, fontSize = 12.sp)
                    }
                    OutlinedButton(onClick = { lyricsImportLauncher.launch(arrayOf("text/plain", "application/octet-stream", "application/x-subrip")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("IMPORT .LRC / .TXT FILE")
                    }
                    OutlinedTextField(
                        value = lyricsDraft,
                        onValueChange = { lyricsDraft = it.take(20000) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 360.dp),
                        placeholder = { Text("Paste lyrics here…\n\n[00:12.00] First line") },
                        minLines = 6,
                        maxLines = 14
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                        .edit().putString("lyrics_${song.id}", lyricsDraft.trim()).apply()
                    lyricsDialogSong = null
                }) { Text("Save lyrics") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE)
                            .edit().remove("lyrics_${song.id}").apply()
                        lyricsDraft = ""
                    }) { Text("Clear") }
                    TextButton(onClick = { lyricsDialogSong = null }) { Text("Cancel") }
                }
            }
        )
    }
    if (showEqualizerDialog) AlertDialog(
        onDismissRequest = { showEqualizerDialog = false },
        title = { Text("5-band equalizer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Adjust the sound while music is playing. Some devices or audio routes may not expose a compatible equalizer session.", color = Muted, fontSize = 12.sp)
                listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz").forEachIndexed { band, label ->
                    Column {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, modifier = Modifier.weight(1f), color = Paper, fontSize = 12.sp)
                            Text((equalizerLevels[band] * 100f).toInt().toString() + " dB", color = Accent, fontSize = 12.sp)
                        }
                        Slider(
                            value = equalizerLevels[band],
                            onValueChange = { value ->
                                val updated = equalizerLevels.toMutableList().also { it[band] = value }
                                equalizerLevels = updated
                                context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).edit()
                                    .putString("equalizer_levels", updated.joinToString(",")).apply()
                                context.sendBroadcast(Intent(PlaybackService.ACTION_EQUALIZER).setPackage(context.packageName)
                                    .putExtra(PlaybackService.EXTRA_BAND, band)
                                    .putExtra(PlaybackService.EXTRA_LEVEL, (value * 100f).toInt()))
                            },
                            valueRange = -15f..15f
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showEqualizerDialog = false }) { Text("Done") } },
        dismissButton = { TextButton(onClick = { equalizerLevels = listOf(0f, 0f, 0f, 0f, 0f); context.getSharedPreferences("music_pro_preferences", Context.MODE_PRIVATE).edit().putString("equalizer_levels", "0,0,0,0,0").apply(); equalizerLevels.indices.forEach { band -> context.sendBroadcast(Intent(PlaybackService.ACTION_EQUALIZER).setPackage(context.packageName).putExtra(PlaybackService.EXTRA_BAND, band).putExtra(PlaybackService.EXTRA_LEVEL, 0)) } }) { Text("Reset") } }
    )
    if (showCreatePlaylist) AlertDialog(
        onDismissRequest = { showCreatePlaylist = false },
        title = { Text("Create playlist") },
        text = { OutlinedTextField(value = newPlaylistName, onValueChange = { newPlaylistName = it.take(40) }, label = { Text("Playlist name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { val name = newPlaylistName.trim(); if (name.isNotBlank() && name !in playlists) { val safeName = name.replace("::", "—").replace(",", " ").trim(); if (safeName.isNotBlank() && safeName !in playlists) { playlists = playlists + (safeName to emptyList()); activePlaylist = safeName } }; showCreatePlaylist = false; tab = "PLAYLISTS" }) { Text("Create") } },
        dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("Cancel") } }
    )
}

@Composable
private fun ColumnScope.NowPlaying(title: String, artist: String, album: String, playing: Boolean, foreground: Color, subtleColor: Color, accentColor: Color, artworkUri: String?, onBack: () -> Unit, onChooseArtwork: () -> Unit, onShareTrack: () -> Unit, onToggle: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit, onShuffle: () -> Unit, onRepeat: () -> Unit, shuffle: Boolean, repeat: Boolean, positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, playbackSpeed: Float, onSpeedChange: (Float) -> Unit, onOpenQueue: () -> Unit, isFavorite: Boolean, onToggleFavorite: () -> Unit, onOpenLyrics: () -> Unit, hasLyrics: Boolean, lyricsText: String) {
    var speedMenuExpanded by remember { mutableStateOf(false) }
    val recordMotion = rememberInfiniteTransition(label = "vinyl-record")
    val recordRotation = recordMotion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 24000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "record-rotation"
    )
    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back to library", tint = foreground, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f)) {
                Text("NOW PLAYING", color = foreground, fontSize = 18.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
                Text("Your music · full player", color = subtleColor, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier.fillMaxWidth().heightIn(max = 320.dp).aspectRatio(1f).padding(4.dp).pointerInput(durationMs) {
                if (durationMs > 0L) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val dx = offset.x - size.width / 2f
                            val dy = offset.y - size.height / 2f
                            val angle = (atan2(dy, dx) + PI / 2.0 + 2.0 * PI) % (2.0 * PI)
                            onSeek((angle / (2.0 * PI) * durationMs).toLong().coerceIn(0L, durationMs))
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val dx = change.position.x - size.width / 2f
                            val dy = change.position.y - size.height / 2f
                            val angle = (atan2(dy, dx) + PI / 2.0 + 2.0 * PI) % (2.0 * PI)
                            onSeek((angle / (2.0 * PI) * durationMs).toLong().coerceIn(0L, durationMs))
                        }
                    )
                }
            },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                Modifier.fillMaxSize().graphicsLayer { rotationZ = if (playing) recordRotation.value else 0f },
                color = Color(0xFF202020),
                shape = CircleShape
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Surface(Modifier.fillMaxSize().padding(12.dp), color = Color(0xFF101010), shape = CircleShape) {
                        Box(contentAlignment = Alignment.Center) {
                            Surface(Modifier.fillMaxSize().padding(16.dp), color = Color(0xFF191919), shape = CircleShape) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(Modifier.size(116.dp), color = accentColor, shape = CircleShape) {
                                        if (artworkUri != null) AsyncImage(model = artworkUri, contentDescription = "Custom track artwork", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
                                        else Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.MusicNote, null, tint = foreground, modifier = Modifier.size(42.dp)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = foreground, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                Text(artist, color = subtleColor, fontSize = 14.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                Text(album.uppercase(), color = accentColor, fontSize = 10.sp, letterSpacing = 2.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenLyrics, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) { Text(if (hasLyrics) "VIEW / EDIT LYRICS" else "ADD LYRICS", color = accentColor, fontSize = 12.sp, letterSpacing = 0.5.sp) }
                    TextButton(onClick = onChooseArtwork, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) { Text("ARTWORK", color = accentColor, fontSize = 12.sp) }
                    TextButton(onClick = onShareTrack, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SHARE", color = accentColor, fontSize = 12.sp)
                    }
                }
            }
            IconButton(onClick = onToggleFavorite) { Icon(if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites", tint = if (isFavorite) accentColor else subtleColor) }
        }
        val timedLyrics = remember(lyricsText) { parseTimedLyrics(lyricsText) }
        val activeLyricIndex = timedLyrics.indexOfLast { it.timeMs <= positionMs }
        if (timedLyrics.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().heightIn(max = 118.dp).padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                timedLyrics.getOrNull(activeLyricIndex)?.let { lyric -> Text(lyric.text, color = accentColor, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onSeek(lyric.timeMs) }.padding(vertical = 4.dp)) }
                timedLyrics.getOrNull(activeLyricIndex + 1)?.let { lyric -> Text(lyric.text, color = subtleColor.copy(alpha = 0.8f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth().clickable { onSeek(lyric.timeMs) }.padding(vertical = 3.dp)) }
            }
        }
        Spacer(Modifier.height(24.dp))
        Slider(
            value = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
            onValueChange = { fraction -> if (durationMs > 0L) { val target = (fraction * durationMs).toLong(); onSeek(target); } },
            enabled = durationMs > 0L,
            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor, inactiveTrackColor = subtleColor.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), color = subtleColor, fontSize = 11.sp)
            Text(formatDuration(durationMs), color = subtleColor, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { onSeek((positionMs - 10_000L).coerceAtLeast(0L)) }, enabled = durationMs > 0L, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                Text("↶ 10s", color = foreground, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(onClick = { onSeek((positionMs + 10_000L).coerceAtMost(durationMs)) }, enabled = durationMs > 0L, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)) {
                Text("10s ↷", color = foreground, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(18.dp))
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
        // Clearly labeled playback modes live in the full player, not on Home.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onShuffle,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (shuffle) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                    contentColor = if (shuffle) accentColor else foreground
                ),
                border = BorderStroke(1.dp, if (shuffle) accentColor else subtleColor.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (shuffle) "SHUFFLE ON" else "SHUFFLE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))
            OutlinedButton(
                onClick = onRepeat,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (repeat) accentColor.copy(alpha = 0.16f) else Color.Transparent,
                    contentColor = if (repeat) accentColor else foreground
                ),
                border = BorderStroke(1.dp, if (repeat) accentColor else subtleColor.copy(alpha = 0.45f)),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp)
            ) {
                Icon(Icons.Default.Repeat, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text(if (repeat) "REPEAT ON" else "REPEAT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onOpenQueue, shape = RoundedCornerShape(50), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("UP NEXT", color = foreground, fontWeight = FontWeight.Bold)
            }
            Box {
            OutlinedButton(
                onClick = { speedMenuExpanded = true },
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text("PLAYBACK SPEED  ·  ${playbackSpeed}×", color = foreground, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }) {
                listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                    DropdownMenuItem(
                        text = { Text(if (speed == 1f) "1.0× · Normal" else "${speed}×") },
                        onClick = { onSpeedChange(speed); speedMenuExpanded = false }
                    )
                }
            }
        }
        }
        Spacer(Modifier.height(20.dp))
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
