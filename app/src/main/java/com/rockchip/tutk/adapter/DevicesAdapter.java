package com.rockchip.tutk.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.v4.util.ArraySet;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.view.DeviceViewHolder;
import com.rockchip.tutk.view.OnDeviceSelectedListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class DevicesAdapter extends RecyclerView.Adapter<DeviceViewHolder> {
    private List<TUTKDevice> devices = new ArrayList<>();
    private OnDeviceSelectedListener deviceSelectedListener;
    Context mContext;
    SharedPreferences sharedPreferences;
    Set<String>  uidSet= new ArraySet<String>();

    public DevicesAdapter(OnDeviceSelectedListener deviceSelectedListener,Context context) {
        this.deviceSelectedListener = deviceSelectedListener;
        this.mContext=context;
        sharedPreferences = mContext.getSharedPreferences("device", Context.MODE_APPEND);
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new DeviceViewHolder(inflater.inflate(R.layout.device_item_layout, parent, false), deviceSelectedListener);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.bindTo(devices.get(position));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void addDevice(@NonNull TUTKDevice service) {
        if (devices.contains(service)){
            return;
        }
        devices.add(service);
        notifyItemInserted(devices.size() - 1);
        notifyDataSetChanged();
        uidSet.add(service.getUID());
        sharedPreferences.edit().putStringSet("UID",uidSet).apply();
    }

    public void removeDevice(@NonNull TUTKDevice service) {
        int foundIndex = -1;
        int listSize = devices.size();

        for (int i = 0; i < listSize && (foundIndex == -1); i++) {
            if (devices.get(i).equals(service)) {
                foundIndex = i;
            }

        }

        devices.remove(foundIndex);
        notifyItemRemoved(foundIndex);
        notifyDataSetChanged();
        uidSet.remove(service.getUID());
        sharedPreferences.edit().putStringSet("UID",uidSet).apply();
    }
}
