package com.bluestrom.gao.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

public class ViewRingPercent extends View {
    private final String TAG = this.getClass().getSimpleName();

    private final static int ROTATE_PATH = 0;

    private float strokeWidth;
    private float octagonAngle;
    private int bullishStartColor;
    private int bullishEndColor;
    private int bearishStartColor;
    private int bearishEndColor;
    private int maskColor;

    private float rotateAngle;
    private float ringOutRadius;// 圆环外层半径
    private float ringInRadius;// 圆环内层半径
    private float lengthMargin = 0f;

    private Matrix matrix;

    private int width, height, lengthDValue, rotateX, rotateY;
    private Paint bullishPathPaint, bearishPathPaint;

    private SweepGradient sweepBullishGradient, sweepBearishGradient;
    private RectF mRectF;

    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次

    private final float default_stroke_width = 45;
    private final int default_bullish_start_color = 0xFFFF5F6F;
    private final int default_bullish_end_color = 0xFFFF0037;
    private final int default_bearish_start_color = 0xFF00D264;
    private final int default_bearish_end_color = 0xFF00B234;
    private final int default_mask_color = 0xFFEEEEEE;

    private static final String INSTANCE_STATE = "saved_instance";

    private final float bullishPercent = 0.7f;

    public ViewRingPercent(Context context) {
        this(context, null);
    }

    public ViewRingPercent(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public ViewRingPercent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BesselLoading);
        initByAttributes(mTypedArray);
        initView();
        mTypedArray.recycle();
        // 关闭硬件加速 Xfermode不支持硬件加速
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    protected void initByAttributes(TypedArray attributes) {
        bullishStartColor = default_bullish_start_color;
        bullishEndColor = default_bullish_end_color;
        bearishStartColor = default_bearish_start_color;
        bearishEndColor = default_bearish_end_color;
    }

    public void initView() {
        firstDraw = true;
        bullishPathPaint = new Paint();
        bullishPathPaint.setStyle(Paint.Style.STROKE);
        bullishPathPaint.setDither(true);
        bullishPathPaint.setAntiAlias(true);
        bullishPathPaint.setStrokeCap(Paint.Cap.BUTT);

        bearishPathPaint = new Paint();
        bearishPathPaint.setStyle(Paint.Style.STROKE);
        bearishPathPaint.setDither(true);
        bearishPathPaint.setAntiAlias(true);
        bearishPathPaint.setStrokeCap(Paint.Cap.BUTT);
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


    // 初始化bessel曲线控制参数
    private void initParameters() {
        // 圆环左端坐标与view最左侧的间距
        float leftD = lengthDValue > 0f ? lengthDValue : 0f;
        ringOutRadius = rotateX - leftD;
        strokeWidth = ringOutRadius / 5 * 2;
        ringInRadius = ringOutRadius - strokeWidth;
        mRectF = new RectF(rotateX - ringOutRadius + strokeWidth / 2, rotateY - ringOutRadius + strokeWidth / 2,
                rotateX + ringOutRadius - strokeWidth / 2, rotateY + ringOutRadius - strokeWidth / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
//        Paint paint = new Paint();
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        canvas.drawPaint(paint);
        drawPathBitmap(canvas);
    }

    // 绘制有透明区域的背景
    private void drawPathBitmap(Canvas canvas) {
        if (firstDraw) {
            firstDraw = false;
            matrix = new Matrix();
            matrix.setRotate(-90, rotateX, rotateY);
            sweepBullishGradient = new SweepGradient(rotateX, rotateY, bullishStartColor, bullishEndColor);
            bullishPathPaint.setStrokeWidth(strokeWidth);
            bullishPathPaint.setShader(sweepBullishGradient);
            sweepBullishGradient.setLocalMatrix(matrix);

            sweepBearishGradient = new SweepGradient(rotateX, rotateY, bearishStartColor, bearishEndColor);
            bearishPathPaint.setStrokeWidth(strokeWidth);
            bearishPathPaint.setShader(sweepBearishGradient);
            sweepBearishGradient.setLocalMatrix(matrix);
        }
        canvas.drawArc(mRectF, -90, rotateAngle * (0.7f - 0.02f), false, bullishPathPaint);
        canvas.drawArc(mRectF, -90 + rotateAngle * 0.7f, rotateAngle * (0.3f - 0.02f), false, bearishPathPaint);
    }

    private volatile boolean loading = false;

    public void improveAngle() {
        postInvalidate();// 非创建此view的线程更新view
    }

    // 开启旋转
    public void startRotate() {
        rotateAngle = 0;
        uiHandler.removeMessages(ROTATE_PATH);
        uiHandler.sendEmptyMessage(ROTATE_PATH);
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
                        rotateAngle += 12;
                        postInvalidate();
                        uiHandler.sendEmptyMessageDelayed(ROTATE_PATH, 20);
                    }
                    break;
                default:
                    break;
            }
        }
    };

}
