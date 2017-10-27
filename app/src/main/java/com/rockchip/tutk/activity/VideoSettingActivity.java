package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.rockchip.tutk.DeviceInfo;
import com.rockchip.tutk.EncoderParameter;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.command.CommandBase;
import com.rockchip.tutk.command.ExposureCommand;
import com.rockchip.tutk.command.FileTransferCommand;
import com.rockchip.tutk.command.FormatCommand;
import com.rockchip.tutk.command.GetEncoderParameters;
import com.rockchip.tutk.command.IDCCommand;
import com.rockchip.tutk.command.MovingCommand;
import com.rockchip.tutk.command.MovingListCommand;
import com.rockchip.tutk.command.ODTCommand;
import com.rockchip.tutk.command.ODTLevelCommand;
import com.rockchip.tutk.command.RebootCommand;
import com.rockchip.tutk.command.RecordTimeCommand;
import com.rockchip.tutk.command.SetEncoderParameters;
import com.rockchip.tutk.command.SoundCommand;
import com.rockchip.tutk.command.ThreeDNRCommand;
import com.rockchip.tutk.command.WaterMarkCommand;
import com.rockchip.tutk.command.WhiteBalanceCommand;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.MsgManager;
import com.rockchip.tutk.utils.SharedPreference;
import com.rockchip.tutk.view.LoadingView;
import com.tutk.IOTC.AVAPIs;

import org.json.JSONException;
import org.json.JSONObject;

import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;

public class VideoSettingActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener, DialogInterface.OnClickListener,
        TUTKDevice.DeviceInfoChangeCallback, TUTKDevice.OnTutkError,
        TUTKDevice.EncoderChangeCallback {
    String mUID;
    TUTKDevice mTUTKDevice;
    private LoadingView mLoadingView;
    public final String TAG = "VideoSettingActivity";
    public final String DEVICE_NAME_KEY = "device_name";
    public final String Resolution_KEY = "resolution_list";
    public final String Moving_KEY = "moving_switch";
    public final String Water_mark_KEY = "water_mark_switch";
    public final String Exposure_KEY = "exposure_list";
    public final String Record_time_KEY = "record_time_list";
    public final String White_balance_KEY = "white_balance_list";
    public final String Format_KEY = "format";
    public final String ThreeDNR_switch_KEY = "3DNR_switch";
    public final String ThreeDNRList_KEY = "3DNR_list";
    public final String IDC_switch_KEY = "IDC_switch";
    public final String Sound_switch_KEY = "sound_switch";
    public final String Update_KEY = "update";
    public final String MoveList_KEY = "move_list";
    public final String Encoder_settings_KEY = "encoder_settings";
    public final String ODT_switch_KEY = "ODT_switch";
    public final String ODT_level_KEY = "ODT_level";
    public final String Device_reboot_KEY = "device_reboot";
    public final String Device_recovery_KEY = "device_recovery";
    public final String Sdcard_KEY = "sdcardInfo";
    public final String DEVICE_PASSWORD_KEY = "device_password";
    Preference device_name, format, update, encoder_settings, device_reboot, sdcard, device_recovery, device_password;
    SwitchPreference water_mark_switch, moving_switch, threeDNR_switch,
            ODT_switch, IDC_switch, sound_switch;
    ListPreference resolution_list, record_time_list, exposure_list,
            white_balance_list, move_list, threeDNR_list, ODT_level;
    AlertDialog mFormatDialog, mRebootDialog, mRecoveryDialog;

    private String mDeviceName;
    private Object mLock = new Object();
    private long mSendMsgTime;
    private boolean mIsResetPwd;

    public static final int REQUEST_UPDATE = 1;
    public static final int REQUEST_RECODE = 2;
    public static final int REQUEST_ENCORDER = 3;
    public static final int REQUEST_PASSWORD = 4;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgDatas.MSG_GET_SETTINGS_TIMEOUT:
                    Toast.makeText(VideoSettingActivity.this,
                            R.string.txt_get_info_failed, Toast.LENGTH_SHORT).show();
                    getPreferenceManager().getSharedPreferences()
                            .unregisterOnSharedPreferenceChangeListener(VideoSettingActivity.this);
                    finish();
                    break;
                case MsgDatas.MSG_UPDATE_SETTINGS:
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    if (null != msg.obj && msg.obj instanceof Integer) {
                        onError((Integer) msg.obj);
                    } else {
                        Toast.makeText(VideoSettingActivity.this, R.string.network_failed, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
                case MsgDatas.MSG_FORMAT_TIMEOUT:
                case MsgDatas.MSG_RECOVERY_TIMEOUT:
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    Toast.makeText(VideoSettingActivity.this,
                            R.string.network_failed, Toast.LENGTH_SHORT).show();
                    break;
                case MsgDatas.MSG_FORMAT_SUCCESS:
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    Toast.makeText(VideoSettingActivity.this,
                            R.string.format_success, Toast.LENGTH_SHORT).show();
                    break;
                case MsgDatas.MSG_FORMAT_FAILED:
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    //odt delayed also used it

                    //Toast.makeText(VideoSettingActivity.this,
                    //        R.string.format_failed, Toast.LENGTH_SHORT).show();
                    break;
                case MsgDatas.MSG_GET_SDCARDINFO_TIMEOUT:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_setting);
        device_name = findPreference(DEVICE_NAME_KEY);
        white_balance_list = (ListPreference) findPreference(White_balance_KEY);
        record_time_list = (ListPreference) findPreference(Record_time_KEY);
        resolution_list = (ListPreference) findPreference(Resolution_KEY);
        water_mark_switch = (SwitchPreference) findPreference(Water_mark_KEY);
        moving_switch = (SwitchPreference) findPreference(Moving_KEY);
        move_list = (ListPreference) findPreference(MoveList_KEY);
        exposure_list = (ListPreference) findPreference(Exposure_KEY);
        format = findPreference(Format_KEY);
        update = findPreference(Update_KEY);
        device_reboot = findPreference(Device_reboot_KEY);
        device_reboot.setOnPreferenceClickListener(this);
        device_recovery = findPreference(Device_recovery_KEY);
        device_recovery.setOnPreferenceClickListener(this);
        format.setOnPreferenceClickListener(this);
        update.setOnPreferenceClickListener(this);

        encoder_settings = findPreference(Encoder_settings_KEY);
        encoder_settings.setOnPreferenceClickListener(this);
        threeDNR_switch = (SwitchPreference) findPreference(ThreeDNR_switch_KEY);
        threeDNR_list = (ListPreference) findPreference(ThreeDNRList_KEY);
        IDC_switch = (SwitchPreference) findPreference(IDC_switch_KEY);
        sound_switch = (SwitchPreference) findPreference(Sound_switch_KEY);
        ODT_switch = (SwitchPreference) findPreference(ODT_switch_KEY);
        ODT_level = (ListPreference) findPreference(ODT_level_KEY);
        sdcard = findPreference(Sdcard_KEY);
        sdcard.setOnPreferenceClickListener(this);
        device_password = findPreference(DEVICE_PASSWORD_KEY);
        device_password.setOnPreferenceClickListener(this);
        init();
    }

    private void init() {
        Intent intent = getIntent();
        if (intent == null) return;

        mUID = intent.getStringExtra(TUTKManager.TUTK_UID);
        if (mUID != null){// && mUID.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(mUID);
        }

        mDeviceName = intent.getStringExtra(TUTKManager.TUTK_DEVICE_NAME);
        if (mDeviceName != null && mDeviceName.length() > 0) {
            device_name.setSummary(mDeviceName);
        }
    }

    @Override
    protected void onResume() {
        mTUTKDevice.addDeviceInfoChangeCallback(this);
        mTUTKDevice.addOnTutkErrorListener(this);
        mTUTKDevice.addEncoderChangeCallbacks(this);
        DeviceInfo deviceInfo = mTUTKDevice.getDeviceInfo();
        if (null != deviceInfo && TextUtils.isEmpty(deviceInfo.getmResolution())) {
            if (null == mLoadingView || !mLoadingView.isShowing()) {
                mLoadingView = new LoadingView(this, getResources().getString(R.string.txt_getting_info));
                mLoadingView.show();
            }
            if (null != mHandler) {
                mHandler.removeMessages(MsgDatas.MSG_GET_SETTINGS_TIMEOUT);
                mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_GET_SETTINGS_TIMEOUT, 6000);
            }
        }
        resetUI(mTUTKDevice);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mTUTKDevice.removeDeviceInfoChangeCallback(this);
        mTUTKDevice.removeOnTutkErrorListener(this);
        mTUTKDevice.removeEncoderChangeCallbacks(this);
        if (null != mLoadingView && mLoadingView.isShowing()) {
            mLoadingView.cancel();
            mLoadingView = null;
        }
        if (null != mHandler) {
            mHandler.removeMessages(MsgDatas.MSG_GET_SETTINGS_TIMEOUT);
        }
        super.onPause();
    }

    private void resetUI(TUTKDevice tutkDevice) {

        DeviceInfo deviceInfo = tutkDevice.getDeviceInfo();

        water_mark_switch.setChecked(deviceInfo.ismWaterMark());

        ODT_switch.setChecked(deviceInfo.isOdt());
        ODT_level.setSummary(String.valueOf(deviceInfo.getOdtLevel()));
        ODT_level.setValue(String.valueOf(deviceInfo.getOdtLevel()));
        moving_switch.setChecked(deviceInfo.ismMoving());
        move_list.setSummary(String.valueOf(deviceInfo.getMoveSensitivity()));
        move_list.setValue(String.valueOf(deviceInfo.getMoveSensitivity()));
        if (deviceInfo.ismMoving()) {
            ODT_switch.setEnabled(true);
            if (deviceInfo.isOdt()) {
                ODT_level.setEnabled(true);
            } else {
                ODT_level.setEnabled(false);
            }
        } else {
            ODT_switch.setEnabled(false);
            ODT_level.setEnabled(false);
        }

        white_balance_list.setSummary(deviceInfo.getmWhiteBalance());
        white_balance_list.setValue(deviceInfo.getmWhiteBalance());

        record_time_list.setSummary(String.valueOf(deviceInfo.getmRecordTime()));
        record_time_list.setValue(String.valueOf(deviceInfo.getmRecordTime()));

        reflushResolution();

        exposure_list.setSummary(String.valueOf(deviceInfo.getmExposure()));
        exposure_list.setValue(String.valueOf(deviceInfo.getmExposure()));

        threeDNR_switch.setChecked(deviceInfo.is3DNR());
        threeDNR_list.setSummary(String.valueOf(deviceInfo.get3DNRLevel()));
        threeDNR_list.setValue(String.valueOf(deviceInfo.get3DNRLevel()));

        IDC_switch.setChecked(deviceInfo.isIDC());
        sound_switch.setChecked(deviceInfo.isSound());
        String sdcardDesc = deviceInfo.getSdcardDesc();
        if (TextUtils.isEmpty(sdcardDesc)) {
            sdcard.setTitle(getResources().getString(R.string.txt_nosdcard));
        } else {
            sdcard.setTitle("sdcard(" + sdcardDesc + ")");
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mTUTKDevice.cancelThumbsThread();
        mTUTKDevice.close();
        mHandler.removeMessages(MsgDatas.MSG_UPDATE_SETTINGS);
        mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
        mHandler.removeMessages(MsgDatas.MSG_RECOVERY_TIMEOUT);
        mHandler.removeMessages(MsgDatas.MSG_GET_SETTINGS_TIMEOUT);
        mHandler.removeMessages(MsgDatas.MSG_GET_SDCARDINFO_TIMEOUT);
        mHandler.removeMessages(MsgDatas.MSG_FORMAT_FAILED);
        if (null != mLoadingView && mLoadingView.isShowing()) {
            mLoadingView.cancel();
        }
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mTUTKDevice == null || mTUTKDevice.getSession() == null) {
            Toast.makeText(getApplicationContext(), R.string.device_view_status, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        CommandBase command = null;
        if (Resolution_KEY.equals(key)) {
            changeResolution(sharedPreferences.getString(key, null));
            return;
        } else if (Moving_KEY.equals(key)) {
            Boolean value = sharedPreferences.getBoolean(key, false);
            command = new MovingCommand(value);
            //moving_switch.setSummary(String.valueOf(value));
            if (value) {
                ODT_switch.setEnabled(true);
                if (ODT_switch.isChecked()) {
                    ODT_level.setEnabled(true);
                } else {
                    ODT_level.setEnabled(false);
                }
            } else {
                ODT_switch.setEnabled(false);
                ODT_level.setEnabled(false);
            }
        } else if (Water_mark_KEY.equals(key)) {
            Boolean value = sharedPreferences.getBoolean(key, false);
            command = new WaterMarkCommand(value);
            //water_mark_switch.setSummary(String.valueOf(value));
        } else if (Exposure_KEY.equals(key)) {
            String value = sharedPreferences.getString(key, null);
            if (value != null) {
                command = new ExposureCommand(value);
            }
            exposure_list.setSummary(value);
        } else if (Record_time_KEY.equals(key)) {
            String value = sharedPreferences.getString(key, null);
            if (value != null) {
                command = new RecordTimeCommand(value);
            }
            record_time_list.setSummary(value);
        } else if (White_balance_KEY.equals(key)) {
            String value = sharedPreferences.getString(key, null);
            if (value != null) {
                command = new WhiteBalanceCommand(value);
            }
            white_balance_list.setSummary(value);
        } else if (ThreeDNR_switch_KEY.equals(key)) {
            Boolean value = sharedPreferences.getBoolean(key, false);
            command = new ThreeDNRCommand(value);
        } else if (IDC_switch_KEY.equals(key)) {
            Boolean value = sharedPreferences.getBoolean(key, false);
            command = new IDCCommand(value);
        } else if (Sound_switch_KEY.equals(key)) {
            Boolean value = sharedPreferences.getBoolean(key, false);
            command = new SoundCommand(value);
        } else if (DEVICE_NAME_KEY.equals(key)) {
            String result = ((EditTextPreference) device_name).getText();
            device_name.setSummary(result);
            mTUTKDevice.getDeviceInfo().setDeviceName(result);
            SharedPreference.putString(mUID, result);
        } else if (ThreeDNRList_KEY.equals(key)) {
            String value = sharedPreferences.getString(key, null);
            if (value != null) {
                //command=new ThreeDNRLevelCommand(value);
            }
            threeDNR_list.setSummary(value);
        } else if (MoveList_KEY.equals(key)) {
            String value = sharedPreferences.getString(key, null);
            if (value != null) {
                command = new MovingListCommand(value);
            }
            move_list.setSummary(value);
        } else if (ODT_switch_KEY.equals(key)) {
            Boolean value = sharedPreferences.getBoolean(key, false);
            command = new ODTCommand(value);
            if (value) {
                ODT_level.setSummary(String.valueOf(DeviceInfo.ODT_DEFAULT_LEVEL - DeviceInfo.ODT_LEVEL_STANDARD));
                ODT_level.setEnabled(true);
            } else {
                ODT_level.setSummary("-1");
                ODT_level.setEnabled(false);
            }
        } else if (ODT_level_KEY.equals(key)) {
            String value = sharedPreferences.getString(key, null);
            if (value != null) {
                command = new ODTLevelCommand(value);
            }
            ODT_level.setSummary(value);
        }
        if (command == null) {

            Log.d(TAG, "command is null");
            return;
        }

        if (null == mLoadingView || !mLoadingView.isShowing()) {
            mLoadingView = new LoadingView(this, getResources().getString(R.string.sending_msg));
            mLoadingView.show();
        }
        sendMsg2Remote(command.Json(), MsgDatas.MSG_UPDATE_SETTINGS, MsgDatas.NETWORK_TIMEOUT * 2);
    }


    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (Update_KEY.equals(preference.getKey())) {
            Intent intent = new Intent(this, UpdateActivity.class);
            intent.putExtra(TUTKManager.TUTK_UID, mUID);
            intent.putExtra("version", update.getSummary());
            startActivityForResult(intent, REQUEST_UPDATE);
            return false;
        } else if (Encoder_settings_KEY.equals(preference.getKey())) {
            if (mTUTKDevice != null && mTUTKDevice.getDeviceInfo().encoderParameters.size() > 0) {
                Intent intent = new Intent(this, EncoderSettingsActivity.class);
                intent.putExtra(TUTKManager.TUTK_UID, mUID);
                intent.putExtra("channel", 1);
                startActivityForResult(intent, REQUEST_ENCORDER);
            } else {
                Toast.makeText(this, R.string.video_setting_activity_fail_get_encoder_parameter, Toast.LENGTH_SHORT).show();
                GetEncoderParameters getEncoderParameters = new GetEncoderParameters();
                byte[] bytes = getEncoderParameters.Json().getBytes();
                int i = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(), RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
                Log.d("GetEncoderParameters", "GetEncoderParameters =　" + i);
            }

        } else if (Format_KEY.equals(preference.getKey())) {
            mFormatDialog = new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.format_summary))
                    .setPositiveButton(android.R.string.yes, this)
                    .setNegativeButton(android.R.string.no, this)
                    .show();
        } else if (Device_reboot_KEY.equals(preference.getKey())) {
            mRebootDialog = new AlertDialog.Builder(this).setMessage(R.string.video_setting_activity_reboot_title)
                    .setPositiveButton(android.R.string.yes, this)
                    .setNegativeButton(android.R.string.no, this)
                    .show();
        } else if (Sdcard_KEY.equals(preference.getKey())) {
            //jump to videolist
            Intent intent = new Intent(this, RemoteRecordActivity.class);
            intent.putExtra(TUTKManager.TUTK_UID, mTUTKDevice.getUID());
            intent.putExtra(TUTKManager.TUTK_DEVICE_NAME, mTUTKDevice.getDeviceInfo().getDeviceName());
            startActivityForResult(intent, REQUEST_RECODE);
        } else if (Device_recovery_KEY.equals(preference.getKey())) {
            mRecoveryDialog = new AlertDialog.Builder(this).setMessage(R.string.video_setting_activity_recovery_title)
                    .setPositiveButton(android.R.string.yes, this)
                    .setNegativeButton(android.R.string.no, this)
                    .show();
        } else if (DEVICE_PASSWORD_KEY.equals(preference.getKey())) {
            Intent intent = new Intent(this, PasswordActivity.class);
            intent.putExtra(TUTKManager.TUTK_UID, mTUTKDevice.getUID());
            intent.putExtra(TUTKManager.TUTK_DEVICE_NAME, mTUTKDevice.getDeviceInfo().getDeviceName());
            startActivityForResult(intent, REQUEST_PASSWORD);
        }
        return false;
    }


    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (dialogInterface == mFormatDialog) {
            boolean format = i == DialogInterface.BUTTON_POSITIVE;
            if (format) {
                FormatCommand formatCommand = new FormatCommand(format);
                sendMsg2Remote(formatCommand.Json(), MsgDatas.MSG_FORMAT_TIMEOUT,
                        MsgDatas.FORMAT_TIMEOUT);
                if (null == mLoadingView || !mLoadingView.isShowing()) {
                    mLoadingView = new LoadingView(this, getResources().getString(R.string.formating));
                    //mLoadingView.setCancelable(false);
                    mLoadingView.show();
                }
                //sendFile();
            }
        } else if (dialogInterface == mRebootDialog) {
            boolean reboot = i == DialogInterface.BUTTON_POSITIVE;
            if (reboot) {
                RebootCommand formatCommand = new RebootCommand();
                int write = mTUTKDevice.getSession().write(formatCommand.Json());
                if (write >= 0) {
                    finish();
                }
                //sendFile();
            }
        } else if (dialogInterface == mRecoveryDialog) {
            if (DialogInterface.BUTTON_POSITIVE == i) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", "ResetFactory");
                    sendMsg2Remote(jsonObject.toString(), MsgDatas.MSG_RECOVERY_TIMEOUT);
                    if (null == mLoadingView || !mLoadingView.isShowing()) {
                        mLoadingView = new LoadingView(this, getResources().getString(R.string.pref_setting_device_recovery));
                        mLoadingView.show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mHandler.removeMessages(MsgDatas.MSG_RECOVERY_TIMEOUT);
                    mHandler.sendEmptyMessage(MsgDatas.MSG_RECOVERY_TIMEOUT);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUEST_UPDATE == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                finish();
            }
        } else if (REQUEST_RECODE == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                finish();
            } else {
                sendMsg2Remote("{\"name\":\"GetSdcardInfo\"}", MsgDatas.MSG_GET_SDCARDINFO_TIMEOUT);
                Log.d(TAG, "send GetSdcardInfo");
            }
        } else if (REQUEST_ENCORDER == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                finish();
            }
        } else if (REQUEST_PASSWORD == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                mIsResetPwd = true;
            }
        }
    }

    @Override
    public void onDeviceInfoChange(DeviceInfo deviceInfo, final String name) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != mHandler) {
                    mHandler.removeMessages(MsgDatas.MSG_GET_SETTINGS_TIMEOUT);
                    mHandler.removeMessages(MsgDatas.MSG_UPDATE_SETTINGS);
                }
                synchronized (mLock) {
                    getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(VideoSettingActivity.this);
                    resetUI(mTUTKDevice);
                    getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(VideoSettingActivity.this);
                }
                if (DeviceInfo.ODT.equals(name)) {
                    long costTime = System.currentTimeMillis() - mSendMsgTime;
                    Log.v(TAG, "send odt and cost time=" + costTime);
                    if (costTime < 10000) {
                        mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_FORMAT_FAILED, 2000);
                    }
                } else if (null != mLoadingView && mLoadingView.isShowing()) {
                    mLoadingView.cancel();
                }
            }
        });

    }

    @Override
    public void onSdcardFormat(boolean result) {
        mHandler.removeMessages(MsgDatas.MSG_FORMAT_TIMEOUT);
        if (result) {
            byte[] bytes = "{\"name\":\"GetSdcardInfo\"}".getBytes();
            int write = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(), RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
            Log.d(TAG, "GetSdcardInfo write= " + write);
            mHandler.sendEmptyMessage(MsgDatas.MSG_FORMAT_SUCCESS);
        } else {
            mHandler.sendEmptyMessage(MsgDatas.MSG_FORMAT_FAILED);
        }
    }

    @Override
    public void onError(final int code) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(VideoSettingActivity.this, String.format("与设备断开连接 [%d]", code), Toast.LENGTH_SHORT).show();
            }
        });
        finish();
    }

    @Override
    public void onEncoderChange(EncoderParameter parameter) {
        if (null != parameter && parameter.getChannel().equals("0")) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLock) {
                        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(VideoSettingActivity.this);
                        reflushResolution();
                        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(VideoSettingActivity.this);
                    }
                }
            });
        }
    }

    private void reflushResolution() {
        String currentResolution = getCurrentResolution();
        if (TextUtils.isEmpty(currentResolution)) {
            resolution_list.setEnabled(false);
        } else {
            resolution_list.setSummary(currentResolution);
            resolution_list.setValue(mTUTKDevice.getDeviceInfo().getmResolution());
            resolution_list.setEnabled(true);
        }
    }

    private String getCurrentResolution() {
        String[] titleArray = getResources().getStringArray(R.array.pref_resolution_list_titles);
        String[] valueArray = getResources().getStringArray(R.array.pref_resolution_list_values);
        String mStr = null;
        for (int i = 0; i < titleArray.length; i++) {
            if (valueArray[i].equals(mTUTKDevice.getDeviceInfo().getmResolution())) {
                mStr = titleArray[i];
            }
        }
        return mStr;
    }

    private void changeResolution(String value) {
        //get parameters
        EncoderParameter encoderParameter = null;
        for (int i = 0; i < mTUTKDevice.getDeviceInfo().encoderParameters.size(); i++) {
            encoderParameter = mTUTKDevice.getDeviceInfo().encoderParameters.get(i);
            if (encoderParameter.getChannel().equals(String.valueOf(0))) {
                break;
            }
        }
        if (TextUtils.isEmpty(value) || null == encoderParameter) {
            Toast.makeText(this, R.string.parameter_error, Toast.LENGTH_SHORT).show();
            return;
        }

        if (value.equals("1080")) {
            encoderParameter.setHeight("1080");
            encoderParameter.setWidth("1920");
        } else if (value.equals("720")) {
            encoderParameter.setHeight("720");
            encoderParameter.setWidth("1280");
        }
        resolution_list.setSummary(value + "P");
        mTUTKDevice.getDeviceInfo().setmResolution(value);
        SetEncoderParameters setEncoderParameters = new SetEncoderParameters(encoderParameter);
        byte[] bytes = setEncoderParameters.Json().getBytes();
        int write = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(), RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
        Log.d(TAG, "change resolution write:" + write);
    }

    private void sendMsg2Remote(final String msg, final int msgWhat) {
        sendMsg2Remote(msg, msgWhat, MsgDatas.NETWORK_TIMEOUT);
    }

    private void sendMsg2Remote(final String msg, final int msgWhat, final long delayMillis) {
        new Thread() {
            @Override
            public void run() {
                Log.v(TAG, "sendMsg2Remote: " + msg);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                int write = -1;
                mHandler.sendEmptyMessageDelayed(msgWhat, delayMillis);
                mSendMsgTime = System.currentTimeMillis();
                try {
                    byte[] bytes = msg.getBytes();
                    write = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(),
                            RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (write != 0) {
                    mHandler.removeMessages(msgWhat);
                    Message msg = new Message();
                    msg.what = msgWhat;
                    msg.obj = write;
                    mHandler.sendMessage(msg);
                } else if (MsgDatas.MSG_FORMAT_TIMEOUT == msgWhat) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            sdcard.setTitle("sdcard");
                        }
                    });
                } else if (MsgDatas.MSG_RECOVERY_TIMEOUT == msgWhat) {
                    mHandler.removeMessages(MsgDatas.MSG_RECOVERY_TIMEOUT);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void finish() {
        if (mIsResetPwd) {
            Message localMessage = new Message();
            localMessage.what = 3;
            MsgManager.sendToHandler("FragementCamera", localMessage);
        }
        super.finish();
    }
}
