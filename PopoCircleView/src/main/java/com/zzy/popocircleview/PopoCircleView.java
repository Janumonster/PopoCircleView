package com.zzy.popocircleview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

/**
 *
 * Description:显示圆形图片，对较大的图片进行缩放，小图直接显示，以圆心为准绘制，可以设置边框，可设置背景颜色和边框颜色
 * Create at 2018年10月25日11:30:18
 * Author：Janumonster@Foxmail.com
 * Version：v1.0.0
 *
 */

public class PopoCircleView extends android.support.v7.widget.AppCompatImageView {

    private static final String TAG = "Popo";

    //三支画笔
    private Paint bgPaint = new Paint();
    private Paint imgPaint = new Paint();
    private Paint borderPaint = new Paint();
    //图片中心点
    private int centerX;
    private int centerY;
    //图片半径
    private float mRadius = 100f;
    //默认背景颜色
    private int mBackgroundColor = Color.TRANSPARENT;
    //默认边框颜色
    private int mBorderColor = Color.WHITE;
    //边框宽度
    private int strokeWidth = 0;
    //图片相关数据
    private int drawWidth;
    private int drawHeight;
    private int drawSize;

    //图片
    private Drawable mDrawable;
    private Bitmap mBitmap;

    private Context mContext;
    private AttributeSet mAttributeSet;

    public PopoCircleView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public PopoCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mAttributeSet = attrs;
        init();
    }

    public PopoCircleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        mAttributeSet = attrs;
        init();
    }

    private void init() {
        if (mAttributeSet != null){
            TypedArray ta = mContext.obtainStyledAttributes(mAttributeSet,R.styleable.PopoCircleView);
            int count = ta.getIndexCount();
            for (int i = 0 ;i < count;i ++){
                int itemId = ta.getIndex(i);
                if (itemId == R.styleable.PopoCircleView_popo_radius) {
                    mRadius = ta.getInt(itemId, 0);
                } else if (itemId == R.styleable.PopoCircleView_popo_border_width) {
                    strokeWidth = ta.getInt(itemId, 0);
                } else if (itemId == R.styleable.PopoCircleView_popo_background_color) {
                    mBackgroundColor = ta.getColor(itemId, 0);
                } else if (itemId == R.styleable.PopoCircleView_popo_border_color) {
                    mBorderColor = ta.getColor(itemId, 0);
                }
            }
            ta.recycle();
        }

        bgPaint.setAntiAlias(true);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setStrokeWidth(strokeWidth);
        bgPaint.setColor(mBackgroundColor);

        imgPaint.setAntiAlias(true);
        imgPaint.setStyle(Paint.Style.FILL);
        imgPaint.setStrokeWidth(strokeWidth);

        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokeWidth);
        borderPaint.setColor(mBorderColor);

        mDrawable = getDrawable();

        if (mDrawable != null){
            BitmapDrawable bitmapDrawable = (BitmapDrawable) mDrawable;
            mBitmap = bitmapDrawable.getBitmap();

            drawWidth = mBitmap.getWidth();
            drawHeight = mBitmap.getHeight();

            drawSize = Math.max(drawWidth,drawHeight);

            Matrix matrix = new Matrix();
            matrix.setScale(2*mRadius/drawWidth,2*mRadius/drawHeight);

            Bitmap bitmap1 = Bitmap.createBitmap(mBitmap,0,0,drawWidth,drawHeight,matrix,true);

            Shader shader = new BitmapShader(bitmap1,Shader.TileMode.CLAMP,Shader.TileMode.CLAMP);
            imgPaint.setShader(shader);
        }else {
            Log.d(TAG, "init:Drawable is null");
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {

        Log.d(TAG, "onDraw: radius:"+mRadius);
        //画背景
        canvas.drawCircle(centerX,centerY,mRadius,bgPaint);

        //画图片
        if (mDrawable != null){
            if (drawSize <= 2 * mRadius){
                canvas.drawBitmap(mBitmap,centerX-drawWidth/2,centerY-drawHeight/2,imgPaint);
            }else {
                canvas.drawCircle(centerX,centerY,mRadius,imgPaint);
            }
        }else {
            Log.d(TAG, "onDraw: Drawable is null");
        }

        //画边框
        canvas.drawCircle(centerX,centerY,mRadius-strokeWidth/2,borderPaint);


    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.d(TAG, "onMeasure: width:"+width+"  height:"+height);

        centerX = width/2;
        centerY = height/2;
        Log.d(TAG, "onMeasure: centerX:"+centerX+"  centerY:"+centerY);

        int size = (int) (2*mRadius);

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        layoutParams.width = size;
        layoutParams.height = size;
        setLayoutParams(layoutParams);
    }

    public float getmRadius() {
        return mRadius;
    }

    public void setmRadius(float mRadius) {
        this.mRadius = mRadius;
    }

    public int getmBackgroundColor() {
        return mBackgroundColor;
    }

    public void setmBackgroundColor(int mBackgroundColor) {
        this.mBackgroundColor = mBackgroundColor;
    }

    public int getmBorderColor() {
        return mBorderColor;
    }

    public void setmBorderColor(int mBorderColor) {
        this.mBorderColor = mBorderColor;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public Drawable getmDrawable() {
        return mDrawable;
    }

    public Bitmap getmBitmap() {
        return mBitmap;
    }
}
