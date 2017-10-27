package com.rockchip.tutk.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by qiujian on 2017/1/19.
 */

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private SurfaceHolder sfh;
    private Paint paint;
    private Thread th;
    private boolean flag;
    private Canvas canvas;
    private int screenW, screenH;
    //定义两个圆形的中心点坐标与半径
    private float smallCenterX = 120, smallCenterY = 120, smallCenterR = 20;
    private float BigCenterX = 120, BigCenterY = 120, BigCenterR = 40;

    /**
     * SurfaceView初始化函数
     */
    public CameraSurfaceView(Context context) {
        super(context);
        sfh = this.getHolder();
        sfh.addCallback(this);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        setFocusable(true);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        sfh = this.getHolder();
        sfh.addCallback(this);
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        setFocusable(true);
    }

    /**
     * SurfaceView视图创建，响应此函数
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        screenW = this.getWidth();
        screenH = this.getHeight();
        flag = true;
        //实例线程
        th = new Thread(this);
        //启动线程
        th.start();
    }

    /**
     * 游戏绘图
     */
    public void myDraw() {
        try {
            canvas = sfh.lockCanvas();
            if (canvas != null) {
                canvas.drawColor(Color.BLACK);
                //绘制大圆
                paint.setAlpha(0x77);
                canvas.drawCircle(BigCenterX, BigCenterY, BigCenterR, paint);
                //绘制小圆
                canvas.drawCircle(smallCenterX, smallCenterY, smallCenterR, paint);

            }
        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            if (canvas != null)
                sfh.unlockCanvasAndPost(canvas);
        }
    }

    /**
     * 触屏事件监听
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //当用户手指抬起，应该恢复小圆到初始位置
        if (event.getAction() == MotionEvent.ACTION_UP) {
            smallCenterX = BigCenterX;
            smallCenterY = BigCenterY;
        } else {
            int pointX = (int) event.getX();
            int pointY = (int) event.getY();
            //判断用户点击的位置是否在大圆内
            if (Math.sqrt(Math.pow((BigCenterX - (int) event.getX()), 2) + Math.pow((BigCenterY - (int) event.getY()), 2)) <= BigCenterR) {
                //让小圆跟随用户触点位置移动
                smallCenterX = pointX;
                smallCenterY = pointY;
            } else {
                setSmallCircleXY(BigCenterX, BigCenterY, BigCenterR, getRad(BigCenterX, BigCenterY, pointX, pointY));
            }
        }
        return true;
    }

    /**
     * 小圆针对于大圆做圆周运动时，设置小圆中心点的坐标位置
     * @param centerX
     *            围绕的圆形(大圆)中心点X坐标
     * @param centerY
     *            围绕的圆形(大圆)中心点Y坐标
     * @param R
     * 			     围绕的圆形(大圆)半径
     * @param rad
     *            旋转的弧度
     */
    public void setSmallCircleXY(float centerX, float centerY, float R, double rad) {
        //获取圆周运动的X坐标
        smallCenterX = (float) (R * Math.cos(rad)) + centerX;
        //获取圆周运动的Y坐标
        smallCenterY = (float) (R * Math.sin(rad)) + centerY;
    }

    /**
     * 按键事件监听
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 得到两点之间的弧度
     * @param px1    第一个点的X坐标
     * @param py1    第一个点的Y坐标
     * @param px2    第二个点的X坐标
     * @param py2    第二个点的Y坐标
     * @return
     */
    public double getRad(float px1, float py1, float px2, float py2) {
        //得到两点X的距离
        float x = px2 - px1;
        //得到两点Y的距离
        float y = py1 - py2;
        //算出斜边长
        float Hypotenuse = (float) Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        //得到这个角度的余弦值（通过三角函数中的定理 ：邻边/斜边=角度余弦值）
        float cosAngle = x / Hypotenuse;
        //通过反余弦定理获取到其角度的弧度
        float rad = (float) Math.acos(cosAngle);
        //当触屏的位置Y坐标<摇杆的Y坐标我们要取反值-0~-180
        if (py2 < py1) {
            rad = -rad;
        }
        return rad;
    }

    /**
     * 游戏逻辑
     */
    private void logic() {
    }

    @Override
    public void run() {
        while (flag) {
            long start = System.currentTimeMillis();
            myDraw();
            logic();
            long end = System.currentTimeMillis();
            try {
                if (end - start < 50) {
                    Thread.sleep(50 - (end - start));
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * SurfaceView视图状态发生改变，响应此函数
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * SurfaceView视图消亡时，响应此函数
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        flag = false;
    }
}
