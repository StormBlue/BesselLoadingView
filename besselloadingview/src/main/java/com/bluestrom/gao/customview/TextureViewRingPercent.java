package com.bluestrom.gao.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

/**
 * Created by Gao-Krund on 2016/8/4.
 */
public class TextureViewRingPercent extends TextureView implements TextureView.SurfaceTextureListener {

    private final String TAG = this.getClass().getSimpleName();

    private final static int ROTATE_PATH = 0;

    private final float strokeWidthRatio = 0.2f;

    private float strokeWidth;
    private float rotateAngle = 0;
    private float ringOutRadius;// 圆环外层半径
    private float lengthMargin = 0f;

    private int width, height, lengthDValue, rotateX, rotateY;
    private Paint mClearPaint, gapPathPaint, bitmapPaint, bullishPaint, bearishPaint;

    private Canvas mSrcCanvas, mBullishCanvas, mBearishCanvas;
    private Bitmap mSrcBitmap, mBullishBitmap, mBearishBitmap, mBullishMaskBitmap, mBearishMaskBitmap;

    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次

    private RectF mRectF;

    private Path bullishPath = new Path(), bearishPath = new Path();

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
        // 关闭硬件加速 Xfermode不支持硬件加速
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    protected void initByAttributes(TypedArray attributes) {

    }

    private void init() {
        initView();
        renderThread = new RenderThread(getContext());
    }

    private void initView() {
        firstDraw = true;
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        gapPathPaint = new Paint();
        gapPathPaint.setStyle(Paint.Style.STROKE);

        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setAntiAlias(true);

        bullishPaint = new Paint();
        bullishPaint.setStyle(Paint.Style.STROKE);
        bullishPaint.setDither(true);
        bullishPaint.setAntiAlias(true);
        bullishPaint.setStrokeCap(Paint.Cap.BUTT);

        bearishPaint = new Paint();
        bearishPaint.setFilterBitmap(true);
        bearishPaint.setAntiAlias(true);
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

    // 初始化控制参数
    private void initParameters() {
        // 圆环左端坐标与view最左侧的间距
        float leftD = lengthDValue > 0f ? lengthDValue : 0f;
        ringOutRadius = rotateX - leftD;
        strokeWidth = ringOutRadius / 5 * 2;
        mRectF = new RectF(rotateX - ringOutRadius + strokeWidth / 2, rotateY - ringOutRadius + strokeWidth / 2,
                rotateX + ringOutRadius - strokeWidth / 2, rotateY + ringOutRadius - strokeWidth / 2);

        mBullishMaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bullish_mask).copy(Bitmap.Config.ARGB_8888, true);
        Matrix bullishMatrix = new Matrix();
        float bullishMaskScaleRatio = getMaskBitmapRatio(mBullishMaskBitmap.getWidth(), mBullishMaskBitmap.getHeight(), width, height);
        bullishMatrix.postScale(bullishMaskScaleRatio, bullishMaskScaleRatio);
        mBullishMaskBitmap = Bitmap.createBitmap(mBullishMaskBitmap, 0, 0, mBullishMaskBitmap.getWidth(),
                mBullishMaskBitmap.getHeight(), bullishMatrix, true);
        mBullishCanvas = new Canvas();

        mBearishMaskBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bearish_mask).copy(Bitmap.Config.ARGB_8888, true);
        Matrix bearishMatrio = new Matrix();
        float bearishMaskScaleRatio = getMaskBitmapRatio(mBearishMaskBitmap.getWidth(), mBearishMaskBitmap.getHeight(), width, height);
        bearishMatrio.postScale(bearishMaskScaleRatio, bearishMaskScaleRatio);
        mBearishMaskBitmap = Bitmap.createBitmap(mBearishMaskBitmap, 0, 0, mBearishMaskBitmap.getWidth(),
                mBearishMaskBitmap.getHeight(), bearishMatrio, true);
        mBearishCanvas = new Canvas();

        mSrcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mSrcCanvas = new Canvas(mSrcBitmap);
        bullishPaint.setStrokeWidth(width * 0.2f);
    }

    private float getMaskBitmapRatio(int oriWidth, int oriHeight, int dstWidth, int dstHeight) {
        float result;
        if ((((float) oriWidth) / oriHeight) > (((float) dstWidth) / dstHeight)) {
            result = ((float) dstWidth) / oriHeight;
        } else {
            result = ((float) dstHeight) / oriHeight;
        }
        return result;
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
                    rotateAngle += 6;
                    drawPathBitmap();
                    Log.i(TAG, "耗时-------" + (System.currentTimeMillis() - startTime));
                    firstDraw = false;
                    long dif = System.currentTimeMillis() - startTime;
                    if (dif < 24) {
                        sleep(24 - dif);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void drawPathBitmap() {
            c = lockCanvas(null);
//            清屏
            c.drawPaint(mClearPaint);
            if (firstDraw) {
                mBullishBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mBullishCanvas = new Canvas(mBullishBitmap);
                mBearishBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mBearishCanvas = new Canvas(mBearishBitmap);
            }
//            mBullishCanvas.drawPaint(mClearPaint);
            mBullishCanvas.drawArc(mRectF, -90, rotateAngle * (0.7f - 0.02f), false, bullishPaint);
            bullishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            mBullishCanvas.drawBitmap(mBullishMaskBitmap, 0, 0, bullishPaint);
            bullishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

//            mBearishCanvas.drawPaint(mClearPaint);
            mBearishCanvas.drawArc(mRectF, rotateAngle * (0.7f) - 90, rotateAngle * (0.3f - 0.02f), false, bullishPaint);
            bearishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            mBearishCanvas.drawBitmap(mBearishMaskBitmap, 0, 0, bearishPaint);
            bearishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

            c.drawBitmap(mBullishBitmap, 0, 0, bitmapPaint);
            c.drawBitmap(mBearishBitmap, 0, 0, bitmapPaint);

            unlockCanvasAndPost(c);
        }

    }
}
