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
public class AdminListAdapter extends BaseAdapter{
//	private String[] mMenuStr = { "分享设备", "应用管理","应用下载", "回收站", "设置", "作为远程摄像头", "作为远程监控器" };  
	private String[] mMenuStr = { "论坛", "设置","分享","关于"};
	private int[] mDrawable = {R.drawable.admin_share,R.drawable.admin_setting,R.drawable.admin_app,R.drawable.admin_app};
	private Context mContext;
	private TextView menuimg;
	private TextView titleimg;
	private TextView text; 
	public AdminListAdapter(Context context) {
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
        	convertView = LayoutInflater.from(mContext).inflate(R.layout.adapter_adminlist, null); 
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

		text.setTextColor(Color.parseColor("#ffffff"));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			menuimg.setBackground(mContext.getResources().getDrawable(R.drawable.admin_enter_normal));
		} else {
			menuimg.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.admin_enter_normal));
		}

		return convertView;
	}
}
