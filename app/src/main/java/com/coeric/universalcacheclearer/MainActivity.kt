package com.coeric.universalcacheclearer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
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
        refresh()
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
            text = "Clean safely. See what is taking space."
            textSize = 16f
            setTextColor(0xFF625F66.toInt())
        }
        root.addView(subtitle, lp(WRAP, 46))

        cacheSize = TextView(this).apply {
            text = "Calculating…"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(0xFF6750A4.toInt())
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        root.addView(cacheSize, LinearLayout.LayoutParams(-1, 120))

        status = TextView(this).apply {
            text = "App cache"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(0xFF625F66.toInt())
        }
        root.addView(status, lp(WRAP, 40))

        progress = ProgressBar(this).apply { visibility = ProgressBar.GONE }
        root.addView(progress, LinearLayout.LayoutParams(-1, 40))

        val clear = Button(this).apply {
            text = "CLEAR MY CACHE"
            isAllCaps = false
            setOnClickListener { clearOwnCache() }
        }
        root.addView(clear, lp(WRAP, 58))

        val manage = Button(this).apply {
            text = "MANAGE APP CACHES"
            isAllCaps = false
            setOnClickListener { openStorageSettings() }
        }
        root.addView(manage, lp(WRAP, 58))

        val info = TextView(this).apply {
            text = "Android protects other apps' private data. For system-wide cache cleanup, this app takes you to Android's storage manager rather than using unsafe or hidden APIs."
            textSize = 13f
            setPadding(8, 24, 8, 0)
            setTextColor(0xFF706D73.toInt())
        }
        root.addView(info, lp(WRAP, 120))
        setContentView(root)
    }

    private fun refresh() {
        progress.visibility = ProgressBar.VISIBLE
        thread {
            val bytes = directorySize(cacheDir)
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                cacheSize.text = formatBytes(bytes)
                status.text = "This app's cache"
            }
        }
    }

    private fun clearOwnCache() {
        progress.visibility = ProgressBar.VISIBLE
        thread {
            cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            runOnUiThread {
                progress.visibility = ProgressBar.GONE
                cacheSize.text = formatBytes(directorySize(cacheDir))
                status.text = "Cache cleared successfully"
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
