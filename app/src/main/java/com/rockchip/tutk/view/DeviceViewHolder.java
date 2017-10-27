package com.rockchip.tutk.view;

import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKSession;
import com.tutk.IOTC.AVAPIs;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_START;


public class DeviceViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    ;
    String TAG = "DeviceViewHolder";

    private TextView deviceName, deviceAddress, txt_status, txt_right;
    private ImageView camera_settings;
    private final View right_layout;
    private OnDeviceSelectedListener serviceSelectedListener;
    private TUTKDevice info;

    AsyncTask<TUTKDevice, Void, TUTKSession> getSession = new AsyncTask<TUTKDevice, Void, TUTKSession>() {
        @Override
        protected TUTKSession doInBackground(TUTKDevice... tutkDevices) {
            if (tutkDevices == null) {
                return null;
            }
            if (tutkDevices.length < 0) {
                return null;
            }
            TUTKDevice tutkDevice = tutkDevices[0];
            if (tutkDevice == null) {
                return null;
            }
            try {
                TUTKSession session = tutkDevice.getSession();
                return session;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    };

    public DeviceViewHolder(View itemView, OnDeviceSelectedListener serviceSelectedListener) {
        super(itemView);
        this.serviceSelectedListener = serviceSelectedListener;
        deviceName = (TextView) itemView.findViewById(R.id.txtDevceName);
        deviceAddress = (TextView) itemView.findViewById(R.id.txtDeviceType);
        camera_settings = (ImageView) itemView.findViewById(R.id.camera_settings);
        txt_status = (TextView) itemView.findViewById(R.id.txt_status);
        txt_status.setVisibility(View.INVISIBLE);
        txt_right = (TextView) itemView.findViewById(R.id.right_txt);
        right_layout = itemView.findViewById(R.id.right_layout);
        itemView.setOnClickListener(this);
        right_layout.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void bindTo(@NonNull TUTKDevice info) {
        this.info = info;
        //String modeFrom = getModeFrom(info);
        deviceName.setText(String.format("%s", info.getDeviceInfo().getDeviceName()));
        /*if (info.isOnLine()){
            deviceAddress.setText(String.format("%s:%s", getAddressFrom(info), getPortFrom(info)));
            txt_right.setText(R.string.device_view_setting);
            camera_settings.setImageResource(R.drawable.wrench);
            txt_status.setVisibility(View.INVISIBLE);
        }else {
            deviceAddress.setText("N/A");
            txt_status.setText(R.string.device_view_status);
            txt_status.setVisibility(View.VISIBLE);
            txt_right.setText(R.string.device_view_retry);
            camera_settings.setImageResource(R.drawable.refresh);
        }*/

        /*String statusFrom = getStatusFrom(info);
        txt_status.setText(statusFrom);
        if ("online".equals(statusFrom)){
            txt_status.setTextColor(Color.BLUE);
        }else{
            txt_status.setTextColor(Color.RED);
        }*/
    }

    private String getAddressFrom(@NonNull TUTKDevice info) {
        String address;
        TUTKSession session = info.getSession();
        if (session != null) {
            address = session.getIP();
        } else {
            address = "N/A";
        }

        return address;
    }

    private String getStatusFrom(@NonNull TUTKDevice info) {
        int avIndex = -1;
        String status;
        TUTKSession session = info.getSession();
        if (session != null) {
            avIndex = session.getAVIndex();
            if (avIndex < 0) {
                status = "offline";
            } else {
                status = "online";
            }

        } else {
            status = "online";
        }

        return status;
    }


    private String getUIDFrom(@NonNull TUTKDevice info) {
        String uid;

        try {
            Object o = new AsyncTask() {
                @Override
                protected Object doInBackground(Object[] objects) {
                    return null;
                }
            }.execute().get(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        TUTKSession session = info.getSession();
        if (session != null) {
            uid = session.getUID();
        } else {
            uid = "unknow";
        }
        return uid;
    }

    private String getModeFrom(@NonNull TUTKDevice info) {
        String mode;
        TUTKSession session = info.getSession();
        if (session != null) {
            mode = session.getMode();
        } else {
            mode = "";
        }

        return mode;
    }

    private int getPortFrom(@NonNull TUTKDevice info) {
        int port = -1;
        TUTKSession session = info.getSession();
        if (session != null) {
            port = session.getPort();
        } else {
            port = -1;
        }

        return port;
    }

    @Override
    public void onClick(View v) {
        /*if (!info.isOnLine()){
            bindTo(info);
            if (!info.isOnLine()){
                serviceSelectedListener.deviceOffline(info);
            }
            return;
        }*/

        synchronized (serviceSelectedListener) {
            if (v.getId() == R.id.right_layout) {
                serviceSelectedListener.onDeviceSettingClick(info);

            } else {
                serviceSelectedListener.onDeviceSelected(info);
            }
        }
    }

    @Override
    public boolean onLongClick(View view) {
        serviceSelectedListener.onDeviceDelete(info);
        return true;
    }
}
