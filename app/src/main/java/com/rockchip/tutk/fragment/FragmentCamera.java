package com.rockchip.tutk.fragment;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.rockchip.tutk.constants.Constants;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.TUTKServer;
import com.rockchip.tutk.constants.GlobalValue;
import com.rockchip.tutk.dialog.DeviceListDialog;
import com.rockchip.tutk.dialog.DeviceLoginDialog;
import com.rockchip.tutk.dialog.QRCodeDialog;
import com.rockchip.tutk.dialog.TipDialog;
import com.rockchip.tutk.http.HttpHub;
import com.rockchip.tutk.utils.HttpUtils;
import com.rockchip.tutk.R;
import com.rockchip.tutk.utils.MsgManager;
import com.rockchip.tutk.utils.UserManager;
import com.rockchip.tutk.view.LoadingView;
import com.tutk.IOTC.st_SearchDeviceInfo;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Set;

import static com.tutk.IOTC.IOTCAPIs.IOTC_Search_Device_Start;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Search_Device_Stop;

public class FragmentCamera extends Fragment implements   TUTKServer.Listener, View.OnClickListener
{
    String TAG = "wz";
    ImageButton btn_adddivice,btn_campreview;
    Button btn_cam_share;
    RelativeLayout layout_cam_tips;
    LinearLayout layout_cam_preview;
    private LoadingView mLoginView;
    TUTKServer mHelperService;
    int mNumOfDevices;
    public Set<String> mStringSet;
    public String mUID;
    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case Constants.UserMSG.LOGINOK:
                {
                    Log.i("wz","LOGINOK");
                    break;
                }
                case Constants.UserMSG.REFRESH:
                {
                    Log.i("wz","REFRESH");
                    refreshDeviceList();
                    break;
                }
                case Constants.UserMSG.DEVICEERROR:
                {
                    new TipDialog(getActivity(), (String)msg.obj).showit();
                    break;
                }
                case Constants.UserMSG.REQUESTPW:
                {
                    TUTKDevice info = TUTKManager.getByUID(GlobalValue.deviceUID);
                    Intent localIntent = new Intent();
                    localIntent.putExtra(TUTKManager.TUTK_UID, info.getUID());
                    localIntent.putExtra(TUTKManager.TYPE, 0);
                    new DeviceLoginDialog(getActivity(), localIntent, "needpw").show();
                    break;
                }
            }
        }
    };

    @Override
    public void deviceAdded(TUTKDevice device) {

    }

    @Override
    public void deviceRemoved(TUTKDevice device) {


    }
    @Override
    public void onDetach() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                IOTC_Search_Device_Stop();
            }
        }).start();
        super.onDetach();
    }

    public void OnServiceBind(TUTKServer server) {
        mHelperService = server;
        mHelperService.addListener(this);
        reflush(false);
    }
    private void reflush(final boolean showDialog) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                mLanSearch(showDialog);
            }
        }).start();
    }

    private void mLanSearch(boolean showDialog) {

        int[] nArray = new int[1];
        mNumOfDevices = 0;

        IOTC_Search_Device_Start(1000, 100);

        while (true) {
            st_SearchDeviceInfo[] ab_LanSearchInfo  = null;//IOTC_Search_Device_Result(nArray, 0);
            if (nArray[0] < 0) {
                break;
            }

         //   Log.d(TAG, "st_SearchDeviceInfo  === " + nArray[0]);

            for (int i = 0; i < nArray[0]; i++) {

                try {
                    String uid = new String(ab_LanSearchInfo[i].UID, 0, ab_LanSearchInfo[i].UID.length, "utf-8");
                    String ip = new String(ab_LanSearchInfo[i].IP, 0, ab_LanSearchInfo[i].IP.length, "utf-8");
                    Log.d(TAG, "UID = " + i + " = " + uid);
                    Log.d(TAG, "IP " + i + " = " + ip);
                    mNumOfDevices++;
                    Intent intent = new Intent(TUTKManager.ACTION_TUTK_DEVICE_ATTACHED);
                    intent.putExtra("UID", uid);
                    Context context = getContext();
                    if (context != null) {
                        context.sendBroadcast(intent);
                    }
                } catch (UnsupportedEncodingException e) {

                    e.printStackTrace();
                }

                Log.d(TAG, "Port " + i + " = " + String.valueOf(ab_LanSearchInfo[i].port));
                Log.d(TAG, "****************************");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        MsgManager.registerHandler(mHandler,"FragementCamera");
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        layout_cam_tips = (RelativeLayout) view.findViewById(R.id.layout_cam_tips);
        layout_cam_preview = ((LinearLayout)view.findViewById(R.id.layout_cam_preview));
        btn_adddivice = (ImageButton)view.findViewById(R.id.btn_title_add);
        btn_adddivice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DeviceListDialog dialog = new DeviceListDialog(getActivity());
                dialog.show();
            }
        });
        btn_campreview = (ImageButton)view.findViewById(R.id.btn_cam_preview);
        btn_campreview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
//                TUTKDevice info = TUTKManager.getByUID(GlobalValue.deviceUID);
//                Intent loginIntent =new Intent(getContext(), LoginActivity.class);
//                loginIntent.putExtra(TUTKManager.TUTK_UID, info.getUID());
//                loginIntent.putExtra(TUTKManager.TYPE, 0);
//                startActivity(loginIntent);

//                Intent mIntent = new Intent(getActivity(), CameraActivity.class);
//                mIntent.putExtra(TUTKManager.TUTK_UID, info.getUID());
//                Log.i("wz","get info UID "+info.getUID());
//                login(info,mIntent);
                TUTKDevice info = TUTKManager.getByUID(GlobalValue.deviceUID);
                Intent localIntent = new Intent();
                localIntent.putExtra(TUTKManager.TUTK_UID, info.getUID());
                localIntent.putExtra(TUTKManager.TYPE, 0);
                new DeviceLoginDialog(getActivity(), localIntent, null).show();
            }
        });
//        btn_addcamera = (Button)view.findViewById(R.id.btn_cam_add);
//        btn_addcamera.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                 DeviceListDialog dialog = new DeviceListDialog(getActivity());
//                 dialog.show();
//            }
//        });
        btn_cam_share = ((Button)view.findViewById(R.id.btn_cam_share));
        btn_cam_share.setOnClickListener(this);
        mLoginView= new LoadingView(getActivity(), "正在连接");
        refreshDeviceList();
        return view;
    }

    private void refreshDeviceList()
    {
        if (UserManager.getUserDeviceList() != null)
        {
            layout_cam_tips.setVisibility(View.GONE);
            layout_cam_preview.setVisibility(View.VISIBLE);
        }
        else
        {
            layout_cam_tips.setVisibility(View.VISIBLE);
            layout_cam_preview.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.btn_cam_share:
                new QRCodeDialog(getActivity()).show();
                return;
        }
    }

    class LoginThread extends Thread{
        TUTKDevice device;
        Intent successIntent;
        private boolean cancle=false;
        public LoginThread(TUTKDevice device,Intent intent) {
            this.device = device;
            this.successIntent= intent;
        }

        @Override
        //setting timeout thread for async task
        public void run() {
            super.run();
            try {
                boolean ret = device.login();

                if (ret == true && !cancle) {
                    if (mLoginView != null && mLoginView.isShowing() && getActivity() != null && !getActivity().isFinishing()) {
                        mLoginView.dismiss();
                    }
                    startActivity(successIntent);
                       /* getActivity().runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getContext(), R.string.device_list_fragment_connet_error, Toast.LENGTH_SHORT).show();
                                if(mLoginView != null && mLoginView.isShowing() && getActivity() != null && !getActivity().isFinishing()) {
                                    mLoginView.dismiss();
                                }
                            }
                        });*/
                }
                else
                {
                    Log.i("wz","login failed");
                }
            } catch (Exception e) {

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {

                        if (mLoginView != null && mLoginView.isShowing() && getActivity() != null && !getActivity().isFinishing()) {
                            Toast.makeText(getContext(), R.string.device_list_fragment_connet_error, Toast.LENGTH_SHORT).show();
                            mLoginView.dismiss();
                        }
                    }
                });
            }
        }
        public void cancle(){
            Log.d("ConnectThread","cancle");
            cancle=true;
        }
    }


    private void login(final TUTKDevice device, final Intent intent) {
        if (mLoginView != null && !mLoginView.isShowing()) {
            mLoginView.show();
        }
        final LoginThread loginThread=new LoginThread(device,intent);
        loginThread.start();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                loginThread.cancle();
//
//                if (mLoginView != null && mLoginView.isShowing() && getActivity() != null && !getActivity().isFinishing()) {
//                    Toast.makeText(getContext(), R.string.device_list_fragment_connet_error, Toast.LENGTH_SHORT).show();
//                    mLoginView.dismiss();
//                }
//            }
//        }, 2000);
    }

    public void httptest(String path)
    {
        JsonHttpResponseHandler jsonhandler = new JsonHttpResponseHandler(){

            @Override
            public void onSuccess(int statusCode, Header[] headers,JSONObject response) {
                super.onSuccess(statusCode, headers, response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

        };
        try {
            String Upath = URLEncoder.encode(path, "utf-8");
            JSONObject postData;
            postData = new JSONObject();
            postData.put("command", 0xF01);
            postData.put("path", Upath);
            StringEntity stringEntity = new StringEntity(postData.toString());
            HttpHub.postjson(getActivity(), HttpUtils.Connect(HttpUtils.HttpCMD.CLIENT_BROWSE), stringEntity, jsonhandler);
        }catch (Exception e)
        {

        }
    }

}

