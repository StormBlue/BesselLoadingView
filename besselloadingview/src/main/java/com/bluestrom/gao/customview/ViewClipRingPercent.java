package com.bluestrom.gao.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class ViewClipRingPercent extends View {
    private final String TAG = this.getClass().getSimpleName();

    private final static int ROTATE_PATH = 0;

    private float strokeWidth;

    private float rotateAngle;
    private float ringOutRadius;// 圆环外层半径
    private float ringInRadius;// 圆环内层半径
    private float lengthMargin = 0f;

    private int width, height, lengthDValue, rotateX, rotateY;

    private RectF mRectF, mGapRectF;

    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次

    private static final String INSTANCE_STATE = "saved_instance";

    private Paint mClearPaint, gapPathPaint, bitmapPaint, bullishPaint, bearishPaint;

    private Canvas mGapCanvas, mBullishCanvas, mBearishCanvas;
    private Bitmap mGapBitmap, mBullishBitmap, mBearishBitmap, mBullishMaskBitmap, mBearishMaskBitmap;

    public ViewClipRingPercent(Context context) {
        this(context, null);
    }

    public ViewClipRingPercent(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public ViewClipRingPercent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BesselLoading);
        initByAttributes(mTypedArray);
        initView();
        mTypedArray.recycle();
        // 关闭硬件加速 Xfermode不支持硬件加速
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    protected void initByAttributes(TypedArray attributes) {

    }

    public void initView() {
        firstDraw = true;

        firstDraw = true;
        mClearPaint = new Paint();
        mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        gapPathPaint = new Paint();
        gapPathPaint.setStyle(Paint.Style.FILL);
        gapPathPaint.setColor(Color.BLUE);
        gapPathPaint.setStrokeWidth(60);

        bitmapPaint = new Paint();
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setAntiAlias(true);

        bullishPaint = new Paint();
        bullishPaint.setStyle(Paint.Style.FILL);
        bullishPaint.setDither(true);
        bullishPaint.setAntiAlias(true);
        bullishPaint.setColor(Color.WHITE);
//        bullishPaint.setStrokeCap(Paint.Cap.BUTT);

        bearishPaint = new Paint();
        bearishPaint.setStyle(Paint.Style.FILL);
        bearishPaint.setFilterBitmap(true);
        bearishPaint.setAntiAlias(true);
        bearishPaint.setColor(Color.WHITE);
//        bullishPaint.setStrokeCap(Paint.Cap.BUTT);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
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
        ringInRadius = ringOutRadius - strokeWidth;
//        mRectF = new RectF(rotateX - ringOutRadius + strokeWidth / 2, rotateY - ringOutRadius + strokeWidth / 2,
//                rotateX + ringOutRadius - strokeWidth / 2, rotateY + ringOutRadius - strokeWidth / 2);
        mRectF = new RectF(0, 0, width, height);
        mGapRectF = new RectF(0, rotateY - 30, width, rotateY - 30);

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
//        bullishPaint.setStrokeWidth(strokeWidth);
//        bearishPaint.setStrokeWidth(strokeWidth);
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long startTime = System.currentTimeMillis();
        drawPathBitmap(canvas);
        Log.i(TAG, "耗时---" + (System.currentTimeMillis() - startTime));
    }

    private void drawPathBitmap(Canvas c) {
//            清屏
        c.drawColor(Color.WHITE);
        if (firstDraw) {
            mBullishBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mBullishCanvas = new Canvas(mBullishBitmap);
            mBearishBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mBearishCanvas = new Canvas(mBearishBitmap);
            mGapBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mGapCanvas = new Canvas(mGapBitmap);
        }
        mBullishCanvas.drawPaint(mClearPaint);
        mBullishCanvas.drawArc(mRectF, -90, rotateAngle * (0.7f - 0.02f), true, bullishPaint);
        bullishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        mBullishCanvas.drawBitmap(mBullishMaskBitmap, 0, 0, bullishPaint);
        bullishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        mBearishCanvas.drawPaint(mClearPaint);
        mBearishCanvas.drawArc(mRectF, rotateAngle * (0.7f) - 90, rotateAngle * (0.3f - 0.02f), true,
                bearishPaint);
        bearishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        mBearishCanvas.drawBitmap(mBearishMaskBitmap, 0, 0, bearishPaint);
        bearishPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        mGapCanvas.drawPaint(mClearPaint);
        mGapCanvas.drawLine(width / 2, 0, width / 2, height / 2, gapPathPaint);

        c.drawBitmap(mBullishBitmap, 0, 0, bitmapPaint);
        c.drawBitmap(mBearishBitmap, 0, 0, bitmapPaint);
        c.drawBitmap(mGapBitmap, 0, 0, bitmapPaint);
        mGapCanvas.rotate(10, rotateX, rotateY);
    }

    private volatile boolean loading = false;

    public void improveAngle() {

        postInvalidate();// 非创建此view的线程更新view
    }

    // 开启旋转
    public void startRotate() {
        rotateAngle = 0;
//        uiHandler.removeMessages(ROTATE_PATH);
//        uiHandler.sendEmptyMessage(ROTATE_PATH);
        new TaskThread().start();
    }

    class TaskThread extends Thread {
        private boolean isRunning = true;

        @Override
        public void run() {
            while (isRunning && rotateAngle < 360) {
                try {
                    long startTime = System.currentTimeMillis();
                    rotateAngle += 10;
                    postInvalidate();
                    firstDraw = false;
                    long dif = System.currentTimeMillis() - startTime;
                    if (dif < 16) {
                        sleep(16 - dif);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 停止旋转
    public void stopRotate() {
        uiHandler.removeMessages(ROTATE_PATH);
    }

    private Handler uiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ROTATE_PATH:
                    if (rotateAngle < 360) {
                        rotateAngle += 6;
                        postInvalidate();
                        uiHandler.sendEmptyMessage(ROTATE_PATH);
                    }
                    break;
                default:
                    break;
            }
        }
    };

}
