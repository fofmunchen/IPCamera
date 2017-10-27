package com.rockchip.tutk.adapter;



import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.rockchip.tutk.R;

@SuppressLint("NewApi")
public class DeviceListAdapter extends BaseAdapter{
//	private String[] mMenuStr = { "分享设备", "应用管理","应用下载", "回收站", "设置", "作为远程摄像头", "作为远程监控器" };  
	private String[] mMenuStr = { "TCL智能摄像头1", "TCL智能摄像头2", "TCL智能摄像头3", "TCL智能摄像头4"};
	private int[] mDrawable = {R.drawable.camera1,R.drawable.camera2,R.drawable.camera3,R.drawable.camera4};
	private Context mContext;
	private TextView menuimg;
	private TextView titleimg;
	private TextView text; 
	public DeviceListAdapter(Context context) {
		mContext = context; 
	}
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return mMenuStr.length;
	}
	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) { 
        	convertView = LayoutInflater.from(mContext).inflate(R.layout.adapter_devicelist, null);
        	menuimg = (TextView) convertView.findViewById(R.id.menuimg);  
        	titleimg = (TextView) convertView.findViewById(R.id.titleimg);  
        	text =(TextView) convertView.findViewById(R.id.menuname);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        	titleimg.setBackground(mContext.getResources().getDrawable(mDrawable[position]));
        } else {
        	titleimg.setBackgroundDrawable(mContext.getResources().getDrawable(mDrawable[position]));
        }
        text.setText(mMenuStr[position]);
		return convertView;
	}
}
