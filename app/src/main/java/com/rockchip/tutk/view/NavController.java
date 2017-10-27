package com.rockchip.tutk.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.rockchip.tutk.R;

/**
 * Created by qiujian on 2017/1/19.
 */

public class NavController extends View {
    private int innerColor;
    private int outerColor;
    private final static int INNER_COLOR_DEFAULT = Color.parseColor("#d32f2f");
    private final static int OUTER_COLOR_DEFAULT = Color.parseColor("#f44336");
    private int OUTER_WIDTH_SIZE;
    private int OUTER_HEIGHT_SIZE;
    private int realWidth;//绘图使用的宽
    private int realHeight;//绘图使用的高
    private float innerCenterX;
    private float innerCenterY;
    private float outRadius;
    private float innerRedius;
    private Paint outerPaint;
    private Paint innerPaint;
    private OnNavAndSpeedListener mCallBack = null;
    public interface OnNavAndSpeedListener{
        public void onNavAndSpeed(float nav,float speed);
    }

    public NavController(Context context) {
        this(context,null);
    }

    public NavController(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = getResources().obtainAttributes(attrs,R.styleable.NavController);
        innerColor = ta.getColor(R.styleable.NavController_InnerColor,INNER_COLOR_DEFAULT);
        outerColor = ta.getColor(R.styleable.NavController_OuterColor,OUTER_COLOR_DEFAULT);
        ta.recycle();
        OUTER_WIDTH_SIZE = dip2px(context,125.0f);
        OUTER_HEIGHT_SIZE = dip2px(context,125.0f);
        outerPaint = new Paint();
        innerPaint = new Paint();
        outerPaint.setColor(outerColor);
        outerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        innerPaint.setColor(innerColor);
        innerPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureWidth(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width,height);
    }

    private int measureWidth(int widthMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthVal = MeasureSpec.getSize(widthMeasureSpec);
        //处理三种模式
        if(widthMode==MeasureSpec.EXACTLY){
            return widthVal+getPaddingLeft()+getPaddingRight();
        }else if(widthMode==MeasureSpec.UNSPECIFIED){
            return OUTER_WIDTH_SIZE;
        }else{
            return Math.min(OUTER_WIDTH_SIZE,widthVal);
        }
    }
    private int measureHeight(int heightMeasureSpec) {
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightVal = MeasureSpec.getSize(heightMeasureSpec);
        //处理三种模式
        if(heightMode==MeasureSpec.EXACTLY){
            return heightVal+getPaddingTop()+getPaddingBottom();
        }else if(heightMode==MeasureSpec.UNSPECIFIED){
            return OUTER_HEIGHT_SIZE;
        }else{
            return Math.min(OUTER_HEIGHT_SIZE,heightVal);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        realWidth = w;
        realHeight = h;
        innerCenterX = realWidth/2;
        innerCenterY = realHeight/2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        outRadius = Math.min(Math.min(realWidth/2-getPaddingLeft(),realWidth/2-getPaddingRight()),Math.min(realHeight/2-getPaddingTop(),realHeight/2-getPaddingBottom()));
        //画外部圆
        canvas.drawCircle(realWidth/2,realHeight/2,outRadius,outerPaint);
        //内部圆
        innerRedius = outRadius*0.5f;
        canvas.drawCircle(innerCenterX,innerCenterY,innerRedius,innerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()==MotionEvent.ACTION_DOWN){
            changeInnerCirclePosition(event);

        }
        if(event.getAction()==MotionEvent.ACTION_MOVE){
            changeInnerCirclePosition(event);
            Log.i("TAG","MOVED");
        }
        if(event.getAction()==MotionEvent.ACTION_UP){
            innerCenterX = realWidth/2;
            innerCenterY = realHeight/2;
            invalidate();
        }
        return true;
    }

    private void changeInnerCirclePosition(MotionEvent e) {
        //圆的方程：（x-realWidth/2）^2 +（y - realHeight/2）^2 <= outRadius^2
        //第一步，确定有效的触摸点集
        float X = e.getX();
        float Y = e.getY();
        if(mCallBack!=null){
            mCallBack.onNavAndSpeed(X,Y);
        }
        boolean isPointInOutCircle = Math.pow(X-realWidth/2,2) +Math.pow(Y-realHeight/2,2)<=Math.pow(outRadius,2);
        if(isPointInOutCircle){
            Log.i("TAG","inCircle");
            //两种情况：小圆半径
            boolean isPointInFree = Math.pow(X-realWidth/2,2) +Math.pow(Y-realHeight/2,2)<=Math.pow(outRadius-innerRedius,2);
            if(isPointInFree){
                innerCenterX = X;
                innerCenterY = Y;
            }else{
                //处理限制区域，这部分使用触摸点与中心点与外圆方程交点作为内圆的中心点
                //使用近似三角形来确定这个点
                //求出触摸点，触摸点垂足和中心点构成的直角三角形（pointTri）的直角边长
                float pointTriX = Math.abs(realWidth/2-X);//横边
                float pointTriY = Math.abs(realHeight/2-Y);//竖边
                float pointTriZ = (float) Math.sqrt((Math.pow(pointTriX,2)+Math.pow(pointTriY,2)));
                float TriSin = pointTriY/pointTriZ;
                float TriCos = pointTriX/pointTriZ;
                //求出在圆环上的三角形的两个直角边的长度
                float limitCircleTriY = (outRadius-innerRedius)*TriSin;
                float limitCircleTriX = (outRadius-innerRedius)*TriCos;
                //确定内圆中心点的位置，分四种情况
                if(X>=realWidth/2 && Y>=realHeight/2){
                    innerCenterX = realWidth/2+limitCircleTriX;
                    innerCenterY = realHeight/2+limitCircleTriY;
                }else if(X<realWidth/2 && Y>=realHeight/2){
                    innerCenterX = realWidth/2-limitCircleTriX;
                    innerCenterY= realHeight/2+limitCircleTriY;
                }else if(X>=realWidth/2 && Y<realHeight/2){
                    innerCenterX = realWidth/2+limitCircleTriX;
                    innerCenterY= realHeight/2-limitCircleTriY;
                }else{
                    innerCenterX = realWidth/2-limitCircleTriX;
                    innerCenterY= realHeight/2-limitCircleTriY;
                }
                Log.i("TAG","inLimit");
            }
            invalidate();
        }else{
            Log.i("TAG","notInCircle");
        }
    }
    public void setOnNavAndSpeedListener(OnNavAndSpeedListener listener){
        mCallBack = listener;
    }
    public static int dip2px(Context context, float dpValue){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue*scale +0.5f);
    }
}
