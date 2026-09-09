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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        scanCache()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 22, 24, 18)
            setBackgroundColor(0xFFF8F7FC.toInt())
        }
        val brand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(2, 0, 2, 8) }
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.ic_universal_u)
            setBackgroundColor(0xFF6750A4.toInt())
            setPadding(14, 14, 14, 14)
        }
        brand.addView(logo, LinearLayout.LayoutParams(58, 58))
        val brandText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 0, 0, 0) }
        brandText.addView(TextView(this).apply {
            text = "UNIVERSAL"; textSize = 12f; setTextColor(0xFF6750A4.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, lp(WRAP, 22))
        brandText.addView(TextView(this).apply {
            text = "Cache Clearer"; textSize = 24f; setTextColor(0xFF18151E.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD)
        }, lp(WRAP, 34))
        brand.addView(brandText, LinearLayout.LayoutParams(0, WRAP, 1f))
        root.addView(brand, lp(WRAP, 72))
        root.addView(TextView(this).apply {
            text = "Clean temporary files. Reclaim space. Stay in control."
            textSize = 14f; setTextColor(0xFF68636F.toInt()); setPadding(2, 0, 2, 4)
        }, lp(WRAP, 42))
        speedometer = SpeedometerView(this)
        root.addView(speedometer, LinearLayout.LayoutParams(-1, 220))
        cacheSize = TextView(this).apply {
            text = "Checking…"; textSize = 32f; gravity = Gravity.CENTER; setTextColor(0xFF6750A4.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(cacheSize, lp(WRAP, 52))
        status = TextView(this).apply {
            text = "Scanning your temporary cache"; textSize = 14f; gravity = Gravity.CENTER; setTextColor(0xFF68636F.toInt())
        }
        root.addView(status, lp(WRAP, 34))
        progress = ProgressBar(this).apply { visibility = ProgressBar.GONE }
        root.addView(progress, LinearLayout.LayoutParams(-1, 32))
        val clear = Button(this).apply {
            text = "⚡  DEEP CLEAN"; isAllCaps = false; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
            background = rounded(0xFF6750A4.toInt(), 18f); setOnClickListener { clearOwnCache() }
        }
        root.addView(clear, lp(WRAP, 58))
        val manage = Button(this).apply {
            text = "Open Android Storage Manager"; isAllCaps = false; textSize = 14f; setTextColor(0xFF6750A4.toInt())
            background = rounded(0xFFEAE5F3.toInt(), 18f); setOnClickListener { openStorageSettings() }
        }
        root.addView(manage, lp(WRAP, 54))
        root.addView(TextView(this).apply {
            text = "This app can safely erase its own cache and temporary files. Android does not allow ordinary apps to silently erase other apps' private caches; use the system storage manager for device-wide cleanup."
            textSize = 11.5f; gravity = Gravity.CENTER; setTextColor(0xFF77727E.toInt()); setPadding(10, 10, 10, 0)
        }, lp(WRAP, 72))
        setContentView(root)
    }

    private fun scanCache() = thread {
        val size = ownTemporarySize()
        runOnUiThread {
            cacheSize.text = formatBytes(size)
            status.text = if (size > 0) "Temporary files ready to clean" else "Your app cache is already clean"
            speedometer.setSpeed(0f, false)
        }
    }

    private fun clearOwnCache() {
        progress.visibility = ProgressBar.VISIBLE
        status.text = "Deep cleaning temporary files…"
        speedometer.setSpeed(0f, true)
        thread {
            val start = System.nanoTime()
            val before = ownTemporarySize()
            val roots = listOf(cacheDir, externalCacheDir, codeCacheDir).filterNotNull().distinctBy { it.absolutePath }
            roots.forEach { root ->
                root.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                    val elapsed = (System.nanoTime() - start) / 1_000_000_000.0
                    val removed = (before - ownTemporarySize()).coerceAtLeast(0L)
                    val speed = if (elapsed > 0) removed / elapsed else 0.0
                    runOnUiThread { speedometer.setSpeed(speed.toFloat(), true); cacheSize.text = formatBytes(removed) }
                }
            }
            val after = ownTemporarySize()
            val removed = (before - after).coerceAtLeast(0L)
            val elapsed = (System.nanoTime() - start) / 1_000_000_000.0
            val speed = if (elapsed > 0) removed / elapsed else 0.0
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                speedometer.setSpeed(speed.toFloat(), false)
                cacheSize.text = formatBytes(after)
                status.text = if (removed > 0) "Clean complete • ${formatBytes(removed)} reclaimed" else "Nothing to clean • cache already empty"
            }
        }
    }

    private fun ownTemporarySize(): Long = listOf(cacheDir, externalCacheDir, codeCacheDir).filterNotNull().distinctBy { it.absolutePath }.sumOf { directorySize(it) }
    private fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        try { startActivity(intent) } catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }
    private fun directorySize(file: File): Long = if (file.isFile) file.length() else file.listFiles()?.sumOf { directorySize(it) } ?: 0L
    private fun formatBytes(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
        else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    }
    private fun rounded(color: Int, radius: Float) = android.graphics.drawable.GradientDrawable().apply { setColor(color); cornerRadius = radius }
    private fun lp(width: Int, height: Int) = LinearLayout.LayoutParams(width, height).apply { bottomMargin = 6 }
    companion object { const val WRAP = ViewGroup.LayoutParams.MATCH_PARENT }
}

private class SpeedometerView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var speed = 0f
    private var active = false
    fun setSpeed(value: Float, running: Boolean) { speed = value.coerceAtLeast(0f); active = running; invalidate() }
    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f; val cy = height * 0.82f
        val radius = (width.coerceAtMost(height * 2) * 0.36f).coerceAtLeast(70f)
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 19f; paint.strokeCap = Paint.Cap.ROUND; paint.color = 0xFFE5E1EA.toInt()
        canvas.drawArc(rect, 180f, 180f, false, paint)
        paint.color = 0xFF6750A4.toInt()
        val fraction = (speed / (100f * 1024f * 1024f)).coerceIn(0f, 1f)
        canvas.drawArc(rect, 180f, 180f * fraction, false, paint)
        paint.style = Paint.Style.FILL; paint.color = 0xFF18151E.toInt()
        val angle = Math.toRadians((180f + 180f * fraction).toDouble()); val length = radius - 25f
        canvas.drawLine(cx, cy, cx + cos(angle).toFloat() * length, cy + sin(angle).toFloat() * length, paint); canvas.drawCircle(cx, cy, 12f, paint)
        paint.textAlign = Paint.Align.CENTER; paint.textSize = 14f; paint.color = 0xFF68636F.toInt()
        canvas.drawText(if (active) "CLEANING SPEED" else "CLEANING STATUS", cx, cy + 35f, paint)
        paint.textSize = 22f; paint.color = 0xFF6750A4.toInt(); canvas.drawText(formatSpeed(speed), cx, cy + 63f, paint)
    }
    private fun formatSpeed(bytes: Float): String = when {
        bytes < 1024f -> "%.0f B/s".format(bytes)
        bytes < 1024f * 1024f -> "%.1f KB/s".format(bytes / 1024f)
        else -> "%.1f MB/s".format(bytes / (1024f * 1024f))
    }
}
