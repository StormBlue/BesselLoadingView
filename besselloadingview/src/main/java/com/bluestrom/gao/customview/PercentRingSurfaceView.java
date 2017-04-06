//package com.bluestrom.gao.customview;
//
//import android.annotation.TargetApi;
//import android.content.Context;
//import android.content.res.TypedArray;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Color;
//import android.graphics.Matrix;
//import android.graphics.Paint;
//import android.graphics.PorterDuff;
//import android.graphics.PorterDuffXfermode;
//import android.graphics.SweepGradient;
//import android.os.Build;
//import android.util.AttributeSet;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//
///**
// * Created by Gao-Krund on 2016/7/5.
// */
//public class PercentRingSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
//
//    private final String TAG = this.getClass().getSimpleName();
//
//    private final static int ROTATE_PATH = 0;
//
//    private float strokeWidth;
//    private float octagonAngle;
//    private int loadingStartColor;
//    private int loadingEndColor;
//    private int maskColor;
//    private DrawMode drawMode;// 绘制模式
//
//    private float ringOutRadius;// 圆环外层半径
//    private float lengthMargin = 0f;
//
//    private int width, height, lengthDValue, rotateX, rotateY;
//    private Paint pathPaint, bitmapPaint;
//
//    private Canvas mSrcCanvas;
//    private Bitmap mSrcBitmap;
//
//    private Matrix matrix;
//    private SweepGradient sweepGradient;
//
//    private boolean firstDraw;// 是否为第一次绘制，保证背景只绘制一次
//
//    private LoopThread thread;
//
//    private Bitmap bg;
//
//    public PercentRingSurfaceView(Context context) {
//        super(context);
//    }
//
//    public PercentRingSurfaceView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BesselLoading);
//        initByAttributes(mTypedArray);
//        init();
//        mTypedArray.recycle();
//    }
//
//    public PercentRingSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }
//
//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public PercentRingSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }
//
//    private void init() {
//        initView();
//        SurfaceHolder holder = getHolder();
//        holder.addCallback(this); //设置Surface生命周期回调
//        thread = new LoopThread(holder, getContext());
//    }
//
//    private void initView() {
//        firstDraw = true;
//
//        pathPaint = new Paint();
//        pathPaint.setStyle(Paint.Style.STROKE);
//        pathPaint.setStrokeWidth(strokeWidth);
//        pathPaint.setDither(true);
//        pathPaint.setAntiAlias(true);
//        pathPaint.setStrokeCap(Paint.Cap.SQUARE);
//
//        bitmapPaint = new Paint();
//        bitmapPaint.setAntiAlias(true);
//    }
//
//    protected void initByAttributes(TypedArray attributes) {
//
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
//        width = MeasureSpec.getSize(widthMeasureSpec);
//        height = MeasureSpec.getSize(heightMeasureSpec);
//        lengthDValue = (width - height) / 2;
//        rotateX = width / 2;
//        rotateY = height / 2;
//        initParameters();
//    }
//
//    // 初始化控制参数
//    private void initParameters() {
//        // 圆环左端坐标与view最左侧的间距
//        float leftD = lengthDValue > 0f ? lengthDValue : 0f;
//        ringOutRadius = rotateX - leftD;
//    }
//
//    @Override
//    public void surfaceCreated(SurfaceHolder surfaceHolder) {
//        thread.isRunning = true;
//        thread.start();
//
////        // 设置透明
////        setZOrderOnTop(true);
////        getHolder().setFormat(PixelFormat.TRANSLUCENT);
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
//
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//        thread.isRunning = false;
//        try {
//            thread.join();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public float getStrokeWidth() {
//        return strokeWidth;
//    }
//
//    public void setStrokeWidth(float strokeWidth) {
//        this.strokeWidth = strokeWidth;
//    }
//
//    public float getOctagonAngle() {
//        return octagonAngle;
//    }
//
//    public void setOctagonAngle(float octagonAngle) {
//        this.octagonAngle = octagonAngle;
//    }
//
//    public int getLoadingStartColor() {
//        return loadingStartColor;
//    }
//
//    public void setLoadingStartColor(int loadingStartColor) {
//        this.loadingStartColor = loadingStartColor;
//    }
//
//    public int getLoadingEndColor() {
//        return loadingEndColor;
//    }
//
//    public void setLoadingEndColor(int loadingEndColor) {
//        this.loadingEndColor = loadingEndColor;
//    }
//
//    public int getMaskColor() {
//        return maskColor;
//    }
//
//    public void setMaskColor(int maskColor) {
//        this.maskColor = maskColor;
//    }
//
//    public DrawMode getDrawMode() {
//        return drawMode;
//    }
//
//    public void setDrawMode(DrawMode drawMode) {
//        this.drawMode = drawMode;
//    }
//
//    public enum DrawMode {
//
//        STANDARD(0),
//
//        RIVISION(1);
//
//        DrawMode(int ni) {
//            nativeInt = ni;
//        }
//
//        final int nativeInt;
//
//        public static DrawMode valueOf(int ordinal) {
//            if (ordinal < 0 || ordinal >= values().length) {
//                throw new IndexOutOfBoundsException("Invalid ordinal");
//            }
//            return values()[ordinal];
//        }
//    }
//
//    /**
//     * 执行绘制的绘制线程
//     *
//     * @author Administrator
//     */
//    class LoopThread extends Thread {
//
//        SurfaceHolder surfaceHolder;
//        Context context;
//        boolean isRunning;
//        Paint paint;
//
//        public LoopThread(SurfaceHolder surfaceHolder, Context context) {
//
//            this.surfaceHolder = surfaceHolder;
//            this.context = context;
//            isRunning = false;
//
//            paint = new Paint();
//            paint.setColor(Color.YELLOW);
//            paint.setStyle(Paint.Style.STROKE);
//        }
//
//        @Override
//        public void run() {
//
//            Canvas c;
//
//            while (isRunning) {
//
//                synchronized (surfaceHolder) {
//                    c = surfaceHolder.lockCanvas(null);
//
////                    c.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//
//                    drawBackground(c);
//                    firstDraw = false;
//
////                    drawPathBitmap(c);
//
//                }
//                surfaceHolder.unlockCanvasAndPost(c);
//
//            }
//
//        }
//
//        // 绘制有透明区域的背景
//        private void drawBackground(Canvas c) {
//            // 绘制背景
//            if (bg == null) {
//                bg = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                Canvas bgCanvas = new Canvas(bg);
//
//                Paint paintBg = new Paint();
//                paintBg.setStyle(Paint.Style.FILL);
//                Bitmap dstBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                Canvas dstCanvas = new Canvas(dstBitmap);
//                paintBg.setColor(Color.rgb(255, 255, 255));
//                paintBg.setAntiAlias(true);
//                dstCanvas.drawPath(besselPath, paintBg);
//                bgCanvas.drawBitmap(dstBitmap, 0, 0, paintBg);
//
//                Bitmap srcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                Canvas srcCanvas = new Canvas(srcBitmap);
//                paintBg.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
//                srcCanvas.drawColor(maskColor);
//                bgCanvas.drawBitmap(srcBitmap, 0, 0, paintBg);
//            }
//
//            c.drawBitmap(bg, 0, 0, bitmapPaint);
//
//            if (firstDraw) {
//                // 绘制首次显示的贝塞尔曲线
//                mSrcBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                mSrcCanvas = new Canvas(mSrcBitmap);
//                sweepGradient = new SweepGradient(rotateX, rotateY, Color.WHITE, Color.BLUE);
//                matrix = new Matrix();
//                pathPaint.setShader(sweepGradient);
//            }
//            drawPathBitmap(c);
//        }
//
//        private void drawPathBitmap(Canvas c) {
//            matrix.setRotate(rotateAngle - 90, rotateX, rotateY);
//            sweepGradient.setLocalMatrix(matrix);
//            mSrcCanvas.drawPath(besselPath, pathPaint);
//            c.drawBitmap(mSrcBitmap, 0, 0, bitmapPaint);
//        }
//
//    }
//}
