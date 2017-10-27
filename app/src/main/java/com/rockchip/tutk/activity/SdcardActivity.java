package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.tutk.DeviceInfo;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.command.FormatCommand;
import com.rockchip.tutk.model.SdcardModel;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.SharedPreference;
import com.tutk.IOTC.AVAPIs;

import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;

/**
 * Created by waha on 2017/3/9.
 */

public class SdcardActivity extends Activity implements
        View.OnClickListener, TUTKDevice.DeviceInfoChangeCallback,
        TUTKDevice.OnTutkError {
    private static final String TAG = "SdcardActivity";

    private TextView txtName, txtWarn, txtCancel, txtFormat;
    private ProgressBar pbWait;
    private TUTKDevice mTUTKDevice;
    private SdcardThread mThread;
    private SdcardModel mModel;
    private boolean mFinishFlag = false;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgDatas.MSG_FORMAT_TIMEOUT:
                    if (!isFinishing()) {
                        Toast.makeText(SdcardActivity.this, R.string.network_failed,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
                case MsgDatas.MSG_FORMAT_SUCCESS:
                    if (!isFinishing()) {
                        Toast.makeText(SdcardActivity.this, R.string.format_success,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
                case MsgDatas.MSG_FORMAT_FAILED:
                    if (!isFinishing()) {
                        txtCancel.setEnabled(false);
                        Toast.makeText(SdcardActivity.this, R.string.format_failed,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        initData();
    }

    private void initUI() {
        setContentView(R.layout.activity_sdcard);

        txtCancel = (TextView) findViewById(R.id.txt_cancel);
        txtFormat = (TextView) findViewById(R.id.txt_format);
        txtName = (TextView) findViewById(R.id.txt_name);
        txtWarn = (TextView) findViewById(R.id.txt_warn);
        txtCancel.setOnClickListener(this);
        txtFormat.setOnClickListener(this);
        pbWait = (ProgressBar) findViewById(R.id.pb_wait);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        mFinishFlag = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing() && mFinishFlag) {
            Log.w(TAG, "finish activity onpause");
            finish();
            mFinishFlag = false;
        } else {
            Log.v(TAG, "onpause");
        }
    }

    private void initData() {
        mModel = (SdcardModel) getIntent().getSerializableExtra("sdcard");
        String uid = mModel.getUid();
        txtName.setText(SharedPreference.getString(uid, uid));
        String desc = mModel.getDesc();
        if (TextUtils.isEmpty(desc)) {
            txtWarn.setText(R.string.sdcard_error);
        } else {
            txtWarn.setText(mModel.getDesc());
        }
        if (uid != null){// && uid.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(uid);
            if (null == mTUTKDevice) {
                Log.e(TAG, "mTutkDevice is null");
                //Toast.makeText(this, R.string.unconnect, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                mTUTKDevice.addDeviceInfoChangeCallback(this);
                mTUTKDevice.addOnTutkErrorListener(this);
            }
        } else {
            //Toast.makeText(this, R.string.error_uid, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "mTutkDevice uid is error");
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_cancel:
                finish();
                break;
            case R.id.txt_format: {
                if (getString(R.string.format).equals(txtFormat.getTag())) {
                    txtFormat.setTag(getString(R.string.confirm));
                    txtFormat.setText(R.string.confirm);
                    txtWarn.setText(R.string.format_summary);
                } else {
                    txtFormat.setEnabled(false);
                    txtCancel.setEnabled(false);
                    pbWait.setVisibility(View.VISIBLE);
                    txtFormat.setTextColor(Color.DKGRAY);
                    txtCancel.setTextColor(Color.DKGRAY);
                    txtWarn.setText(R.string.formating);
                    if (null != mThread) {
                        //mUpdateThread.interrupt();
                        mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
                        mThread.setCancel(true);
                    }
                    mThread = new SdcardThread();
                    mThread.start();
                }
            }
            break;
        }
    }

    @Override
    public void onBackPressed() {
        if (null != mThread) {

        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
        if (null != mTUTKDevice) {
            mTUTKDevice.removeDeviceInfoChangeCallback(this);
            mTUTKDevice.removeOnTutkErrorListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onDeviceInfoChange(DeviceInfo deviceInfo, String name) {

    }

    @Override
    public void onSdcardFormat(boolean result) {
        mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
        mHandler.sendEmptyMessage(result ? MsgDatas.MSG_FORMAT_SUCCESS : MsgDatas.MSG_FORMAT_FAILED);
    }

    @Override
    public void onError(int code) {
        Log.e(TAG, "finish error when rev onError");
        finish();
    }

    private class SdcardThread extends Thread {
        private boolean mCancel;

        public SdcardThread() {
            mCancel = false;
        }

        public void setCancel(boolean cancel) {
            mCancel = cancel;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
            mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_FORMAT_TIMEOUT,
                    MsgDatas.FORMAT_TIMEOUT);
            FormatCommand formatCommand = new FormatCommand(true);
            int write = -1;
            try {
                byte[] bytes = formatCommand.Json().getBytes();
                write = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(),
                        RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
            } catch (Exception e) {
                Log.e(TAG, "sendFormat happen error");
                e.printStackTrace();
                write = -1;
            }
            if (write != 0) {
                mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
                mHandler.sendEmptyMessage(MsgDatas.MSG_FORMAT_TIMEOUT);
            }
        }
    }

}
