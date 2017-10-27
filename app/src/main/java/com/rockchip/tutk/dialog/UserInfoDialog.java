package com.rockchip.tutk.dialog;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rockchip.tutk.constants.GlobalValue;
import com.rockchip.tutk.R;
import com.alibaba.fastjson.JSONObject;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.rockchip.tutk.http.HttpHub;
import com.rockchip.tutk.utils.UserManager;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

public class UserInfoDialog extends Dialog implements View.OnClickListener{
	private Context mContext;
	private LinearLayout layout_return;
	public UserInfoDialog(Context context) {
		super(context, R.style.AppTheme);
		mContext = context;

	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_userinfo);
		((TextView)findViewById(R.id.txt_username)).setText(GlobalValue.userNickname);
		((Button)findViewById(R.id.btn_logout)).setOnClickListener(this);
		layout_return = (LinearLayout)findViewById(R.id.btn_title_return) ;
		layout_return.setOnClickListener(this);
	}
	
	 
	@Override
	public void onClick(View v)
	{
		int id = v.getId();
		switch (id) {
			case R.id.btn_title_return: {
				this.dismiss();
				break;
			}
			case R.id.btn_logout:
			{
				if(UserManager.logout()) {
					GlobalValue.deviceUID = null;
					GlobalValue.userId = null;
					GlobalValue.userNickname = null;
					GlobalValue.userPhone = null;
					Intent intent = ((Activity)mContext).getIntent();
					((Activity) this.mContext).finish();
					this.mContext.startActivity(intent);
				}
				break;
			}
		}
	}


    @Override
	public void onBackPressed() {
        this.dismiss();
	}


	public void sendModifyUserInfo() {
		JsonHttpResponseHandler jsonhandler = new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(int statusCode, Header[] headers, org.json.JSONObject response) {
				super.onSuccess(statusCode, headers, response);
				Log.i("wz"," json data is "+response+"; statusCode is "+statusCode);
				String resultMsg = null;
				try {
					resultMsg = response.getString("rsMsg");
					String rsCode = response.getString("rsCode");
					Log.i("wz"," json resultMsg is "+resultMsg+"; rsCode is "+rsCode);
				} catch (org.json.JSONException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void onFailure(int statusCode, Header[] headers,
								  Throwable throwable, org.json.JSONObject errorResponse) {
				super.onFailure(statusCode, headers, throwable, errorResponse);
				Log.i("wz"," onFailure  errorResponse "+errorResponse+"; statusCode is "+statusCode);
			}
		};
		JSONObject postData;
		postData = new JSONObject();
		try {
			postData.put("userId", "1000100003");
			postData.put("nickname", "wz");
			postData.put("signature", "test");
			postData.put("sex", "1");
			postData.put("token", "oiHZuwvntK3z6sqOiWO");
			postData.put("phone", "18682180714");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/usr/updateUserInfo", stringEntity, jsonhandler);
		}  catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}


}
