package com.bluestrom.gao.customview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.SweepGradient;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

public class KrundBesselLoading extends View {
    private final String TAG = this.getClass().getSimpleName();

    private final static int ROTATE_PATH = 0;

    private float strokeWidth;
    private float octagonAngle;
    private int loadingStartColor;
    private int loadingEndColor;
    private int maskColor;
    private DrawMode drawMode;// 绘制模式

    private float rotateAngle = 0;
    private float octagonAngleTangent = 0.17409388f;
    private final float lengthRatio = 0.1107427056f;
    private float ringOutRadius;// 圆环外层半径
    private float lengthMargin = 0f;

    private int width, height, lengthDValue, rotateX, rotateY;
    private Paint pathPaint, bitmapPaint;
    private float[] coordinate_01, coordinate_02, coordinate_03, coordinate_04, coordinate_05, coordinate_06, coordinate_07, coordinate_08, coordinate_a, coordinate_b, coordinate_c, coordinate_d;
    private Path besselPath;

    private Canvas mSrcCanvas;
    private Bitmap mSrcBitmap;

    private Matrix matrix;
    private SweepGradient sweepGradient;

    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次

    private final float default_octagon_angle = 9.8758636243f;
    private final float default_stroke_width = 10;
    private final int default_loading_start_color = Color.BLUE;
    private final int default_loading_end_color = Color.WHITE;
    private final int default_mask_color = Color.WHITE;
    private final DrawMode default_draw_mode = DrawMode.STANDARD;

    private static final String INSTANCE_STATE = "saved_instance";
    private static final String INSTANCE_OCTAGON_ANGLE = "octagon_angle";
    private static final String INSTANCE_STROKE_WIDTH = "stroke_width";
    private static final String INSTANCE_LOADING_START_COLOR = "loading_start_color";
    private static final String INSTANCE_LOADING_END_COLOR = "loading_end_color";
    private static final String INSTANCE_MASK_COLOR = "mask_color";
    private static final String INSTANCE_DRAW_MODE = "draw_mode";

    public KrundBesselLoading(Context context) {
        this(context, null);
    }

    public KrundBesselLoading(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public KrundBesselLoading(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BesselLoading);
        initByAttributes(mTypedArray);
        initView();
        mTypedArray.recycle();
        // 关闭硬件加速 Xfermode不支持硬件加速
//        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    protected void initByAttributes(TypedArray attributes) {
        octagonAngle = attributes.getFloat(R.styleable.BesselLoading_octagon_angle, default_octagon_angle);
        strokeWidth = attributes.getDimension(R.styleable.BesselLoading_loading_stroke_width, default_stroke_width);
        loadingStartColor = attributes.getColor(R.styleable.BesselLoading_loading_start_color, default_loading_start_color);
        loadingEndColor = attributes.getColor(R.styleable.BesselLoading_loading_end_color, default_loading_end_color);
        maskColor = attributes.getColor(R.styleable.BesselLoading_loading_end_color, default_mask_color);
        drawMode = DrawMode.valueOf(attributes.getInt(R.styleable.BesselLoading_loading_draw_mode, default_draw_mode.nativeInt));
    }

    public void initView() {
        firstDraw = true;
        octagonAngleTangent = (float) Math.tan(octagonAngle / 180 * Math.PI);

        pathPaint = new Paint();
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(strokeWidth);
        pathPaint.setDither(true);
        pathPaint.setAntiAlias(true);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);

        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
        lengthDValue = (width - height) / 2;
        rotateX = width / 2;
        rotateY = height / 2;
        initBesselCoordinate();
    }


    // 初始化bessel曲线控制参数
    private void initBesselCoordinate() {
        // 圆环左端坐标与view最左侧的间距
        float leftD = lengthDValue > 0f ? lengthDValue : 0f;
        ringOutRadius = rotateX - leftD;
        if (DrawMode.RIVISION.nativeInt == getDrawMode().nativeInt) {
            lengthMargin = lengthRatio * ringOutRadius - (float) strokeWidth;
        }
        float pointMargin = octagonAngleTangent * ringOutRadius;
        coordinate_01 = new float[]{rotateX - ringOutRadius - lengthMargin, rotateY - pointMargin - lengthMargin};
        coordinate_02 = new float[]{rotateX - ringOutRadius - lengthMargin, rotateY + pointMargin + lengthMargin};
        coordinate_03 = new float[]{rotateX - pointMargin - lengthMargin, rotateY + ringOutRadius + lengthMargin};
        coordinate_04 = new float[]{rotateX + pointMargin + lengthMargin, rotateY + ringOutRadius + lengthMargin};
        coordinate_05 = new float[]{rotateX + ringOutRadius + lengthMargin, rotateY + pointMargin + lengthMargin};
        coordinate_06 = new float[]{rotateX + ringOutRadius + lengthMargin, rotateY - pointMargin - lengthMargin};
        coordinate_07 = new float[]{rotateX + pointMargin + lengthMargin, rotateY - ringOutRadius - lengthMargin};
        coordinate_08 = new float[]{rotateX - pointMargin - lengthMargin, rotateY - ringOutRadius - lengthMargin};
        coordinate_a = new float[]{(coordinate_01[0] + coordinate_08[0]) / 2, (coordinate_01[1] + coordinate_08[1]) / 2};
        coordinate_b = new float[]{(coordinate_02[0] + coordinate_03[0]) / 2, (coordinate_02[1] + coordinate_03[1]) / 2};
        coordinate_c = new float[]{(coordinate_04[0] + coordinate_05[0]) / 2, (coordinate_04[1] + coordinate_05[1]) / 2};
        coordinate_d = new float[]{(coordinate_06[0] + coordinate_07[0]) / 2, (coordinate_06[1] + coordinate_07[1]) / 2};

        besselPath = new Path();
        besselPath.moveTo(coordinate_a[0], coordinate_a[1]);

        besselPath.cubicTo(coordinate_01[0], coordinate_01[1], coordinate_02[0], coordinate_02[1], coordinate_b[0], coordinate_b[1]);
        besselPath.cubicTo(coordinate_03[0], coordinate_03[1], coordinate_04[0], coordinate_04[1], coordinate_c[0], coordinate_c[1]);
        besselPath.cubicTo(coordinate_05[0], coordinate_05[1], coordinate_06[0], coordinate_06[1], coordinate_d[0], coordinate_d[1]);
        besselPath.cubicTo(coordinate_07[0], coordinate_07[1], coordinate_08[0], coordinate_08[1], coordinate_a[0], coordinate_a[1]);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawBackground();
        firstDraw = false;
        canvas.drawBitmap(mSrcBitmap, 0, 0, bitmapPaint);
    }

    // 绘制有透明区域的背景
    private void drawBackground() {
        if (!firstDraw) return;
        // 绘制背景
        Bitmap bg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas bgCanvas = new Canvas(bg);

        Paint paintBg = new Paint();
        paintBg.setStyle(Paint.Style.FILL);
        Bitmap dstBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas dstCanvas = new Canvas(dstBitmap);
        paintBg.setColor(Color.rgb(255, 255, 255));
        paintBg.setAntiAlias(true);
        dstCanvas.drawPath(besselPath, paintBg);
        bgCanvas.drawBitmap(dstBitmap, 0, 0, paintBg);

        Bitmap srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas srcCanvas = new Canvas(srcBitmap);
        paintBg.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        srcCanvas.drawColor(maskColor);
        bgCanvas.drawBitmap(srcBitmap, 0, 0, paintBg);

        setBackground(new BitmapDrawable(null, bg));

        // 绘制首次显示的贝塞尔曲线
        mSrcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mSrcCanvas = new Canvas(mSrcBitmap);
        sweepGradient = new SweepGradient(rotateX, rotateY, Color.WHITE, Color.BLUE);
        matrix = new Matrix();
        pathPaint.setShader(sweepGradient);
        drawPathBitmap();
    }

    private void drawPathBitmap() {
        matrix.setRotate(rotateAngle - 90, rotateX, rotateY);
        sweepGradient.setLocalMatrix(matrix);
        mSrcCanvas.drawPath(besselPath, pathPaint);
    }

    private volatile boolean loading = false;

    public void improveAngle() {

        postInvalidate();// 非创建此view的线程更新view
    }

    // 开启旋转
    public void startRotate() {
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
                    rotateAngle += 4;
                    rotateAngle = (rotateAngle % 360);
                    drawPathBitmap();
                    postInvalidate();
                    uiHandler.sendEmptyMessageDelayed(ROTATE_PATH, 5);
                    break;
                default:
                    break;
            }
        }
    };

    public enum DrawMode {

        STANDARD(0),

        RIVISION(1);

        DrawMode(int ni) {
            nativeInt = ni;
        }

        final int nativeInt;

        public static DrawMode valueOf(int ordinal) {
            if (ordinal < 0 || ordinal >= values().length) {
                throw new IndexOutOfBoundsException("Invalid ordinal");
            }
            return values()[ordinal];
        }
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public float getOctagonAngle() {
        return octagonAngle;
    }

    public void setOctagonAngle(float octagonAngle) {
        this.octagonAngle = octagonAngle;
    }

    public int getLoadingStartColor() {
        return loadingStartColor;
    }

    public void setLoadingStartColor(int loadingStartColor) {
        this.loadingStartColor = loadingStartColor;
    }

    public int getLoadingEndColor() {
        return loadingEndColor;
    }

    public void setLoadingEndColor(int loadingEndColor) {
        this.loadingEndColor = loadingEndColor;
    }

    public int getMaskColor() {
        return maskColor;
    }

    public void setMaskColor(int maskColor) {
        this.maskColor = maskColor;
    }

    public DrawMode getDrawMode() {
        return drawMode;
    }

    public void setDrawMode(DrawMode drawMode) {
        this.drawMode = drawMode;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState());
        bundle.putFloat(INSTANCE_STROKE_WIDTH, getStrokeWidth());
        bundle.putFloat(INSTANCE_OCTAGON_ANGLE, getOctagonAngle());
        bundle.putInt(INSTANCE_LOADING_START_COLOR, getLoadingStartColor());
        bundle.putInt(INSTANCE_LOADING_END_COLOR, getLoadingEndColor());
        bundle.putInt(INSTANCE_MASK_COLOR, getMaskColor());
        bundle.putInt(INSTANCE_DRAW_MODE, getDrawMode().nativeInt);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            final Bundle bundle = (Bundle) state;
            strokeWidth = bundle.getFloat(INSTANCE_STROKE_WIDTH);
            octagonAngle = bundle.getFloat(INSTANCE_OCTAGON_ANGLE);
            loadingStartColor = bundle.getInt(INSTANCE_LOADING_START_COLOR);
            loadingEndColor = bundle.getInt(INSTANCE_LOADING_END_COLOR);
            maskColor = bundle.getInt(INSTANCE_MASK_COLOR);
            setDrawMode(DrawMode.valueOf(bundle.getInt(INSTANCE_DRAW_MODE)));
            initView();
            super.onRestoreInstanceState(bundle.getParcelable(INSTANCE_STATE));
            return;
        }
        super.onRestoreInstanceState(state);
    }

}
