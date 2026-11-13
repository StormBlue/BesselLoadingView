package com.bluestrom.gao.customview

import android.animation.ValueAnimator
import android.content.Context
import android.database.ContentObserver
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SweepGradient
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.provider.Settings
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import kotlin.math.min
import kotlin.math.tan

/** A rotating gradient on the original four-segment Bezier ring. Use on the main thread. */
class BesselLoadingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val ring = Path()
    private val matrix = Matrix()
    private var gradient: SweepGradient? = null
    private var geometryDirty = true
    private var centerX = 0f
    private var centerY = 0f
    private var drawable = false
    private var phase = 0f
    private var animator: ValueAnimator? = null
    private var aggregatedVisible = false
    private var ready = false

    /** Requested loading state. Animation also requires visibility and system motion permission. */
    var isLoading: Boolean = true
        private set(value) {
            if (field == value) return
            field = value
            syncAnimation()
            invalidate()
            if (ready && isAttachedToWindow) {
                sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
            }
        }

    /** Stroke width in pixels. XML accepts dimensions such as 4dp. */
    var strokeWidth: Float = 4f * resources.displayMetrics.density
        set(value) {
            require(value.isFinite() && value > 0f) { "strokeWidth must be finite and positive" }
            field = value
            geometryDirty = true
            invalidate()
        }
    var startColor: Int = 0x002563EB
        set(value) { field = value; geometryDirty = true; invalidate() }
    var endColor: Int = 0xFF2563EB.toInt()
        set(value) { field = value; geometryDirty = true; invalidate() }
    /** Time for one clockwise revolution in milliseconds. */
    var durationMillis: Long = 1400L
        set(value) {
            require(value > 0) { "durationMillis must be positive" }
            field = value
            animator?.cancel()
            animator = null
            syncAnimation()
        }
    /** Bezier control angle in degrees, 0..45. Larger values give a squarer ring. */
    var shapeAngle: Float = 9.875864f
        set(value) {
            require(value.isFinite() && value in 0f..45f) { "shapeAngle must be in 0..45 degrees" }
            field = value
            geometryDirty = true
            invalidate()
        }

    private val motionObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { syncAnimation(); invalidate() }
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.BesselLoadingView, defStyleAttr, 0)
        try {
            isLoading = a.getBoolean(R.styleable.BesselLoadingView_bessel_autoStart, true)
            strokeWidth = a.getDimension(R.styleable.BesselLoadingView_bessel_strokeWidth, strokeWidth)
            startColor = a.getColor(R.styleable.BesselLoadingView_bessel_startColor, startColor)
            endColor = a.getColor(R.styleable.BesselLoadingView_bessel_endColor, endColor)
            durationMillis = a.getInt(R.styleable.BesselLoadingView_bessel_duration, 1400).toLong()
            shapeAngle = a.getFloat(R.styleable.BesselLoadingView_bessel_shapeAngle, shapeAngle)
        } finally { a.recycle() }
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        ready = true
    }

    /** Start or resume loading. Safe to call repeatedly, including before attachment. */
    fun start() { isLoading = true }

    /** Freeze the gradient in place. The ring remains visible. */
    fun stop() { isLoading = false }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (48 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(
            resolveSize(maxOf(suggestedMinimumWidth, size + paddingLeft + paddingRight), widthMeasureSpec),
            resolveSize(maxOf(suggestedMinimumHeight, size + paddingTop + paddingBottom), heightMeasureSpec),
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { geometryDirty = true }

    private fun updateGeometry() {
        geometryDirty = false
        centerX = paddingLeft + (width - paddingLeft - paddingRight) / 2f
        centerY = paddingTop + (height - paddingTop - paddingBottom) / 2f
        val available = min(width - paddingLeft - paddingRight, height - paddingTop - paddingBottom).coerceAtLeast(0).toFloat()
        paint.strokeWidth = min(strokeWidth, available / 2f)
        drawable = available > 0f && paint.strokeWidth > 0f
        val tangent = tan(Math.toRadians(shapeAngle.toDouble())).toFloat()
        // Original control points and diagonal midpoints; fit the actual cubic extrema to padding.
        val radius = ((available - paint.strokeWidth) / 2f) / (0.875f + 0.125f * tangent)
        val control = tangent * radius
        val midpoint = (radius + control) / 2f
        ring.rewind()
        ring.moveTo(centerX - midpoint, centerY - midpoint)
        ring.cubicTo(centerX - radius, centerY - control, centerX - radius, centerY + control, centerX - midpoint, centerY + midpoint)
        ring.cubicTo(centerX - control, centerY + radius, centerX + control, centerY + radius, centerX + midpoint, centerY + midpoint)
        ring.cubicTo(centerX + radius, centerY + control, centerX + radius, centerY - control, centerX + midpoint, centerY - midpoint)
        ring.cubicTo(centerX + control, centerY - radius, centerX - control, centerY - radius, centerX - midpoint, centerY - midpoint)
        ring.close()
        gradient = SweepGradient(centerX, centerY, startColor, endColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (geometryDirty) updateGeometry()
        if (!drawable) return
        matrix.setRotate(phase - 90f, centerX, centerY)
        gradient?.setLocalMatrix(matrix)
        paint.shader = gradient
        canvas.drawPath(ring, paint)
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        geometryDirty = true
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        geometryDirty = true
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(layoutDirection)
        geometryDirty = true
    }

    private fun motionEnabled(): Boolean = if (Build.VERSION.SDK_INT >= 26) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

    private fun syncAnimation() {
        if (!ready) return
        val shouldAnimate = isLoading && isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
            (Build.VERSION.SDK_INT < 24 || aggregatedVisible) && motionEnabled()
        if (!shouldAnimate) {
            animator?.cancel()
            animator = null
        } else if (animator == null) {
            val startingPhase = phase
            animator = ValueAnimator.ofFloat(startingPhase, startingPhase + 360f).apply {
                duration = durationMillis
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { phase = (it.animatedValue as Float) % 360f; invalidate() }
                start()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, motionObserver)
        syncAnimation()
    }

    override fun onDetachedFromWindow() {
        context.contentResolver.unregisterContentObserver(motionObserver)
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        syncAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        syncAnimation()
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        aggregatedVisible = isVisible
        syncAnimation()
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = ProgressBar::class.java.name
        val state = resources.getString(if (isLoading) R.string.bessel_loading else R.string.bessel_stopped)
        if (Build.VERSION.SDK_INT >= 30) info.stateDescription = state
        info.contentDescription = when {
            contentDescription.isNullOrEmpty() -> state
            Build.VERSION.SDK_INT < 30 -> resources.getString(R.string.bessel_description_with_state, contentDescription, state)
            else -> contentDescription
        }
    }

    override fun onSaveInstanceState(): Parcelable = Bundle().apply {
        putParcelable("super", super.onSaveInstanceState())
        putBoolean("loading", isLoading)
        putFloat("stroke", strokeWidth)
        putFloat("shape", shapeAngle)
        putInt("start", startColor)
        putInt("end", endColor)
        putLong("duration", durationMillis)
    }

    @Suppress("DEPRECATION")
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is Bundle) { super.onRestoreInstanceState(state); return }
        super.onRestoreInstanceState(state.getParcelable("super"))
        isLoading = state.getBoolean("loading")
        strokeWidth = state.getFloat("stroke")
        shapeAngle = state.getFloat("shape")
        startColor = state.getInt("start")
        endColor = state.getInt("end")
        durationMillis = state.getLong("duration")
    }
}
