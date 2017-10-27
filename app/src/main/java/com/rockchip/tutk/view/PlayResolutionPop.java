package com.rockchip.tutk.view;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import android.util.Log;
import android.widget.Toast;
import com.rockchip.tutk.EncoderParameter;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.command.SetEncoderParameters;
import com.tutk.IOTC.AVAPIs;
import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;

import com.rockchip.tutk.R;

/**
 * Created by waha on 2017/3/17.
 */

public class PlayResolutionPop implements View.OnClickListener {
    private Context mContext;
    private PopupWindow mPop;
    private OnPlayResolutionPopListener mListener;

    private EncoderParameter mEncoderParameter;
    private TUTKDevice mTUTKDevice;
    private static final String Width_1080 = "1920";
    private static final String Height_1080 = "1080";

    private static final String Width_720 = "1280";
    private static final String Height_720 = "720";

    private static final String Width_480 = "640";
    private static final String Height_480 = "480";

    private static final String RATE_1080 = "2000";
    private static final String RATE_720 = "1000";
    private static final String RATE_480 = "300";

    public PlayResolutionPop(Context context, OnPlayResolutionPopListener listener,TUTKDevice tutkDevice) {
        mContext = context;
        initUI(context);
        mListener = listener;
        mTUTKDevice = tutkDevice;
    }

    private void initUI(Context context) {
        String[] titleArray = mContext.getResources().getStringArray(R.array.pref_play_resolution_list_titles);
        String[] valueArray = mContext.getResources().getStringArray(R.array.pref_play_resolution_list_values);
        LayoutInflater inflater = LayoutInflater.from(context);
        View popView = inflater.inflate(R.layout.pop_resolution, null);
        LinearLayout layoutResolution = (LinearLayout) popView.findViewById(R.id.layout_resolution);
        for (int i = 0; i < titleArray.length; i++) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 1, 0, 0);
            TextView txtView = new TextView(context);
            txtView.setPadding(18, 18, 18, 18);
            txtView.setTextColor(Color.WHITE);
            txtView.setTextSize(18);
            txtView.setText(titleArray[i]);
            txtView.setTag(valueArray[i]);
            txtView.setGravity(Gravity.CENTER);
            txtView.setLayoutParams(params);
            txtView.setBackgroundColor(Color.parseColor("#808080"));
            layoutResolution.addView(txtView);
            txtView.setOnClickListener(this);
        }
        mPop = new PopupWindow(popView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public void showPop(View view) {
        if (null != mPop) {
            mPop.showAsDropDown(view);
        }
    }

    public void hidePop() {
        if (null != mPop) {
            mPop.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (null != mListener && v instanceof TextView) {
            String title = ((TextView) v).getText().toString();
            String value = v.getTag().toString();

            //get video parameters
            EncoderParameter encoderParameter = null;
            for (int i = 0; i < mTUTKDevice.getDeviceInfo().encoderParameters.size(); i++) {
                encoderParameter = mTUTKDevice.getDeviceInfo().encoderParameters.get(i);
                if (encoderParameter.getChannel().equals(String.valueOf(1))) {
                    mEncoderParameter = encoderParameter;
                }
            }
            //set video parameters
             if(mEncoderParameter!=null){
                if(value.equals(Height_1080)){
                    Log.d("ljh","---------1080P");
                    mEncoderParameter.setHeight(Height_1080);
                    mEncoderParameter.setWidth(Width_1080);
                    mEncoderParameter.setBit_rate(RATE_1080);
                }else if(value.equals(Height_720)){
                    Log.d("ljh","---------720P");
                    mEncoderParameter.setHeight(Height_720);
                    mEncoderParameter.setWidth(Width_720);
                    mEncoderParameter.setBit_rate(RATE_720);
                }else{
                    Log.d("ljh","---------480P");
                    mEncoderParameter.setHeight(Height_480);
                    mEncoderParameter.setWidth(Width_480);
                    mEncoderParameter.setBit_rate(RATE_480);
                }
                SetEncoderParameters setEncoderParameters = new SetEncoderParameters(mEncoderParameter);
                byte[] bytes = setEncoderParameters.Json().getBytes();
                int i = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(), RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
             }else{
                Toast.makeText(this.mContext, R.string.txt_set_resolution_failed, Toast.LENGTH_SHORT).show();
             }
            mTUTKDevice.getDeviceInfo().setPlayResolution(value);
            mListener.playResolutionClick(title, value);
        }
    }

    public interface OnPlayResolutionPopListener {
        public boolean playResolutionClick(String title, String val);
    }
}
