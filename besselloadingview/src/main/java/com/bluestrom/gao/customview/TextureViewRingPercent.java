package com.bluestrom.gao.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

/**
 * Created by Gao-Krund on 2016/8/4.
 */
public class TextureViewRingPercent extends TextureView implements TextureView.SurfaceTextureListener {

    private final String TAG = this.getClass().getSimpleName();

    private final static int ROTATE_PATH = 0;

    private float strokeWidth;
    private float octagonAngle;
    private int bullishStartColor;
    private int bullishEndColor;
    private int bearishStartColor;
    private int bearishEndColor;
    private int maskColor;

    private float rotateAngle = 0;
    private float ringOutRadius;// 圆环外层半径
    private float ringInRadius;// 圆环内层半径
    private float lengthMargin = 0f;

    private int width, height, lengthDValue, rotateX, rotateY;
    private Paint pathPaint, bitmapPaint;

    private Canvas mSrcCanvas;
    private Bitmap mSrcBitmap;

    private Matrix matrix;
    private SweepGradient sweepGradient;
    private RectF mRectF;

    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次

    private RenderThread renderThread;

    private final float default_stroke_width = 45;
    private final int default_bullish_start_color = 0xFF5F6F;
    private final int default_bullish_end_color = 0xFF0037;
    private final int default_bearish_start_color = 0x00D264;
    private final int default_bearish_end_color = 0x00B234;
    private final int default_mask_color = 0xFFEEEEEE;

    private static final String INSTANCE_STATE = "saved_instance";

    public TextureViewRingPercent(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BesselLoading);
        initByAttributes(mTypedArray);
        init();
        setSurfaceTextureListener(this);
        setOpaque(false);
        mTypedArray.recycle();
    }

    protected void initByAttributes(TypedArray attributes) {
        strokeWidth = attributes.getDimension(R.styleable.BesselLoading_loading_stroke_width, default_stroke_width);
        maskColor = attributes.getColor(R.styleable.BesselLoading_loading_end_color, default_mask_color);
        bullishStartColor = default_bullish_start_color;
        bullishEndColor = default_bullish_end_color;
        bearishStartColor = default_bearish_start_color;
        bearishEndColor = default_bearish_end_color;
    }

    private void init() {
        initView();
        renderThread = new RenderThread(getContext());
    }

    private void initView() {
        firstDraw = true;

        pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(strokeWidth);

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
//        bitmapPaint.setFilterBitmap(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        lengthDValue = (width - height) / 2;
        rotateX = width / 2;
        rotateY = height / 2;
        initParameters();
    }

    // 初始化bessel曲线控制参数
    private void initParameters() {
        // 圆环左端坐标与view最左侧的间距
        float leftD = lengthDValue > 0f ? lengthDValue : 0f;
        ringOutRadius = rotateX - leftD;
        strokeWidth = ringOutRadius / 5 * 3;
        ringInRadius = ringOutRadius - strokeWidth;
        mRectF = new RectF(rotateX - ringOutRadius, rotateY - ringOutRadius, rotateX + ringOutRadius, rotateY + ringOutRadius);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        renderThread.isRunning = true;
        renderThread.start();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        renderThread.isRunning = false;
        try {
            renderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    /**
     * 执行绘制的绘制线程
     *
     * @author Administrator
     */
    class RenderThread extends Thread {

        Context context;
        boolean isRunning;
        Paint paint;
        Canvas c = null;

        public RenderThread(Context context) {

            this.context = context;
            isRunning = false;

            paint = new Paint();
            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
        }

        @Override
        public void run() {

            while (isRunning && rotateAngle <= 360) {
                try {
                    long startTime = System.currentTimeMillis();
                    drawBackground();
                    Log.i(TAG, "绘制" + rotateAngle);
                    firstDraw = false;
                    rotateAngle += 3;
                    long dif = System.currentTimeMillis() - startTime;
                    if (dif < 15) {
                        sleep(15 - dif);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        // 绘制有透明区域的背景
        private void drawBackground() {
            if (firstDraw) {
                // 绘制首次显示的贝塞尔曲线
                pathPaint.setColor(getResources().getColor(android.R.color.holo_red_dark));
                mSrcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mSrcCanvas = new Canvas(mSrcBitmap);
                sweepGradient = new SweepGradient(rotateX, rotateY, bullishEndColor, bullishStartColor);
//                pathPaint.setShader(sweepGradient);
            }
            drawPathBitmap();
        }

        private void drawPathBitmap() {
            mSrcCanvas.drawArc(mRectF, 0, rotateAngle, true, pathPaint);
            c = lockCanvas(null);
            c.drawBitmap(mSrcBitmap, 0, 0, bitmapPaint);
            unlockCanvasAndPost(c);
        }

    }
}
