package com.narsing.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.Equalizer
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class PlaybackService : MediaLibraryService() {
    private var mediaSession: MediaLibrarySession? = null
    private var player: ExoPlayer? = null
    private var equalizer: Equalizer? = null
    private var equalizerSessionId: Int = 0

    private val equalizerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val currentPlayer = player ?: return
            val sessionId = currentPlayer.audioSessionId
            if (sessionId <= 0) return
            runCatching {
                if (equalizer == null || equalizerSessionId != sessionId) {
                    equalizer?.release()
                    val effect = Equalizer(0, sessionId)
                    equalizer = effect
                    equalizerSessionId = sessionId

                    // Restore the user's saved curve when Android creates a new audio session.
                    // Equalizer instances are session-bound, so a previous instance's bands
                    // cannot be relied on after playback is recreated or the session changes.
                    val savedLevels = getSharedPreferences("music_pro_preferences", MODE_PRIVATE)
                        .getString("equalizer_levels", "0,0,0,0,0")
                        .orEmpty()
                        .split(",")
                        .map { it.toFloatOrNull() ?: 0f }
                    val range = effect.bandLevelRange
                    for (index in 0 until minOf(5, effect.numberOfBands.toInt())) {
                        val level = ((savedLevels.getOrNull(index) ?: 0f) * 100f).toInt()
                        effect.setBandLevel(
                            index.toShort(),
                            level.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
                        )
                    }
                    effect.enabled = true
                }
                val effect = equalizer ?: return
                val band = intent.getIntExtra(EXTRA_BAND, -1)
                if (band !in 0 until minOf(5, effect.numberOfBands.toInt())) return
                val range = effect.bandLevelRange
                val requested = intent.getIntExtra(EXTRA_LEVEL, 0)
                effect.setBandLevel(band.toShort(), requested.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
        }
    }

    private val libraryCallback = object : MediaLibrarySession.Callback {
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val root = MediaItem.Builder()
                .setMediaId(ROOT_ID)
                .setMediaMetadata(MediaMetadata.Builder()
                    .setTitle("Music Pro")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build())
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(root, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val songs = runCatching { MusicRepository(this@PlaybackService).loadSongs() }.getOrDefault(emptyList())
            val children: List<MediaItem> = when {
                parentId == ROOT_ID -> listOf(
                    folder("tracks", "All tracks", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
                    folder("albums", "Albums", MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
                    folder("artists", "Artists", MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
                    folder("genres", "Genres", MediaMetadata.MEDIA_TYPE_FOLDER_GENRES)
                )
                parentId == "tracks" -> songs.map { it.toLibraryMediaItem() }
                parentId == "albums" -> songs.groupBy { it.album }.keys.sortedBy { it.lowercase() }
                    .map { folder("album:$it", it, MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS) }
                parentId == "artists" -> songs.flatMap { song -> song.artist.split(Regex("\\s*(?:;|,|\\s&\\s|\\bfeat\\.?\\s+|\\bft\\.?\\s+|\\bfeaturing\\s+)\\s*", RegexOption.IGNORE_CASE)).map { it.trim() to song } }
                    .filter { it.first.isNotBlank() }.groupBy({ it.first }, { it.second }).keys.sortedBy { it.lowercase() }
                    .map { folder("artist:$it", it, MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS) }
                parentId == "genres" -> songs.flatMap { song -> song.genres.map { it to song } }
                    .groupBy({ it.first }, { it.second }).keys.sortedBy { it.lowercase() }
                    .map { folder("genre:$it", it, MediaMetadata.MEDIA_TYPE_FOLDER_GENRES) }
                parentId.startsWith("album:") -> songs.filter { it.album == parentId.removePrefix("album:") }.map { it.toLibraryMediaItem() }
                parentId.startsWith("artist:") -> songs.filter { song -> song.artist.split(Regex("\\s*(?:;|,|\\s&\\s|\\bfeat\\.?\\s+|\\bft\\.?\\s+|\\bfeaturing\\s+)\\s*", RegexOption.IGNORE_CASE)).any { it.trim().equals(parentId.removePrefix("artist:"), true) } }.map { it.toLibraryMediaItem() }
                parentId.startsWith("genre:") -> songs.filter { parentId.removePrefix("genre:") in it.genres }.map { it.toLibraryMediaItem() }
                else -> emptyList()
            }
            val safePageSize = pageSize.coerceAtLeast(1)
            val from = (page.toLong() * safePageSize).coerceAtMost(children.size.toLong()).toInt()
            val pageItems = children.drop(from).take(safePageSize)
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(pageItems), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val song = runCatching { MusicRepository(this@PlaybackService).loadSongs().firstOrNull { it.uri.toString() == mediaId || it.id.toString() == mediaId } }.getOrNull()
            return if (song != null) Futures.immediateFuture(LibraryResult.ofItem(song.toLibraryMediaItem(), null))
            else Futures.immediateFuture(LibraryResult.ofError(androidx.media3.session.SessionError.ERROR_BAD_VALUE))
        }
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, equalizerReceiver, IntentFilter(ACTION_EQUALIZER), ContextCompat.RECEIVER_NOT_EXPORTED)
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
            addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    refreshWidgetState()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    refreshWidgetState()
                }

                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    refreshWidgetState()
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    refreshWidgetState()
                }
            })
        }
        mediaSession = MediaLibrarySession.Builder(this, player!!, libraryCallback).build()
        refreshWidgetState()
    }

    private fun refreshWidgetState() {
        val currentPlayer = player ?: return
        val item = currentPlayer.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString() ?: "Choose a track"
        val artist = item?.mediaMetadata?.artist?.toString() ?: "Your offline library"
        val shuffle = currentPlayer.shuffleModeEnabled
        val repeat = currentPlayer.repeatMode != Player.REPEAT_MODE_OFF
        MusicWidgetProvider.refresh(this, title, artist, currentPlayer.isPlaying, shuffle, repeat)
        MusicCompactWidgetProvider.refresh(this, title, artist, currentPlayer.isPlaying, shuffle, repeat)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = mediaSession

    override fun onDestroy() {
        runCatching { unregisterReceiver(equalizerReceiver) }
        equalizer?.release()
        equalizer = null
        equalizerSessionId = 0
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun folder(id: String, title: String, mediaType: Int): MediaItem =
        MediaItem.Builder().setMediaId(id).setMediaMetadata(MediaMetadata.Builder()
            .setTitle(title).setIsBrowsable(true).setIsPlayable(false).setMediaType(mediaType).build()).build()

    private fun Song.toLibraryMediaItem(): MediaItem = MediaItem.Builder()
        .setMediaId(uri.toString())
        .setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album)
            .setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build())
        .build()

    companion object {
        const val ACTION_EQUALIZER = "com.narsing.music.action.EQUALIZER"
        const val EXTRA_BAND = "band"
        const val EXTRA_LEVEL = "level"
        private const val ROOT_ID = "music-pro-root"
    }
}
