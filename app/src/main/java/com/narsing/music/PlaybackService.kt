package com.narsing.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.Equalizer
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
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
                    equalizer = Equalizer(0, sessionId).apply { enabled = true }
                    equalizerSessionId = sessionId
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

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(this, equalizerReceiver, IntentFilter(ACTION_EQUALIZER), ContextCompat.RECEIVER_NOT_EXPORTED)
        player = ExoPlayer.Builder(this).build().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true)
            setHandleAudioBecomingNoisy(true)
        }
        mediaSession = MediaSession.Builder(this, player!!).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

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

    companion object {
        const val ACTION_EQUALIZER = "com.narsing.music.action.EQUALIZER"
        const val EXTRA_BAND = "band"
        const val EXTRA_LEVEL = "level"
    }
}
