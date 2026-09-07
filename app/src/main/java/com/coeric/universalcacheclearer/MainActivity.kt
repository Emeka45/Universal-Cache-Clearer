package com.coeric.universalcacheclearer

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var cacheSize: TextView
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        clearOwnCacheOnOpen()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 24)
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }
        val title = TextView(this).apply {
            text = "Universal Cache Clearer"
            textSize = 27f
            setTextColor(0xFF17151C.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(title, lp(WRAP, 58))

        val subtitle = TextView(this).apply {
            text = "Automatic cache cleaning"
            textSize = 16f
            setTextColor(0xFF625F66.toInt())
        }
        root.addView(subtitle, lp(WRAP, 46))

        cacheSize = TextView(this).apply {
            text = "Cleaning…"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(0xFF6750A4.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(cacheSize, LinearLayout.LayoutParams(-1, 120))

        status = TextView(this).apply {
            text = "Clearing cache…"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF625F66.toInt())
        }
        root.addView(status, lp(WRAP, 40))

        progress = ProgressBar(this).apply { visibility = ProgressBar.VISIBLE }
        root.addView(progress, LinearLayout.LayoutParams(-1, 40))

        val clear = Button(this).apply {
            text = "CLEAR AGAIN"
            isAllCaps = false
            setOnClickListener { clearOwnCacheOnOpen() }
        }
        root.addView(clear, lp(WRAP, 58))

        val manage = Button(this).apply {
            text = "MANAGE DEVICE STORAGE"
            isAllCaps = false
            setOnClickListener { openStorageSettings() }
        }
        root.addView(manage, lp(WRAP, 58))

        val info = TextView(this).apply {
            text = "Universal Cache Clearer automatically clears its own temporary cache whenever it opens. Android does not allow ordinary apps to silently erase other apps' private caches, so device-wide cleanup is handled through Android's storage manager."
            textSize = 13f
            setPadding(8, 24, 8, 0)
            setTextColor(0xFF706D73.toInt())
        }
        root.addView(info, lp(WRAP, 140))
        setContentView(root)
    }

    private fun clearOwnCacheOnOpen() {
        progress.visibility = ProgressBar.VISIBLE
        cacheSize.text = "Cleaning…"
        status.text = "Clearing cache automatically"
        thread {
            val before = directorySize(cacheDir)
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            val after = directorySize(cacheDir)
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                cacheSize.text = formatBytes(before - after)
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

    private fun lp(width: Int, height: Int) = LinearLayout.LayoutParams(width, height).apply { bottomMargin = 10 }
    companion object { const val WRAP = ViewGroup.LayoutParams.MATCH_PARENT }
}
