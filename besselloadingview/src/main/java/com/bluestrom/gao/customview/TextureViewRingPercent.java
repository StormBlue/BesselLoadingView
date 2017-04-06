package com.bluestrom.gao.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import static android.graphics.Region.Op.INTERSECT;

/**
 * Created by Gao-Krund on 2016/8/4.
 */
public class TextureViewRingPercent extends TextureView implements TextureView.SurfaceTextureListener {

    private final String TAG = this.getClass().getSimpleName();

    private final static int ROTATE_PATH = 0;

    private final float strokeWidthRatio = 0.2f;

    private float rotateAngle = 0;
    private float ringOutRadius;// 圆环外层半径
    private float lengthMargin = 0f;

    private int width, height, lengthDValue, rotateX, rotateY;
    private Paint gapPathPaint, bitmapPaint;

    private Canvas mSrcCanvas, mBullishCanvas, mBearsCanvas;
    private Bitmap mSrcBitmap, mBullishMaskBitmap, mBearishMaskBitmap;

    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次

    private RectF mRectF;

    private Path bullishPath = new Path(),bearishPath = new Path();

    private RenderThread renderThread;

    private Bitmap bg;

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

    }

    private void init() {
        initView();
        renderThread = new RenderThread(getContext());
    }

    private void initView() {
        firstDraw = true;
        gapPathPaint = new Paint();
        gapPathPaint.setStyle(Paint.Style.STROKE);

        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setAntiAlias(true);
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
        mBullishMaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bullish_mask);
        mBearishMaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bearish_mask);
        mBullishCanvas = new Canvas(mBullishMaskBitmap);
        mBearsCanvas = new Canvas(mBearishMaskBitmap);

        mRectF = new RectF(rotateX - ringOutRadius, rotateY - ringOutRadius,
                rotateX + ringOutRadius, rotateY + ringOutRadius);

        mSrcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mSrcCanvas = new Canvas(mSrcBitmap);
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

            while (isRunning && rotateAngle < 360) {
                try {
                    long startTime = System.currentTimeMillis();
                    drawBackground();
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
            Log.i(TAG, "子线程完成");

        }

        private void drawBackground() {
            bullishPath.addArc(mRectF, -90, rotateAngle * (0.7f - 0.02f));
            mBullishCanvas.clipPath(bullishPath, INTERSECT);

            bearishPath.addArc(mRectF,-90 + rotateAngle * 0.7f, rotateAngle * (0.3f - 0.02f));
            mBearsCanvas.clipPath(bearishPath,INTERSECT);

            drawPathBitmap();
        }

        private void drawPathBitmap() {
            mSrcCanvas.drawBitmap(,0,0,bitmapPaint);
            c = lockCanvas(null);
            c.drawBitmap(bg, 0, 0, bitmapPaint);
            c.drawBitmap(mSrcBitmap, 0, 0, bitmapPaint);
            unlockCanvasAndPost(c);
        }

    }
}
