package com.bluestrom.gao.customview

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class BesselLoadingViewTest {
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun view() = BesselLoadingView(InstrumentationRegistry.getInstrumentation().targetContext)
    private fun animator(view: BesselLoadingView): ValueAnimator? =
        BesselLoadingView::class.java.getDeclaredField("animator").apply { isAccessible = true }.get(view) as ValueAnimator?

    @Test fun animationFollowsStartStopVisibilityAndDetach() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(android.content.Intent(
            instrumentation.targetContext, RingTestActivity::class.java,
        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) as RingTestActivity
        try {
            instrumentation.waitForIdleSync()
            onMain {
                val ring = activity.ring
                val motionEnabled = if (android.os.Build.VERSION.SDK_INT >= 26) ValueAnimator.areAnimatorsEnabled()
                    else android.provider.Settings.Global.getFloat(activity.contentResolver, "animator_duration_scale", 1f) > 0f
                fun assertMotionState() {
                    if (motionEnabled) assertTrue(animator(ring)!!.isRunning) else assertNull(animator(ring))
                }
                assertTrue(ring.isLoading)
                assertMotionState()
                val first = animator(ring)
                ring.start()
                assertSame(first, animator(ring))
                ring.stop()
                assertFalse(ring.isLoading)
                assertNull(animator(ring))
                ring.start()
                assertMotionState()
                ring.visibility = View.GONE
                assertTrue(ring.isLoading)
                assertNull(animator(ring))
                ring.visibility = View.VISIBLE
                assertMotionState()
                (ring.parent as android.view.ViewGroup).removeView(ring)
                assertNull(animator(ring))
            }
        } finally { onMain { activity.finish() } }
    }

    @Test fun validatesParametersAndSupportsStartBeforeAttachment() = onMain {
        val ring = view()
        for (bad in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException::class.java) { ring.strokeWidth = bad }
        }
        for (bad in listOf(-1f, 46f, Float.NaN)) {
            assertThrows(IllegalArgumentException::class.java) { ring.shapeAngle = bad }
        }
        assertThrows(IllegalArgumentException::class.java) { ring.durationMillis = 0 }
        ring.stop(); ring.start()
        assertTrue(ring.isLoading)
        assertNull(animator(ring))
    }

    @Test fun wrapContentRelativePaddingAndTinyBoundsRender() = onMain {
        val ring = view()
        val spec = View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.AT_MOST)
        ring.measure(spec, spec)
        assertTrue(ring.measuredWidth in 1..1000)
        ring.startColor = android.graphics.Color.RED
        ring.endColor = android.graphics.Color.RED
        ring.layout(0, 0, 200, 140)
        val bitmap = Bitmap.createBitmap(200, 140, Bitmap.Config.ARGB_8888)
        ring.draw(Canvas(bitmap))
        ring.setPaddingRelative(20, 10, 40, 30)
        bitmap.eraseColor(0)
        ring.draw(Canvas(bitmap))
        var pixels = 0
        for (y in 0 until 140) for (x in 0 until 200) {
            if (bitmap.getPixel(x, y) ushr 24 > 0) {
                pixels++
                assertTrue("pixel outside padding: $x,$y", x >= 20 && x < 160 && y >= 10 && y < 110)
            }
        }
        assertTrue(pixels > 0)
        ring.layout(0, 0, 1, 1)
        ring.draw(Canvas(bitmap))
    }

    @Test fun restoresStyleAndLoadingStateAndHasNoPercentageRange() = onMain {
        val first = view().apply {
            id = 123
            stop()
            strokeWidth = 9f
            shapeAngle = 30f
            durationMillis = 900
            startColor = 0xFF00FF00.toInt()
        }
        val state = SparseArray<Parcelable>()
        first.saveHierarchyState(state)
        val restored = view().apply { id = 123; restoreHierarchyState(state) }
        assertEquals(first.strokeWidth, restored.strokeWidth)
        assertEquals(first.shapeAngle, restored.shapeAngle)
        assertEquals(first.durationMillis, restored.durationMillis)
        assertEquals(first.startColor, restored.startColor)
        assertFalse(restored.isLoading)
        restored.contentDescription = "Fetching data"
        val info = AccessibilityNodeInfo()
        restored.onInitializeAccessibilityNodeInfo(info)
        assertEquals("android.widget.ProgressBar", info.className)
        assertNull(info.rangeInfo)
        assertEquals("Fetching data", info.contentDescription)
        if (android.os.Build.VERSION.SDK_INT >= 30) assertEquals("Loading stopped", info.stateDescription)
    }
}

class RingTestActivity : android.app.Activity() {
    lateinit var ring: BesselLoadingView
    override fun onCreate(state: android.os.Bundle?) {
        super.onCreate(state)
        ring = BesselLoadingView(this)
        setContentView(ring)
    }
}
