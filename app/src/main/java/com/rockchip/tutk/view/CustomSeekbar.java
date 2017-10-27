package com.rockchip.tutk.view;

import android.content.Context;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by waha on 2017/5/11.
 */

public class CustomSeekbar extends AppCompatSeekBar {
    private boolean mCanTrack = false;

    public CustomSeekbar(Context context) {
        super(context);
    }

    public CustomSeekbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomSeekbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mCanTrack) {
            return false;
        }
        return super.onTouchEvent(event);
    }

    public void setCanTrack(boolean canTrack) {
        this.mCanTrack = canTrack;
    }
}
