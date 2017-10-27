package com.rockchip.tutk.activity;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.rockchip.tutk.DeviceInfo;
import com.rockchip.tutk.EncoderParameter;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.command.GetEncoderParameters;
import com.rockchip.tutk.command.SetEncoderParameters;
import com.rockchip.tutk.model.SdcardModel;
import com.rockchip.tutk.view.NumberPickerDialog;
import com.tutk.IOTC.AVAPIs;

import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;

public class EncoderSettingsActivity extends PreferenceActivity implements Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener, TUTKDevice.EncoderChangeCallback, TUTKDevice.OnTutkError {

    public static final String ENCODER_FRAME_RATE_KEY = "encoder_frame_rate";
    public static final String ENCODER_LEVEL_KEY = "encoder_level";
    public static final String ENCODER_GOP_SIZE_KEY = "encoder_gop_size";
    public static final String ENCODER_PROFILE_KEY = "encoder_profile";
    public static final String ENCODER_QUALITY_KEY = "encoder_quality";
    public static final String ENCODER_QP_INIT_KEY = "encoder_qp_init";
    public static final String ENCODER_QP_MIN_KEY = "encoder_qp_min";
    public static final String ENCODER_QP_MAX_KEY = "encoder_qp_max";
    public static final String ENCODER_QP_STEP_KEY = "encoder_qp_step";
    public static final String ENCODER_RC_MODE_KEY = "encoder_rc_mode";
    public static final String ENCODER_WIDTH_KEY = "encoder_width";
    public static final String ENCODER_HEIGHT_KEY = "encoder_height";
    public static final String ENCODER_WIDTH_HEIGHT_KEY = "encoder_width_height";
    public static final String ENCODER_BIT_RATE_KEY = "encoder_bit_rate";
    public static final String ENCODER_CHANNEL_KEY = "encoder_channel";

    Preference p_frame_rate;
    Preference p_level;
    Preference p_gop_size;
    Preference p_profile;
    Preference p_quality;
    Preference p_qp_init;
    Preference p_qp_min;
    Preference p_qp_max;
    Preference p_qp_step;
    Preference p_rc_mode;
    /*Preference p_width;
    Preference p_height;*/
    Preference p_bit_rate;
    Preference p_channel;
    Preference p_width_height;

    public static  int DEFAULT_CHANNEL = 1;
    public static final int DEFAULT_QUALITY = 1;
    public static final int DEFAULT_RC_MODE = 1;
    public static final int NUMBER_MIN = 0;
    public static final int NUMBER_MAX = 100;
    public static final int DEFAULT_FRAME_RATE = 15;
    public static final int DEFAULT_LEVEL = 51;
    public static final int DEFAULT_GOP_SIZE = 60;
    public static final int DEFAULT_PROFILE = 100;
    public static final int DEFAULT_QP_INIT = 0;
    public static final int DEFAULT_QP_MIN = 4;
    public static final int DEFAULT_QP_MAX = 48;
    public static final int DEFAULT_QP_STEP = 4;
    public static final int DEFAULT_WIDTH = 1920;
    public static final int DEFAULT_HEIGHT = 1080;
    public static final int DEFAULT_WIDTH_HEIGHT = 0;
    public static final int DEFAULT_BIT_RATE = 800;
    private String mUID;
    private TUTKDevice mTUTKDevice;
    private String mDeviceName;
    EncoderParameter mEncoderParameter;
    private String TAG = "EncoderSettingsActivity";
    Handler mHandler=new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_encoder_setting);
        p_frame_rate = findPreference(ENCODER_FRAME_RATE_KEY);
        p_frame_rate.setOnPreferenceClickListener(this);
        p_level = findPreference(ENCODER_LEVEL_KEY);
        p_level.setOnPreferenceClickListener(this);
        p_gop_size = findPreference(ENCODER_GOP_SIZE_KEY);
        p_gop_size.setOnPreferenceClickListener(this);
        p_profile = findPreference(ENCODER_PROFILE_KEY);
        p_profile.setOnPreferenceClickListener(this);
        p_qp_init = findPreference(ENCODER_QP_INIT_KEY);
        p_qp_init.setOnPreferenceClickListener(this);
        p_qp_min = findPreference(ENCODER_QP_MIN_KEY);
        p_qp_min.setOnPreferenceClickListener(this);
        p_qp_max = findPreference(ENCODER_QP_MAX_KEY);
        p_qp_max.setOnPreferenceClickListener(this);
        p_qp_step = findPreference(ENCODER_QP_STEP_KEY);
        p_qp_step.setOnPreferenceClickListener(this);
        /*p_width = findPreference(ENCODER_WIDTH_KEY);
        p_width.setOnPreferenceClickListener(this);
        p_height = findPreference(ENCODER_HEIGHT_KEY);
        p_height.setOnPreferenceClickListener(this);*/
        p_bit_rate = findPreference(ENCODER_BIT_RATE_KEY);
        p_bit_rate.setOnPreferenceClickListener(this);
        p_width_height = findPreference(ENCODER_WIDTH_HEIGHT_KEY);
        p_channel = findPreference(ENCODER_CHANNEL_KEY);
        p_quality = findPreference(ENCODER_QUALITY_KEY);
        p_rc_mode = findPreference(ENCODER_RC_MODE_KEY);

        init();

        resetData();
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
            setTitle(mDeviceName);
        }
    }

    private void resetData() {
        EncoderParameter encoderParameter = null;
        if (mTUTKDevice != null) {
            DeviceInfo deviceInfo = mTUTKDevice.getDeviceInfo();
            for (int i = 0; i < deviceInfo.encoderParameters.size(); i++) {
                encoderParameter = deviceInfo.encoderParameters.get(i);
                if (encoderParameter.getChannel().equals(String.valueOf(DEFAULT_CHANNEL))) {
                    mEncoderParameter = encoderParameter;
                    SharedPreferences.Editor edit = getPreferenceManager().getSharedPreferences().edit();
                    edit.putString(ENCODER_FRAME_RATE_KEY, encoderParameter.getFrame_rate());
                    edit.putString(ENCODER_LEVEL_KEY, encoderParameter.getLevel());
                    edit.putString(ENCODER_GOP_SIZE_KEY, encoderParameter.getGop_size());
                    edit.putString(ENCODER_PROFILE_KEY, encoderParameter.getProfile());
                    edit.putString(ENCODER_QUALITY_KEY, encoderParameter.getQuality());
                    edit.putString(ENCODER_QP_INIT_KEY, encoderParameter.getQp_init());
                    edit.putString(ENCODER_QP_MIN_KEY, encoderParameter.getQp_min());
                    edit.putString(ENCODER_QP_MAX_KEY, encoderParameter.getQp_max());
                    edit.putString(ENCODER_QP_STEP_KEY, encoderParameter.getQp_step());
                    edit.putString(ENCODER_RC_MODE_KEY, encoderParameter.getRc_mode());
                    edit.putString(ENCODER_WIDTH_KEY, encoderParameter.getWidth());
                    edit.putString(ENCODER_HEIGHT_KEY, encoderParameter.getHeight());
                    edit.putString(ENCODER_BIT_RATE_KEY, encoderParameter.getBit_rate());
                    edit.putString(ENCODER_CHANNEL_KEY, encoderParameter.getChannel());
                    edit.putString(ENCODER_CHANNEL_KEY, encoderParameter.getChannel());
                    boolean commit = edit.commit();
                    edit.apply();
                }
            }
        }
    }

    private void resetUI(TUTKDevice tutkDevice) {

        EncoderParameter encoderParameter = null;
        if (mTUTKDevice != null) {
            DeviceInfo deviceInfo = mTUTKDevice.getDeviceInfo();
            for (int i = 0; i < deviceInfo.encoderParameters.size(); i++) {
                encoderParameter = deviceInfo.encoderParameters.get(i);
                if (encoderParameter.getChannel().equals(String.valueOf(DEFAULT_CHANNEL))) {
                    mEncoderParameter = encoderParameter;
                    p_frame_rate.setDefaultValue(encoderParameter.getFrame_rate());
                    p_frame_rate.setSummary(String.valueOf(encoderParameter.getFrame_rate()));
                    p_level.setDefaultValue(encoderParameter.getLevel());
                    p_level.setSummary(String.valueOf(encoderParameter.getLevel()));
                    p_gop_size.setDefaultValue(encoderParameter.getGop_size());
                    p_gop_size.setSummary(String.valueOf(encoderParameter.getGop_size()));
                    p_profile.setDefaultValue(encoderParameter.getProfile());
                    p_profile.setSummary(String.valueOf(encoderParameter.getProfile()));
                    p_qp_init.setDefaultValue(encoderParameter.getQp_init());
                    p_qp_init.setSummary(String.valueOf(encoderParameter.getQp_init()));
                    p_qp_min.setDefaultValue(encoderParameter.getQp_min());
                    p_qp_min.setSummary(String.valueOf(encoderParameter.getQp_min()));
                    p_qp_max.setDefaultValue(encoderParameter.getQp_max());
                    p_qp_max.setSummary(String.valueOf(encoderParameter.getQp_max()));
                    p_qp_step.setDefaultValue(encoderParameter.getQp_step());
                    p_qp_step.setSummary(String.valueOf(encoderParameter.getQp_step()));
                    /*p_width.setDefaultValue(encoderParameter.getWidth());
                    p_width.setSummary(String.valueOf(encoderParameter.getWidth()));
                    p_height.setDefaultValue(encoderParameter.getHeight());
                    p_height.setSummary(String.valueOf(encoderParameter.getHeight()));*/
                    p_bit_rate.setDefaultValue(encoderParameter.getBit_rate());
                    p_bit_rate.setSummary(String.valueOf(encoderParameter.getBit_rate()));

                    p_channel.setDefaultValue(encoderParameter.getChannel());
                    p_channel.setSummary(getResources().getStringArray(R.array.pref_encoder_channel_list_titles)[Integer.parseInt(encoderParameter.getChannel())]);
                    p_quality.setDefaultValue(encoderParameter.getQuality());
                    p_quality.setSummary(getResources().getStringArray(R.array.pref_encoder_quality_list_titles)[Integer.parseInt(encoderParameter.getQuality())]);
                    p_rc_mode.setDefaultValue(encoderParameter.getRc_mode());
                    p_rc_mode.setSummary(getResources().getStringArray(R.array.pref_encoder_rc_mode_list_titles)[Integer.parseInt(encoderParameter.getRc_mode())]);

                    p_width_height.setSummary(String.format("%s * %s",encoderParameter.getWidth(),encoderParameter.getHeight()));
                }
            }
        }
        if (encoderParameter == null) {
            p_frame_rate.setDefaultValue(DEFAULT_FRAME_RATE);
            p_frame_rate.setSummary(String.valueOf(DEFAULT_FRAME_RATE));
            p_level.setDefaultValue(DEFAULT_LEVEL);
            p_level.setSummary(String.valueOf(DEFAULT_LEVEL));
            p_gop_size.setDefaultValue(DEFAULT_GOP_SIZE);
            p_gop_size.setSummary(String.valueOf(DEFAULT_GOP_SIZE));
            p_profile.setDefaultValue(DEFAULT_PROFILE);
            p_profile.setSummary(String.valueOf(DEFAULT_PROFILE));
            p_qp_init.setDefaultValue(DEFAULT_QP_INIT);
            p_qp_init.setSummary(String.valueOf(DEFAULT_QP_INIT));
            p_qp_min.setDefaultValue(DEFAULT_QP_MIN);
            p_qp_min.setSummary(String.valueOf(DEFAULT_QP_MIN));
            p_qp_max.setDefaultValue(DEFAULT_QP_MAX);
            p_qp_max.setSummary(String.valueOf(DEFAULT_QP_MAX));
            p_qp_step.setDefaultValue(DEFAULT_QP_STEP);
            p_qp_step.setSummary(String.valueOf(DEFAULT_QP_STEP));
            /*p_width.setDefaultValue(DEFAULT_WIDTH);
            p_width.setSummary(String.valueOf(DEFAULT_WIDTH));
            p_height.setDefaultValue(DEFAULT_HEIGHT);
            p_height.setSummary(String.valueOf(DEFAULT_HEIGHT));*/
            p_bit_rate.setDefaultValue(DEFAULT_BIT_RATE);
            p_bit_rate.setSummary(String.valueOf(DEFAULT_BIT_RATE));

            p_channel.setDefaultValue(DEFAULT_CHANNEL);
            p_channel.setSummary(getResources().getStringArray(R.array.pref_encoder_channel_list_titles)[DEFAULT_CHANNEL]);
            p_quality.setDefaultValue(DEFAULT_QUALITY);
            p_quality.setSummary(getResources().getStringArray(R.array.pref_encoder_quality_list_titles)[DEFAULT_QUALITY]);
            p_rc_mode.setDefaultValue(DEFAULT_RC_MODE);
            p_rc_mode.setSummary(getResources().getStringArray(R.array.pref_encoder_rc_mode_list_titles)[DEFAULT_RC_MODE]);

            p_width_height.setSummary(String.format("%s * %s",encoderParameter.getWidth(),encoderParameter.getHeight()));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG,String.format("onSharedPreferenceChanged key=%s, value = %s",key,sharedPreferences.getString(key,"")));
        if (key.equals(ENCODER_QUALITY_KEY)){
            p_quality.setSummary(getResources().getStringArray(R.array.pref_encoder_quality_list_titles)[Integer.parseInt(sharedPreferences.getString(key,""))]);
        }else if (key.equals(ENCODER_RC_MODE_KEY)){
            p_rc_mode.setSummary(getResources().getStringArray(R.array.pref_encoder_rc_mode_list_titles)[Integer.parseInt(sharedPreferences.getString(key,""))]);
        }else if (key.equals(ENCODER_WIDTH_HEIGHT_KEY)){
            if (sharedPreferences.getString(key,"").equals("0"))
            {
                mEncoderParameter.setWidth("1920");
                mEncoderParameter.setHeight("1080");
            }else if  (sharedPreferences.getString(key,"").equals("1")){
                mEncoderParameter.setWidth("1280");
                mEncoderParameter.setHeight("720");
            }else if  (sharedPreferences.getString(key,"").equals("2")){
                mEncoderParameter.setWidth("640");
                mEncoderParameter.setHeight("480");
            }else{
                return;
            }
            sharedPreferences.edit().putString(key,"-1");
        }


        if (key.equals(ENCODER_FRAME_RATE_KEY)){
            mEncoderParameter.setFrame_rate(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_LEVEL_KEY)){
            mEncoderParameter.setLevel(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_GOP_SIZE_KEY)){
            mEncoderParameter.setGop_size(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_PROFILE_KEY)){
            mEncoderParameter.setProfile(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_QUALITY_KEY)){
            mEncoderParameter.setQuality(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_QP_INIT_KEY)){
            mEncoderParameter.setQp_init(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_QP_MIN_KEY)){
            mEncoderParameter.setQp_min(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_QP_MAX_KEY)){
            mEncoderParameter.setQp_max(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_QP_STEP_KEY)){
            mEncoderParameter.setQp_step(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_RC_MODE_KEY)){
            mEncoderParameter.setRc_mode(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_WIDTH_KEY)){
            mEncoderParameter.setWidth(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_HEIGHT_KEY)){
            mEncoderParameter.setHeight(sharedPreferences.getString(key,""));
        }else if (key.equals(ENCODER_BIT_RATE_KEY)){
            mEncoderParameter.setBit_rate(sharedPreferences.getString(key,""));
        }

        if (key.equals(ENCODER_CHANNEL_KEY)){
            DEFAULT_CHANNEL=Integer.parseInt(sharedPreferences.getString(key,""));
            resetUI(mTUTKDevice);
        }else{
            try{
                SetEncoderParameters setEncoderParameters = new SetEncoderParameters(mEncoderParameter);
                byte[] bytes = setEncoderParameters.Json().getBytes();
                int i = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(), RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
                Log.d(TAG, "SetEncoderParameters ret=" + i);
            } catch (NullPointerException e){
                Log.e(TAG, "happen NullPointerException when SetEncoderParameters");
                e.printStackTrace();
                if(!isFinishing()){
                    Toast.makeText(this, R.string.unconnect, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                }
            }
        }
    }

    @Override
    public void onEncoderChange(EncoderParameter parameter) {
        mEncoderParameter=parameter;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                resetUI(mTUTKDevice);
            }
        });

    }

    @Override
    public void onError(final int code) {
        if(!isFinishing()){
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EncoderSettingsActivity.this, String.format("与设备断开连接 [%d]", code), Toast.LENGTH_SHORT).show();
                }
            });
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    class NumberSetLinstener implements NumberPickerDialog.OnNumberSetListener {
        Preference mPreference;

        public NumberSetLinstener(Preference preference) {
            this.mPreference = preference;
        }

        @Override
        public void onNumberSet(int number) {
            mPreference.setSummary(String.valueOf(number));
            mPreference.getEditor().putString(mPreference.getKey(),String.valueOf(number)).apply();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        resetUI(mTUTKDevice);
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        if (mTUTKDevice!=null){
            mTUTKDevice.addOnTutkErrorListener(this);
            mTUTKDevice.addEncoderChangeCallbacks(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        if(null != mTUTKDevice){
            mTUTKDevice.removeOnTutkErrorListener(this);
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        NumberSetLinstener numberSetLinstener = new NumberSetLinstener(preference);
        if (preference == p_frame_rate) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_FRAME_RATE : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_frame_rate).show();
            return true;
        } else if (preference == p_level) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_LEVEL : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_level).show();
            return true;
        } else if (preference == p_gop_size) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_GOP_SIZE : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_gop_size).show();
            return true;
        } else if (preference == p_profile) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_PROFILE : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_profile).show();
            return true;
        } else if (preference == p_qp_init) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_QP_INIT : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_qp_init).show();
            return true;
        } else if (preference == p_qp_step) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_QP_STEP : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_qp_step).show();
            return true;
        } else if (preference == p_qp_min) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_QP_MIN : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_qp_min).show();
            return true;
        } else if (preference == p_qp_max) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_QP_MAX : Integer.parseInt(preference.getSummary().toString()),
                    NUMBER_MIN,
                    NUMBER_MAX,
                    R.string.pref_encoder_qp_max).show();
            return true;
        } /*else if (preference == p_width) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_WIDTH : Integer.parseInt(preference.getSummary().toString()),
                    480,
                    1920,
                    R.string.pref_encoder_width).show();
            return true;
        } else if (preference == p_height) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_HEIGHT : Integer.parseInt(preference.getSummary().toString()),
                    320,
                    1080,
                    R.string.pref_encoder_height).show();
            return true;
        }*/ else if (preference == p_bit_rate) {
            new NumberPickerDialog(this, numberSetLinstener,
                    preference.getSummary() == null ? DEFAULT_BIT_RATE : Integer.parseInt(preference.getSummary().toString()),
                    50,
                    5,
                    1000 * 1000,
                    R.string.pref_encoder_bit_rate).show();
            return true;
        }

        return false;
    }
}