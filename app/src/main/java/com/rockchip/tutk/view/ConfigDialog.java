package com.rockchip.tutk.view;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.rockchip.tutk.DeviceInfo;
import com.rockchip.tutk.PlayConfig;
import com.rockchip.tutk.R;

/**
 * Created by waha on 2017/3/16.
 */

public class ConfigDialog extends Dialog implements
        View.OnClickListener,
        SeekBar.OnSeekBarChangeListener {
    private Context mContext;
    private SeekBar sbBright;
    private SeekBar sbVolume;
    private SeekBar sbContrast;
    private SeekBar sbSaturation;
    private SeekBar sbSharpness;
    private LinearLayout layoutInfo;
    private LinearLayout layoutWait;
    private TextView txtClose;
    private PlayConfig mInfo;
    private OnConfigDialogListener mListener;

    public ConfigDialog(@NonNull Context context, DeviceInfo info,
                        OnConfigDialogListener listener) {
        super(context, R.style.LoadingView);
        mContext = context;
        mListener = listener;
        mInfo = info.getPlayConfig();
        initUI();
    }

    private void initUI() {
        setContentView(R.layout.dialog_config);
        sbBright = (SeekBar) findViewById(R.id.sb_bright);
        sbBright.setOnSeekBarChangeListener(this);
        sbVolume = (SeekBar) findViewById(R.id.sb_volume);
        sbVolume.setOnSeekBarChangeListener(this);
        sbContrast = (SeekBar) findViewById(R.id.sb_contrast);
        sbContrast.setOnSeekBarChangeListener(this);
        sbSaturation = (SeekBar) findViewById(R.id.sb_saturation);
        sbSaturation.setOnSeekBarChangeListener(this);
        sbSharpness = (SeekBar) findViewById(R.id.sb_sharpness);
        sbSharpness.setOnSeekBarChangeListener(this);
        txtClose = (TextView) findViewById(R.id.txt_close);
        txtClose.setOnClickListener(this);
        layoutWait = (LinearLayout) findViewById(R.id.layout_wait);
        layoutInfo = (LinearLayout) findViewById(R.id.layout_info);

        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.alpha = 0.85f;
        getWindow().setAttributes(attributes);
        setCanceledOnTouchOutside(false);
        setCancelable(false);
    }

    private void initData() {
        sbBright.setMax(100);
        sbVolume.setMax(100);
        sbContrast.setMax(100);
        sbSaturation.setMax(100);
        sbSharpness.setMax(100);
        sbBright.setProgress(mInfo.getPlayBrightness());
        sbVolume.setProgress(mInfo.getPlayVolume());
        sbContrast.setProgress(mInfo.getPlayContrast());
        sbSaturation.setProgress(mInfo.getPlaySaturation());
        sbSharpness.setProgress(mInfo.getPlaySharpness());
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.sb_bright:
                if (null != mListener) {
                    mListener.brightChange(sbBright.getProgress());
                }
                break;
            case R.id.sb_volume:
                if (null != mListener) {
                    mListener.volumeChange(sbVolume.getProgress());
                }
                break;
            case R.id.sb_contrast:
                if (null != mListener) {
                    mListener.contrastChange(sbContrast.getProgress());
                }
                break;
            case R.id.sb_saturation:
                if (null != mListener) {
                    mListener.saturationChange(sbSaturation.getProgress());
                }
                break;
            case R.id.sb_sharpness:
                if (null != mListener) {
                    mListener.sharpnessChange(sbSharpness.getProgress());
                }
                break;
        }
    }

    public void loadFinish() {
        initData();
        layoutWait.setVisibility(View.GONE);
        layoutInfo.setVisibility(View.VISIBLE);
    }

    public void refreshBright() {
        if (null != sbBright && null != mInfo) {
            sbBright.setProgress(mInfo.getPlayBrightness());
        }
    }

    public void refreshVolume() {
        if (null != sbVolume && null != mInfo) {
            sbVolume.setProgress(mInfo.getPlayVolume());
        }
    }

    public void refreshContrast() {
        if (null != sbContrast && null != mInfo) {
            sbContrast.setProgress(mInfo.getPlayContrast());
        }
    }

    public void refreshSaturation() {
        if (null != sbSaturation && null != mInfo) {
            sbSaturation.setProgress(mInfo.getPlaySaturation());
        }
    }

    public void refreshSharpness() {
        if (null != sbSharpness && null != mInfo) {
            sbSharpness.setProgress(mInfo.getPlaySharpness());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_close:
                ConfigDialog.this.cancel();
                break;
        }
    }

    public interface OnConfigDialogListener {
        void brightChange(int value);

        void volumeChange(int value);

        void contrastChange(int value);

        void saturationChange(int value);

        void sharpnessChange(int value);
    }
}
