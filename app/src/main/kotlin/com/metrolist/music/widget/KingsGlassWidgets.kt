/**
 * Kings Play - three glass widgets: Identify a Song, Mini Player, Kings Play pill.
 * Licensed under GPL-3.0
 */

package com.metrolist.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.playback.MusicService

/** Draws the Mini Player and Kings Play pill widgets from the player state. */
object KingsGlassWidgets {
    const val ACTION_PLAY_PAUSE = "com.kingsplay.music.widget.KG_PLAY_PAUSE"
    const val ACTION_NEXT = "com.kingsplay.music.widget.KG_NEXT"
    const val ACTION_PREVIOUS = "com.kingsplay.music.widget.KG_PREVIOUS"

    private var lastTitle: String? = null
    private var lastArtist: String? = null
    private var lastSource: Bitmap? = null
    private var lastArt: Bitmap? = null
    private var lastPlaying = false

    /** Called from MetrolistWidgetManager every time the song or play state changes. */
    fun update(context: Context, title: String, artist: String, albumArt: Bitmap?, isPlaying: Boolean) {
        lastTitle = title
        lastArtist = artist
        lastPlaying = isPlaying
        if (albumArt !== lastSource) {
            lastSource = albumArt
            lastArt = albumArt?.let { roundedSquare(it) }
        }
        render(context)
    }

    fun render(context: Context) {
        try {
            val manager = AppWidgetManager.getInstance(context)
            val title = lastTitle?.takeIf { it.isNotBlank() } ?: "Play your music"
            val artist = lastArtist?.takeIf { it.isNotBlank() } ?: "Kings Play"
            val lightIcon = if (lastPlaying) R.drawable.kg_ic_pause_white else R.drawable.kg_ic_play_white
            val darkIcon = if (lastPlaying) R.drawable.kg_ic_pause_dark else R.drawable.kg_ic_play_dark

            val miniIds = manager.getAppWidgetIds(ComponentName(context, KgMiniWidgetReceiver::class.java))
            if (miniIds.isNotEmpty()) {
                val v = RemoteViews(context.packageName, R.layout.widget_kg_mini)
                v.setTextViewText(R.id.kg_mini_title, title)
                v.setTextViewText(R.id.kg_mini_artist, artist)
                setArt(v, R.id.kg_mini_art)
                v.setImageViewResource(R.id.kg_mini_play, lightIcon)
                v.setOnClickPendingIntent(R.id.kg_mini_art, openApp(context))
                v.setOnClickPendingIntent(R.id.kg_mini_play, control(context, KgMiniWidgetReceiver::class.java, ACTION_PLAY_PAUSE))
                v.setOnClickPendingIntent(R.id.kg_mini_prev, control(context, KgMiniWidgetReceiver::class.java, ACTION_PREVIOUS))
                v.setOnClickPendingIntent(R.id.kg_mini_next, control(context, KgMiniWidgetReceiver::class.java, ACTION_NEXT))
                manager.updateAppWidget(miniIds, v)
            }

            val pillIds = manager.getAppWidgetIds(ComponentName(context, KgPillWidgetReceiver::class.java))
            if (pillIds.isNotEmpty()) {
                val v = RemoteViews(context.packageName, R.layout.widget_kg_pill)
                v.setTextViewText(R.id.kg_pill_title, title)
                v.setTextViewText(R.id.kg_pill_artist, artist)
                setArt(v, R.id.kg_pill_art)
                v.setImageViewResource(R.id.kg_pill_play, darkIcon)
                v.setOnClickPendingIntent(R.id.kg_pill_art, openApp(context))
                v.setOnClickPendingIntent(R.id.kg_pill_play_box, control(context, KgPillWidgetReceiver::class.java, ACTION_PLAY_PAUSE))
                manager.updateAppWidget(pillIds, v)
            }
        } catch (e: Exception) {
            // widgets must never crash the app
        }
    }

    private fun setArt(views: RemoteViews, viewId: Int) {
        val art = lastArt
        if (art != null) {
            views.setImageViewBitmap(viewId, art)
        } else {
            views.setImageViewResource(viewId, R.drawable.kg_cover_default)
        }
    }

    private fun roundedSquare(src: Bitmap): Bitmap {
        val size = minOf(src.width, src.height)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(0f, 0f, size.toFloat(), size.toFloat())
        val radius = size * 0.22f
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val left = (src.width - size) / 2
        val top = (src.height - size) / 2
        canvas.drawBitmap(src, Rect(left, top, left + size, top + size), rect, paint)
        return out
    }

    private fun openApp(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context, 7310, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun control(context: Context, receiver: Class<*>, action: String): PendingIntent {
        val intent = Intent(context, receiver).apply { this.action = action }
        return PendingIntent.getBroadcast(
            context, (receiver.name + action).hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Opens the song recognition screen of the app. */
    fun recognitionIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_RECOGNITION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context, 7311, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/** Shared behaviour of the two player widgets: draw, and pass button taps to the music service. */
abstract class KgPlayerWidgetBase : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        KingsGlassWidgets.render(context)
        askServiceToRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        KingsGlassWidgets.render(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val serviceAction = when (intent.action) {
            KingsGlassWidgets.ACTION_PLAY_PAUSE -> MusicWidgetReceiver.ACTION_PLAY_PAUSE
            KingsGlassWidgets.ACTION_NEXT -> MusicWidgetReceiver.ACTION_NEXT
            KingsGlassWidgets.ACTION_PREVIOUS -> MusicWidgetReceiver.ACTION_PREVIOUS
            else -> null
        } ?: return
        val serviceIntent = Intent(context, MusicService::class.java).apply { action = serviceAction }
        try {
            context.startService(serviceIntent)
        } catch (e: Exception) {
            // service might be restricted in the background
        }
    }

    private fun askServiceToRefresh(context: Context) {
        if (!MusicService.isRunning) return
        val intent = Intent(context, MusicService::class.java).apply {
            action = "com.metrolist.music.widget.UPDATE_WIDGET"
        }
        try {
            context.startService(intent)
        } catch (e: Exception) {
            // ignore
        }
    }
}

class KgMiniWidgetReceiver : KgPlayerWidgetBase()

class KgPillWidgetReceiver : KgPlayerWidgetBase()

/** Tap to Identify a Song: opens the recognition screen of the app. */
class KgIdentifyWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_kg_identify)
        views.setOnClickPendingIntent(R.id.kg_id_root, KingsGlassWidgets.recognitionIntent(context))
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}