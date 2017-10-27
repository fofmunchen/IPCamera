package com.rockchip.tutk.dialog;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.rockchip.tutk.R;
import com.rockchip.tutk.http.HttpHub;
import com.rockchip.tutk.utils.UserManager;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;

@SuppressLint("NewApi")
public class PhoneLoginDialog extends Dialog implements View.OnClickListener{
	private Context mContext;
	private String mVerificationCode = "123321";
	private LinearLayout layout_return;
	public PhoneLoginDialog(Context context) {
		super(context, R.style.AppTheme);
		mContext = context;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_phonelogin);
		((TextView)findViewById(R.id.btn_phonelogin_report)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String code = getEdittextVerificationCode();
				String phonenum = getEdittextPhoneNum();
				String pwd = getEdittextPasswd();
				if(code == null)
					Toast.makeText(mContext, "验证码不能为空", Toast.LENGTH_SHORT).show();
				if(!code.equals(mVerificationCode))
					Toast.makeText(mContext, "请输入正确的验证码", Toast.LENGTH_SHORT).show();
				if(phonenum.length() != 11)
					Toast.makeText(mContext, "请输入11位电话号码", Toast.LENGTH_SHORT).show();
				if(pwd.length() < 6)
					Toast.makeText(mContext, "请输入6位以上密码", Toast.LENGTH_SHORT).show();
				else
					sendLoginData(phonenum,pwd);
			}
		});
		((TextView)findViewById(R.id.btn_phonelogin_visible)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		((TextView)findViewById(R.id.txt_register_verificationtips)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String phonenum = getEdittextPhoneNum();
				if(phonenum.length() != 11)
					Toast.makeText(mContext, "请输入11位电话号码", Toast.LENGTH_SHORT).show();
				else
					sendVerificationCode(phonenum,mVerificationCode);
			}
		});
		layout_return = (LinearLayout)findViewById(R.id.btn_title_return) ;
		layout_return.setOnClickListener(this);
	}
	
	public String getEdittextPhoneNum()
	{
		String str = ((EditText)findViewById(R.id.txt_phonelogin_phonenum)).getText().toString();
		return str;
	}

	public String getEdittextPasswd()
	{
		String str = ((EditText)findViewById(R.id.txt_phonelogin_pw)).getText().toString();
		return str;
	}
	public String getEdittextVerificationCode()
	{
		String str = ((EditText)findViewById(R.id.txt_phonelogin_verificationcode)).getText().toString();
		return str;
	}
	@Override
	public void onClick(View v) {
		PhoneLoginDialog.this.dismiss();
		switch(v.getId())
		{
			case R.id.btn_title_return: {
				this.dismiss();
				break;
			}
		}
	}
	@Override
	public void onBackPressed() {
		this.dismiss();
	}

	public void sendVerificationCode(String phonenum,String code) {
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
			postData.put("phone", phonenum);
			postData.put("vftCode", code);
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/usr/acquiredVerificationCode", stringEntity, jsonhandler);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void sendLoginData(String phonenum,String pwd) {
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
					if(rsCode.equals("1000")) {
						String token = response.getString("token");
						org.json.JSONObject json =  response.getJSONObject("user");
						UserManager.saveUserData(json);
						PhoneLoginDialog.this.dismiss();
					}
					else if(rsCode.equals("1002"))
						Toast.makeText(mContext, resultMsg, Toast.LENGTH_SHORT).show();
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
			postData.put("phone", phonenum);
			postData.put("password",pwd);
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/usr/userLogin", stringEntity, jsonhandler);
		}  catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
