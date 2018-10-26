package com.zzy.popocircleview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;

/**
 * 学习作品来自 https://github.com/hdodenhof/CircleImageView 为了更好的学习自定义控件
 * Description:显示圆形图片，对较大的图片进行缩放，小图直接显示，以圆心为准绘制，可以设置边框，可设置背景颜色和边框颜色
 * Create at 2018年10月25日11:30:18
 * Author：Janumonster@Foxmail.com
 * Version：v1.0.0
 *
 */

public class PopoCircleView extends android.support.v7.widget.AppCompatImageView {

    private static final String TAG = "Popo";

    private static final int DEFULT_BORDER_WIDTH = 0;
    private static final int DEFULT_BORDER_COLOR = Color.TRANSPARENT;
    private static final int DEFULT_BACKGROUND_COLOR = Color.TRANSPARENT;

    //三支画笔
    private Paint bgPaint = new Paint();
    private Paint imgPaint = new Paint();
    private Paint borderPaint = new Paint();

    //图片和边框显示区域
    private RectF mDrawableRect = new RectF();
    private RectF mBorderRect = new RectF();
    //图片半径
    private float mDrawableRadius;
    private float mBorderRadius;
    //默认背景颜色
    private int mBackgroundColor = DEFULT_BACKGROUND_COLOR;
    //默认边框颜色
    private int mBorderColor = DEFULT_BORDER_COLOR;
    //边框宽度
    private int mBorderWidth = DEFULT_BORDER_WIDTH;
    //图片相关数据
    private int mBitmapWidth;
    private int mBitmapHeight;
    //图片
    private Bitmap mBitmap;
    private BitmapShader mBitmapShader;

    private Matrix mMatrix = new Matrix();

    private boolean mBorderOverlay;
    private boolean isReady;
    private boolean isPending;

    public PopoCircleView(Context context) {
        super(context);

        init();
    }

    public PopoCircleView(Context context, AttributeSet attrs) {
        super(context, attrs,0);

        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.PopoCircleView);

        mBorderWidth = typedArray.getDimensionPixelSize(R.styleable.PopoCircleView_popo_border_width,0);
        mBorderColor = typedArray.getColor(R.styleable.PopoCircleView_popo_border_color,Color.TRANSPARENT);
        mBackgroundColor = typedArray.getColor(R.styleable.PopoCircleView_popo_background_color,Color.TRANSPARENT);
        mBorderOverlay = typedArray.getBoolean(R.styleable.PopoCircleView_popo_border_overlay,false);

        Log.d(TAG, "PopoCircleView: mBorderWidth:"+mBorderWidth);

        typedArray.recycle();

        init();
    }

    public PopoCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    private void init() {
        super.setScaleType(ScaleType.CENTER_CROP);
        isReady = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    Rect bounds = new Rect();
                    mBorderRect.roundOut(bounds);
                    outline.setRoundRect(bounds,bounds.width()/2.0f);
                }
            });
        }

        if (isPending){
            updatePaint();
            isPending = false;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (mBitmap == null){
            return;
        }

        if (mBackgroundColor != Color.TRANSPARENT){
            canvas.drawCircle(mDrawableRect.centerX(),mDrawableRect.centerY(), mDrawableRadius,bgPaint);
        }

        canvas.drawCircle(mDrawableRect.centerX(),mDrawableRect.centerY(), mDrawableRadius,imgPaint);
        Log.d(TAG, "onDraw: draw img");
        if (mBorderWidth > 0){
            Log.d(TAG, "onDraw: draw border");

            canvas.drawCircle(mBorderRect.centerX(),mBorderRect.centerY(), mBorderRadius,borderPaint);
        }

    }

    private Bitmap getBitmapFromDrawable(Drawable drawable){
        if (drawable == null){
            return null;
        }

        if (drawable instanceof BitmapDrawable){
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap;

        if (drawable instanceof ColorDrawable){
            bitmap = Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);
        }else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),drawable.getIntrinsicHeight(),Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0,0,canvas.getWidth(),canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    /**
     * 该方法获取控件可用控件的大小，并且你比较晚调用，可以获取到实际值
     * 且你每次调用之后都会有一个重绘请求，所以可以更新画面
     * @return
     */
    private RectF calculateBounds(){
        //通过这种方法来获取控件的宽高，我本人之前是在onMeasure中去获取，并且设置了固定的半径，不好处理，这种方法就很好处理了
        int availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        int availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();

        int sideLength = Math.min(availableHeight,availableWidth);

        int left = getPaddingLeft()+(availableWidth-sideLength)/2;
        int top = getPaddingTop()+(availableHeight-sideLength)/2;

        return new RectF(left,top,left+sideLength,top+sideLength);
    }

    /**
     * 按图片窄边的缩放比例缩放，并结合imageview的scaletype属性设置，画出中间部分画面
     */
    private void updateMatrix(){
        float scale;
        //dx，dy用于调整缩放后的位置，不然图像将被画在左上角
        float dx = 0;
        float dy = 0;
        //清空设置
        mMatrix.set(null);

        if (mBitmapWidth * mDrawableRect.height() > mBitmapHeight * mDrawableRect.width()){
            scale = mDrawableRect.height()/(float) mBitmapHeight;
            dx = (mDrawableRect.width() - mBitmapWidth * scale)*0.5f;
        }else {
            scale = mDrawableRect.width() / (float) mBitmapWidth;
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f;
        }

        mMatrix.setScale(scale,scale);
        mMatrix.postTranslate(dx+0.5f+mDrawableRect.left,dy+0.5f+mDrawableRect.top);

        mBitmapShader.setLocalMatrix(mMatrix);
    }

    private void updatePaint(){

        if (!isReady){
            isPending = true;
            return;
        }


        if (getWidth() == 0 || getHeight() == 0){
            return;
        }

        if (mBitmap == null){
            return;
        }

        mBitmapShader = new BitmapShader(mBitmap,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);

        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(mBackgroundColor);

        imgPaint.setAntiAlias(true);
        imgPaint.setShader(mBitmapShader);

        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(mBorderWidth);
        borderPaint.setColor(mBorderColor);

        mBitmapWidth = mBitmap.getWidth();
        mBitmapHeight = mBitmap.getHeight();

        //获取控件的宽高，只要尺寸改变就要重新调用
        mBorderRect.set(calculateBounds());
        mBorderRadius = Math.min((mBorderRect.width() - mBorderWidth)/2.0f,(mBorderRect.hashCode() - mBorderWidth)/2.0f);
        Log.d(TAG, "updatePaint: mBorderRadius:"+mBorderRadius);
        mDrawableRect.set(mBorderRect);
        if (!mBorderOverlay && mBorderWidth > 0){
            mDrawableRect.inset(mBorderWidth - 1.0f,mBorderWidth - 1.0f);
        }
        mDrawableRadius = Math.min(mDrawableRect.width()/2.0f,mDrawableRect.height()/2.0f);
        Log.d(TAG, "updatePaint: mDrawableRadius:"+mDrawableRadius);

        updateMatrix();
        invalidate();
    }


    private void initializeBitmap(){
        mBitmap = getBitmapFromDrawable(getDrawable());
        updatePaint();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        initializeBitmap();
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        initializeBitmap();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        initializeBitmap();
    }

    @Override
    public void setImageURI(@Nullable Uri uri) {
        super.setImageURI(uri);
        initializeBitmap();
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom);
        updatePaint();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        super.setPaddingRelative(start, top, end, bottom);
        updatePaint();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePaint();
    }

    public int getmBackgroundColor() {
        return mBackgroundColor;
    }

    public void setmBackgroundColor(int mBackgroundColor) {
        if (this.mBackgroundColor == mBackgroundColor){
            return;
        }
        this.mBackgroundColor = mBackgroundColor;
    }

    public int getmBorderColor() {
        return mBorderColor;
    }

    public void setmBorderColor(int mBorderColor) {
        if (this.mBorderColor == mBorderColor){
            return;
        }
        this.mBorderColor = mBorderColor;
    }

    public int getmBorderWidth() {
        return mBorderWidth;
    }

    public void setmBorderWidth(int mBorderWidth) {
        if (this.mBorderWidth == mBorderWidth){
            return;
        }
        this.mBorderWidth = mBorderWidth;
        updatePaint();
    }

    public boolean ismBorderOverlay() {
        return mBorderOverlay;
    }

    public void setmBorderOverlay(boolean mBorderOverlay) {
        if (this.mBorderOverlay == mBorderOverlay){
            return;
        }
        this.mBorderOverlay = mBorderOverlay;
        updatePaint();
    }
}
