package com.rockchip.tutk.activity;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.command.UpdateResponse;
import com.rockchip.tutk.command.UpdateRequest;
import com.rockchip.tutk.utils.CrcUtils;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.RDTAPIs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by waha on 2017/2/22.
 */

public class UpdateActivity extends Activity implements
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "UpdateActivity";
    private final String FILE_FIRMWARE = "Firmware.img";
    private final String PATH_FIRMWARE_IPC = "/mnt/sdcard/" + FILE_FIRMWARE;
    private final String OK = "";
    private final int MSG_UPDATE_UPLOAD = 0;
    private final int MSG_UPDATE_UPDATE = 1;
    private final int MSG_UPDATE_FINISH = 2;
    private final int MSG_CONNECT_ERROR = 3;
    private final int TIME_MAX_READ = 10000;
    private final int MAX_BYTE_SENDED = 1024;
    private final int MAX_READ_TIMEOUT = 10;

    private TextView txtVersion;
    private TextView txtUpdate;
    private TextView txtProgress;
    private LinearLayout layoutProgress;
    private RadioButton rbSdcard;
    private RadioButton rbStorage;
    private Switch swSendFirmware;
    private TextView txtWarn;
    private TUTKDevice mTUTKDevice;
    private int mRdtId = -1;
    private UpdateThread mUpdateThread;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_UPLOAD:
                    txtProgress.setText(R.string.update_firmware_upload);
                    break;
                case MSG_UPDATE_UPDATE:
                    txtProgress.setText(R.string.update_firmware_update);
                    break;
                case MSG_UPDATE_FINISH:
                    layoutProgress.setVisibility(View.GONE);
                    String ss = null;
                    if (null != msg.obj && !TextUtils.isEmpty(msg.obj.toString())) {
                        ss = msg.obj.toString();
                        Toast.makeText(UpdateActivity.this, ss, Toast.LENGTH_SHORT).show();
                        if (getString(R.string.update_success).equals(ss)
                                || getString(R.string.unconnect).equals(ss)) {
                            setResult(Activity.RESULT_OK);
                            finish();
                        }
                    }
                    break;
                case MSG_CONNECT_ERROR:
                    Toast.makeText(UpdateActivity.this, R.string.unconnect, Toast.LENGTH_SHORT).show();
                    setResult(Activity.RESULT_OK);
                    finish();
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
        setContentView(R.layout.activity_update);

        txtVersion = (TextView) findViewById(R.id.txt_version);
        txtUpdate = (TextView) findViewById(R.id.txt_update);
        txtUpdate.setOnClickListener(this);
        txtProgress = (TextView) findViewById(R.id.txt_progress);
        layoutProgress = (LinearLayout) findViewById(R.id.layout_progress);
        rbSdcard = (RadioButton) findViewById(R.id.rb_sdcard);
        rbStorage = (RadioButton) findViewById(R.id.rb_storage);
        txtWarn = (TextView) findViewById(R.id.txt_firmware_warn);
        swSendFirmware = (Switch) findViewById(R.id.sw_send_firmware);
        swSendFirmware.setOnCheckedChangeListener(this);
        swSendFirmware.setChecked(true);
    }


    private void initData() {
        String uid = getIntent().getStringExtra("UID");
        if (uid != null){// && uid.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(uid);
        } else {
            Toast.makeText(this, R.string.error_uid, Toast.LENGTH_SHORT).show();
            finish();
        }
        if (getIntent().hasExtra("version")) {
            txtVersion.setText(getIntent().getStringExtra("version"));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_update: {
                if (layoutProgress.getVisibility() != View.VISIBLE) {
                    layoutProgress.setVisibility(View.VISIBLE);
                    txtProgress.setText(R.string.update_firmware_check);
                    if (null != mUpdateThread) {
                        //mUpdateThread.interrupt();
                        mUpdateThread.setCancel(true);
                    }
                    mUpdateThread = new UpdateThread();
                    mUpdateThread.start();
                }
            }
            break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            rbStorage.setEnabled(true);
            rbSdcard.setEnabled(true);
            txtWarn.setText(
                    String.format(getString(R.string.update_warn_phone), FILE_FIRMWARE));
        } else {
            rbStorage.setEnabled(false);
            rbSdcard.setEnabled(false);
            txtWarn.setText(
                    String.format(getString(R.string.update_warn_ipc), FILE_FIRMWARE));
        }
    }

    @Override
    public void onBackPressed() {
        if (View.VISIBLE == layoutProgress.getVisibility()) {

        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class UpdateThread extends Thread {
        private boolean mCancel;

        public UpdateThread() {
            mCancel = false;
        }

        public void setCancel(boolean cancel) {
            mCancel = cancel;
        }

        @Override
        public void run() {
            Message msg = new Message();
            msg.what = MSG_UPDATE_FINISH;
            msg.obj = update();
            mHandler.sendMessage(msg);
        }

        private String update() {
            if (mRdtId < 0) {
                mRdtId = mTUTKDevice.getSession().getRDTIndex();
                if (mRdtId < 0 && mTUTKDevice.getSession().getSID() > -1) {
                    IOTCAPIs.IOTC_Session_Close(mTUTKDevice.getSession().getSID());
                    mTUTKDevice.getSession().setSID(-1);
                    return getString(R.string.unconnect);
                }
            }
            Log.v("TUTKSession", "mRdtId:" + mRdtId);
            if (!swSendFirmware.isChecked()) {
                return sendUpdate(PATH_FIRMWARE_IPC);
            }
            //send request
            String path = getStoragePath();
            String fileName = FILE_FIRMWARE;
            String ret = sendDownloadRequest(path, FILE_FIRMWARE);
            if (mCancel) {
                return "";
            } else if (OK.equals(ret)) {
                mHandler.sendEmptyMessage(MSG_UPDATE_UPLOAD);
            } else {
                return ret;
            }

            //send file
            UpdateResponse retObj = sendFile(path + fileName);
            if (mCancel) {
                return "";
            } else if (null != retObj
                    && "OK".equals(retObj.getFileCheckStatus())) {
                mHandler.sendEmptyMessage(MSG_UPDATE_UPDATE);
            } else if (null != retObj) {
                return retObj.getFileCheckStatus();
            } else {
                return getString(R.string.update_failed_upload);
            }

            //send update
            return sendUpdate(retObj.getValue());
        }

        private String sendUpdate(String path) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("name", "Updater");
                jsonObject.put("value", path);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String json = jsonObject.toString();
            Log.v(TAG, "send: " + json);
            byte[] data = json.getBytes();
            if (sendMsg(data, data.length) < 1) {
                return getString(R.string.update_failed);
            }
            UpdateResponse retObj = revMsg(UpdateResponse.class);
            if (null == retObj) {
                return getString(R.string.update_failed);
            } else if (retObj.isResult()) {
                return getString(R.string.update_success);
            }
            return retObj.getReason();
        }

        private UpdateResponse sendFile(String fullName) {
            if (TextUtils.isEmpty(fullName)) {
                return null;
            }
            FileInputStream fis = null;
            try {
                File file = new File(fullName);
                fis = new FileInputStream(file);
                byte[] buffer = new byte[MAX_BYTE_SENDED];
                int len = -1;
                int count = 0;
                while ((len = fis.read(buffer)) != -1) {
                    if (sendMsg(buffer, len) < 1) {
                        return null;
                    }
                    count++;
                }
                Log.v(TAG, "send count:" + count);
                //rev sendfile result
                return revMsg(UpdateResponse.class);
            } catch (Exception e) {
                Log.e(TAG, "sendFile happen error: " + fullName);
                e.printStackTrace();
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fis = null;
                }
            }
            return null;
        }

        private String sendDownloadRequest(String path, String fileName) {
            //build request
            File file = new File(path + fileName);
            long crc = CrcUtils.calc_img_crc(path + fileName);
            //long crc = CrcUtils.calc_img_crc_with_api(path + fileName);
            Log.v(TAG, "crc: " + crc);
            if (0 == crc || null == file || !file.exists()) {
                return getString(R.string.update_firmware_unfound);
            }
            UpdateRequest requestObj = new UpdateRequest(fileName, crc, file.length());
            requestObj.setWrite2storage(rbSdcard.isChecked() ? true : false);
            String strRequest = requestObj.Json();
            Log.v(TAG, "send msg: " + strRequest);
            byte[] data = strRequest.getBytes();
            //send request
            if (sendMsg(data, data.length) < 1) {
                return getString(R.string.update_firmware_unupload);
            }
            //rev request result
            UpdateResponse retObj = revMsg(UpdateResponse.class);
            if (null != retObj && retObj.isResult()) {
                return OK;
            } else if (null != retObj) {
                return retObj.getReason();
            }
            return getString(R.string.update_firmware_unupload);
        }

        private <T> T revMsg(Class<T> classOfT) {
            byte[] buff = new byte[1024];
            int ret = RDTAPIs.RDT_ER_TIMEOUT;
            int count = 0;
            while (ret == RDTAPIs.RDT_ER_TIMEOUT) {
                count++;
                ret = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, TIME_MAX_READ);
                if (ret < 0) {
                    Log.e(TAG, "rev read: " + ret);
                    if (count > MAX_READ_TIMEOUT) {
                        break;
                    }
                } else {
                    Log.v(TAG, "rev read: " + ret);
                }
            }
            if (ret < 0) {
                if (mTUTKDevice.getSession().getSID() > -1) {
                    IOTCAPIs.IOTC_Session_Close(mTUTKDevice.getSession().getSID());
                    mTUTKDevice.getSession().setSID(-1);
                }
                if (mRdtId > -1) {
                    RDTAPIs.RDT_Destroy(mRdtId);
                }
                mHandler.sendEmptyMessage(MSG_CONNECT_ERROR);
                return null;
            }
            String strRev = new String(buff, 0, ret);
            strRev = strRev.replace("[", "");
            strRev = strRev.replace("]", "");
            Log.v(TAG, "rev: " + strRev);
            Gson gson = new Gson();
            T rev = null;
            try {
                rev = gson.fromJson(strRev, classOfT);
            } catch (Exception e) {
                Log.e(TAG, "happen error when change gson");
                e.printStackTrace();
            }
            return rev;
        }

        private int sendMsg(byte[] buffer, int buffSize) {
            int ret;
            while (true) {
                if (mCancel) {
                    return 0;
                }
                ret = RDTAPIs.RDT_Write(mRdtId, buffer, buffSize);
                Log.d(TAG, "send byte write: " + ret);
                if (ret < 0) {
                    RDTAPIs.RDT_Destroy(mRdtId);
                    mRdtId = -1;
                    IOTCAPIs.IOTC_Session_Close(mTUTKDevice.getSession().getSID());
                    mHandler.sendEmptyMessage(MSG_CONNECT_ERROR);
                    break;
                }
                break;
            }
            return ret;
        }
    }

    public static String getStoragePath() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return Environment.getExternalStorageDirectory() + "/";
        }
        return null;
    }

}
