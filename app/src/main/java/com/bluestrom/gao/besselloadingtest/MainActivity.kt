package com.bluestrom.gao.besselloadingtest

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.Switch
import android.widget.TextView
import com.bluestrom.gao.customview.BesselLoadingView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        val ring = findViewById<BesselLoadingView>(R.id.ring)
        findViewById<Switch>(R.id.shape).setOnCheckedChangeListener { _, checked ->
            ring.shapeAngle = if (checked) 30f else 9.875864f
        }
        findViewById<Switch>(R.id.warm).setOnCheckedChangeListener { _, checked ->
            ring.startColor = if (checked) 0x00EA580C else 0x002563EB
            ring.endColor = if (checked) 0xFFEA580C.toInt() else 0xFF2563EB.toInt()
        }
        findViewById<Switch>(R.id.slow).setOnCheckedChangeListener { _, checked ->
            ring.durationMillis = if (checked) 2800L else 1400L
        }
        findViewById<View>(R.id.start).setOnClickListener { ring.start(); updateStatus() }
        findViewById<View>(R.id.stop).setOnClickListener { ring.stop(); updateStatus() }
        updateStatus()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        updateStatus()
    }

    private fun updateStatus() {
        val loading = findViewById<BesselLoadingView>(R.id.ring).isLoading
        findViewById<TextView>(R.id.status).setText(if (loading) R.string.loading else R.string.stopped)
    }
}
