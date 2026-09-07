package com.coeric.universalcacheclearer

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : AppCompatActivity() {
    private lateinit var cacheSize: TextView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var speedometer: SpeedometerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        clearOwnCacheOnOpen()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 20)
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }
        val title = TextView(this).apply {
            text = "Universal Cache Clearer"
            textSize = 27f
            setTextColor(0xFF17151C.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title, lp(WRAP, 54))

        val subtitle = TextView(this).apply {
            text = "Automatic cache cleaning"
            textSize = 16f
            setTextColor(0xFF625F66.toInt())
        }
        root.addView(subtitle, lp(WRAP, 40))

        speedometer = SpeedometerView(this)
        root.addView(speedometer, LinearLayout.LayoutParams(-1, 230))

        cacheSize = TextView(this).apply {
            text = "0 B"
            textSize = 34f
            gravity = Gravity.CENTER
            setTextColor(0xFF6750A4.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(cacheSize, LinearLayout.LayoutParams(-1, 62))

        status = TextView(this).apply {
            text = "Preparing cleanup…"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF625F66.toInt())
        }
        root.addView(status, lp(WRAP, 34))

        progress = ProgressBar(this).apply { visibility = ProgressBar.VISIBLE }
        root.addView(progress, LinearLayout.LayoutParams(-1, 34))

        val clear = Button(this).apply {
            text = "CLEAR AGAIN"
            isAllCaps = false
            setOnClickListener { clearOwnCacheOnOpen() }
        }
        root.addView(clear, lp(WRAP, 54))

        val manage = Button(this).apply {
            text = "MANAGE DEVICE STORAGE"
            isAllCaps = false
            setOnClickListener { openStorageSettings() }
        }
        root.addView(manage, lp(WRAP, 54))

        val info = TextView(this).apply {
            text = "The speedometer shows the live cache-cleaning rate. This app automatically clears its own temporary cache when opened. Android controls cleanup of other apps' private caches through its system storage tools."
            textSize = 12.5f
            setPadding(8, 12, 8, 0)
            setTextColor(0xFF706D73.toInt())
        }
        root.addView(info, lp(WRAP, 100))
        setContentView(root)
    }

    private fun clearOwnCacheOnOpen() {
        progress.visibility = ProgressBar.VISIBLE
        cacheSize.text = "0 B"
        status.text = "Clearing cache automatically"
        speedometer.setSpeed(0f, false)
        thread {
            val start = System.nanoTime()
            val before = directorySize(cacheDir)
            val files = cacheDir.listFiles() ?: emptyArray()
            var removed = 0L
            files.forEach { file ->
                val size = directorySize(file)
                file.deleteRecursively()
                removed += size
                val elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0
                val speed = if (elapsedSeconds > 0) removed / elapsedSeconds else 0.0
                runOnUiThread {
                    speedometer.setSpeed(speed.toFloat(), true)
                    cacheSize.text = formatBytes(removed)
                }
            }
            val after = directorySize(cacheDir)
            val totalRemoved = (before - after).coerceAtLeast(0L)
            val elapsedSeconds = (System.nanoTime() - start) / 1_000_000_000.0
            val finalSpeed = if (elapsedSeconds > 0) totalRemoved / elapsedSeconds else 0.0
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                speedometer.setSpeed(finalSpeed.toFloat(), false)
                cacheSize.text = formatBytes(totalRemoved)
                status.text = "Cache cleared automatically"
            }
        }
    }

    private fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        try { startActivity(intent) } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun directorySize(file: File): Long = if (file.isFile) file.length() else file.listFiles()?.sumOf { directorySize(it) } ?: 0L

    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }

    private fun lp(width: Int, height: Int) = LinearLayout.LayoutParams(width, height).apply { bottomMargin = 8 }
    companion object { const val WRAP = ViewGroup.LayoutParams.MATCH_PARENT }
}

private class SpeedometerView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var speedBytesPerSecond = 0f
    private var active = false

    fun setSpeed(speed: Float, running: Boolean) {
        speedBytesPerSecond = speed.coerceAtLeast(0f)
        active = running
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height * 0.82f
        val radius = (width.coerceAtMost(height * 2) * 0.36f).coerceAtLeast(70f)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 18f
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = 0xFFE8E5EC.toInt()
        canvas.drawArc(rect, 180f, 180f, false, paint)

        paint.color = 0xFF6750A4.toInt()
        val maxSpeed = 100f * 1024f * 1024f
        val fraction = (speedBytesPerSecond / maxSpeed).coerceIn(0f, 1f)
        canvas.drawArc(rect, 180f, 180f * fraction, false, paint)

        paint.style = Paint.Style.FILL
        paint.color = 0xFF17151C.toInt()
        val angle = Math.toRadians((180f + 180f * fraction).toDouble())
        val needleLength = radius - 25f
        val nx = cx + cos(angle).toFloat() * needleLength
        val ny = cy + sin(angle).toFloat() * needleLength
        paint.strokeWidth = 7f
        canvas.drawLine(cx, cy, nx, ny, paint)
        canvas.drawCircle(cx, cy, 12f, paint)

        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 16f
        paint.color = 0xFF625F66.toInt()
        canvas.drawText(if (active) "CLEANING SPEED" else "CLEANING COMPLETE", cx, cy + 35f, paint)

        paint.textSize = 24f
        paint.color = 0xFF6750A4.toInt()
        canvas.drawText(formatSpeed(speedBytesPerSecond), cx, cy + 65f, paint)
    }

    private fun formatSpeed(bytes: Float): String = when {
        bytes < 1024f -> "%.0f B/s".format(bytes)
        bytes < 1024f * 1024f -> "%.1f KB/s".format(bytes / 1024f)
        else -> "%.1f MB/s".format(bytes / (1024f * 1024f))
    }
}
