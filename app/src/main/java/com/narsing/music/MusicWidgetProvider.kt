package com.narsing.music

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

class MusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        // Render immediately, then sync with the active playback session when available.
        ids.forEach { id -> render(context, manager, id, "Choose a track", "Your offline library", false, false, false) }
        syncWidgets(context, manager, ids)
    }

    private fun syncWidgets(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                val controller = future.get()
                val item = controller.currentMediaItem
                val title = item?.mediaMetadata?.title?.toString() ?: "Choose a track"
                val artist = item?.mediaMetadata?.artist?.toString() ?: "Your offline library"
                widgetIds.forEach { id -> render(context, manager, id, title, artist, controller.isPlaying, controller.shuffleModeEnabled, controller.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF) }
                MediaController.releaseFuture(future)
            }.onFailure {
                MediaController.releaseFuture(future)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action !in setOf(ACTION_PLAY, ACTION_PREVIOUS, ACTION_NEXT, ACTION_SHUFFLE, ACTION_REPEAT)) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            runCatching {
                val controller = future.get()
                when (action) {
                    ACTION_PLAY -> if (controller.isPlaying) controller.pause() else controller.play()
                    ACTION_PREVIOUS -> if (controller.hasPreviousMediaItem()) controller.seekToPreviousMediaItem() else controller.seekTo(0, 0L)
                    ACTION_NEXT -> if (controller.hasNextMediaItem()) controller.seekToNextMediaItem() else controller.seekTo(0, 0L)
                    ACTION_SHUFFLE -> controller.shuffleModeEnabled = !controller.shuffleModeEnabled
                    ACTION_REPEAT -> controller.repeatMode = if (controller.repeatMode == androidx.media3.common.Player.REPEAT_MODE_OFF) androidx.media3.common.Player.REPEAT_MODE_ALL else androidx.media3.common.Player.REPEAT_MODE_OFF
                }
                val item = controller.currentMediaItem
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
                ids.forEach { id ->
                    render(context, AppWidgetManager.getInstance(context), id,
                        item?.mediaMetadata?.title?.toString() ?: "Choose a track",
                        item?.mediaMetadata?.artist?.toString() ?: "Your offline library",
                        controller.isPlaying,
                        controller.shuffleModeEnabled,
                        controller.repeatMode != androidx.media3.common.Player.REPEAT_MODE_OFF)
                }
                MediaController.releaseFuture(future)
            }.onFailure { MediaController.releaseFuture(future) }
        }, ContextCompat.getMainExecutor(context))
    }

    companion object {
        const val ACTION_PLAY = "com.narsing.music.widget.PLAY"
        const val ACTION_PREVIOUS = "com.narsing.music.widget.PREVIOUS"
        const val ACTION_NEXT = "com.narsing.music.widget.NEXT"
        const val ACTION_SHUFFLE = "com.narsing.music.widget.SHUFFLE"
        const val ACTION_REPEAT = "com.narsing.music.widget.REPEAT"

        fun refresh(context: Context, title: String, artist: String, playing: Boolean, shuffle: Boolean, repeat: Boolean) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MusicWidgetProvider::class.java))
            ids.forEach { id -> render(context, manager, id, title, artist, playing, shuffle, repeat) }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int, title: String, artist: String, playing: Boolean, shuffle: Boolean, repeat: Boolean) {
            val views = RemoteViews(context.packageName, R.layout.music_widget)
            views.setTextViewText(R.id.widget_title, title)
            views.setTextViewText(R.id.widget_artist, artist)
            views.setTextViewText(R.id.widget_play, if (playing) "Ⅱ" else "▶")
            views.setTextViewText(R.id.widget_shuffle, "⤨")
            views.setTextViewText(R.id.widget_repeat, "↻")
            views.setTextColor(R.id.widget_shuffle, if (shuffle) 0xFFE84B35.toInt() else 0xFFF4F1E8.toInt())
            views.setTextColor(R.id.widget_repeat, if (repeat) 0xFFE84B35.toInt() else 0xFFF4F1E8.toInt())
            views.setOnClickPendingIntent(R.id.widget_previous, actionIntent(context, ACTION_PREVIOUS, id * 5 + 1))
            views.setOnClickPendingIntent(R.id.widget_play, actionIntent(context, ACTION_PLAY, id * 5 + 2))
            views.setOnClickPendingIntent(R.id.widget_next, actionIntent(context, ACTION_NEXT, id * 5 + 3))
            views.setOnClickPendingIntent(R.id.widget_shuffle, actionIntent(context, ACTION_SHUFFLE, id * 5 + 4))
            views.setOnClickPendingIntent(R.id.widget_repeat, actionIntent(context, ACTION_REPEAT, id * 5 + 5))
            views.setOnClickPendingIntent(R.id.widget_brand, PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id, views)
        }

        private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MusicWidgetProvider::class.java).setAction(action)
            return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}