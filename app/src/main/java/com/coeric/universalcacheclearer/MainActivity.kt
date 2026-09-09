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
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,22,24,18); setBackgroundColor(0xFFF8F7FC.toInt()) }
        val brand = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = ImageView(this).apply { setImageResource(R.drawable.ic_universal_u); setBackgroundColor(0xFF6750A4.toInt()); setPadding(14,14,14,14) }
        brand.addView(logo, LinearLayout.LayoutParams(58,58))
        val names = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14,0,0,0) }
        names.addView(TextView(this).apply { text="UNIVERSAL"; textSize=12f; setTextColor(0xFF6750A4.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) }, lp(WRAP,22))
        names.addView(TextView(this).apply { text="Cache Clearer"; textSize=24f; setTextColor(0xFF18151E.toInt()); setTypeface(typeface, android.graphics.Typeface.BOLD) }, lp(WRAP,34))
        brand.addView(names, LinearLayout.LayoutParams(0,WRAP,1f)); root.addView(brand,lp(WRAP,64))
        root.addView(TextView(this).apply { text="One tap to request a full device cache cleanup."; textSize=14f; setTextColor(0xFF68636F.toInt()) },lp(WRAP,42))
        speedometer=SpeedometerView(this); root.addView(speedometer,LinearLayout.LayoutParams(-1,210))
        cacheSize=TextView(this).apply { text="Checking…"; textSize=31f; gravity=Gravity.CENTER; setTextColor(0xFF6750A4.toInt()); setTypeface(typeface,android.graphics.Typeface.BOLD) }; root.addView(cacheSize,lp(WRAP,52))
        status=TextView(this).apply { text="Scanning app cache"; textSize=14f; gravity=Gravity.CENTER; setTextColor(0xFF68636F.toInt()) }; root.addView(status,lp(WRAP,34))
        progress=ProgressBar(this).apply { visibility=ProgressBar.GONE }; root.addView(progress,LinearLayout.LayoutParams(-1,32))
        val clear=Button(this).apply { text="⚡  CLEAR ALL APP CACHES"; isAllCaps=false; textSize=15f; setTextColor(0xFFFFFFFF.toInt()); background=rounded(0xFF6750A4.toInt(),18f); setOnClickListener{requestSystemCacheClear()} }; root.addView(clear,lp(WRAP,58))
        val manage=Button(this).apply { text="Open Storage Settings"; isAllCaps=false; textSize=14f; setTextColor(0xFF6750A4.toInt()); background=rounded(0xFFEAE5F3.toInt(),18f); setOnClickListener{openStorageSettings()} }; root.addView(manage,lp(WRAP,54))
        root.addView(TextView(this).apply { text="Android controls device-wide cache deletion. Universal Cache Clearer uses the official system request instead of pretending it can access other apps' private data."; textSize=11.5f; gravity=Gravity.CENTER; setTextColor(0xFF77727E.toInt()); setPadding(8,10,8,0) },lp(WRAP,70))
        setContentView(root)
    }

    private fun scanCache()=thread{ val size=ownTemporarySize(); runOnUiThread{cacheSize.text=formatBytes(size); status.text=if(size>0)"This app has temporary files ready to clean" else "This app cache is already clean"} }

    private fun requestSystemCacheClear(){
        progress.visibility=ProgressBar.VISIBLE; status.text="Requesting Android's device-wide cache cleanup…"; speedometer.setSpeed(0f,true)
        try {
            startActivity(Intent("android.intent.action.CLEAR_APP_CACHE"))
        } catch (_: Exception) {
            progress.visibility=ProgressBar.GONE
            status.text="This device does not expose the system cache-cleanup screen."
            openStorageSettings()
        }
    }

    override fun onResume(){ super.onResume(); if(::progress.isInitialized && progress.visibility==ProgressBar.VISIBLE){ progress.visibility=ProgressBar.GONE; speedometer.setSpeed(0f,false); scanCache(); status.text="Returned from Android storage manager" } }

    private fun openStorageSettings(){ try{startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))}catch(_:Exception){startActivity(Intent(Settings.ACTION_SETTINGS))} }
    private fun ownTemporarySize():Long=listOfNotNull(cacheDir,externalCacheDir,codeCacheDir).distinctBy{it.absolutePath}.sumOf{directorySize(it)}
    private fun directorySize(file:File):Long=if(file.isFile)file.length()else file.listFiles()?.sumOf{directorySize(it)}?:0L
    private fun formatBytes(bytes:Long):String=when{bytes<1024->"$bytes B";bytes<1024*1024->"%.1f KB".format(bytes/1024.0);bytes<1024*1024*1024->"%.1f MB".format(bytes/(1024.0*1024.0));else->"%.2f GB".format(bytes/(1024.0*1024.0*1024.0))}
    private fun rounded(color:Int,radius:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(color);cornerRadius=radius}
    private fun lp(width:Int,height:Int)=LinearLayout.LayoutParams(width,height).apply{bottomMargin=6}
    companion object{const val WRAP=ViewGroup.LayoutParams.MATCH_PARENT}
}

private class SpeedometerView(context:android.content.Context):View(context){
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG); private var active=false
    fun setSpeed(value:Float,running:Boolean){active=running;invalidate()}
    override fun onDraw(canvas:Canvas){val cx=width/2f;val cy=height*.82f;val radius=(width.coerceAtMost(height*2)*.36f).coerceAtLeast(70f);val rect=RectF(cx-radius,cy-radius,cx+radius,cy+radius);paint.style=Paint.Style.STROKE;paint.strokeWidth=19f;paint.strokeCap=Paint.Cap.ROUND;paint.color=0xFFE5E1EA.toInt();canvas.drawArc(rect,180f,180f,false,paint);paint.color=0xFF6750A4.toInt();canvas.drawArc(rect,180f,active*0f+4f,false,paint);paint.style=Paint.Style.FILL;paint.color=0xFF18151E.toInt();val angle=Math.toRadians(182.0);val len=radius-25f;canvas.drawLine(cx,cy,cx+cos(angle).toFloat()*len,cy+sin(angle).toFloat()*len,paint);canvas.drawCircle(cx,cy,12f,paint);paint.textAlign=Paint.Align.CENTER;paint.textSize=14f;paint.color=0xFF68636F.toInt();canvas.drawText(if(active)"REQUESTING SYSTEM CLEANUP" else "READY",cx,cy+35f,paint);paint.textSize=21f;paint.color=0xFF6750A4.toInt();canvas.drawText("DEVICE CACHE",cx,cy+63f,paint)}
}
