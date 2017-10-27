package com.rockchip.tutk.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.rockchip.tutk.R;
import com.rockchip.tutk.http.HttpHub;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.alibaba.fastjson.JSONObject;
import com.rockchip.tutk.utils.UserManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterDialog extends Dialog implements View.OnClickListener{
	private Context mContext;
	private EditText edit_phone,edit_verification,edit_passwd;
	private TextView btn_clear,btn_send,btn_visible,btn_report;
	private String mVerificationCode = "123123";
	private LinearLayout layout_return;
	public RegisterDialog(Context context) {
		super(context, R.style.AppTheme);
		mContext = context;

	}
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_register);
		edit_phone = (EditText)findViewById(R.id.txt_register_phonenum);
		edit_verification = (EditText)findViewById(R.id.txt_register_verificationcode);
		edit_passwd = (EditText)findViewById(R.id.txt_register_pw);
		btn_clear = (TextView)findViewById(R.id.btn_register_clear);
		btn_send = (TextView)findViewById(R.id.txt_register_verificationtips);
		btn_visible = (TextView)findViewById(R.id.btn_register_visible);
		btn_report = (TextView)findViewById(R.id.btn_register_report);
		layout_return = (LinearLayout)findViewById(R.id.btn_title_return) ;
		layout_return.setOnClickListener(this);
		btn_clear.setOnClickListener(this);
		btn_send.setOnClickListener(this);
		btn_visible.setOnClickListener(this);
		btn_report.setOnClickListener(this);
	}
	
	 
	@Override
	public void onClick(View v)
	{
		int id = v.getId();
		switch (id) {
			case R.id.btn_register_clear: {
				//sendGetBind();
				break;
			}
			case R.id.txt_register_verificationtips: {
				String phonenum = getEdittextPhoneNum();
				if(phonenum.length() != 11)
					Toast.makeText(mContext, "请输入11位电话号码", Toast.LENGTH_SHORT).show();
				else
					sendVerificationCode(phonenum,mVerificationCode);
				break;
			}
			case R.id.btn_register_visible: {
				//sendUnBind();
				//sendReportStatus();
				//sendReportBind();
				break;
			}
			case R.id.btn_title_return:
			{
				this.dismiss();
				break;
			}
			case R.id.btn_register_report: {
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
					sendRegisterReport(phonenum,pwd);
				//sendLoginData();
				//sendModifyInfo();
				//sendModifyPasswd();
				//sendReportBind();
				//sendModifyDeviceInfo();
				break;
			}
		}
	}

	public String getEdittextPhoneNum()
	{
		String str = ((EditText)findViewById(R.id.txt_register_phonenum)).getText().toString();
		return str;
	}

	public String getEdittextPasswd()
	{
		String str = ((EditText)findViewById(R.id.txt_register_pw)).getText().toString();
		return str;
	}
	public String getEdittextVerificationCode()
	{
		String str = ((EditText)findViewById(R.id.txt_register_verificationcode)).getText().toString();
		return str;
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
	public void sendRegisterReport(String phonenum,String pwd)
	{
		JsonHttpResponseHandler jsonhandler = new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(int statusCode, Header[] headers, org.json.JSONObject response) {
				super.onSuccess(statusCode, headers, response);
				Log.i("wz"," json data is "+response+"; statusCode is "+statusCode);
				try {
					String resultMsg = response.getString("rsMsg");
					String rsCode = response.getString("rsCode");
					Toast.makeText(mContext, resultMsg, Toast.LENGTH_SHORT).show();
					if(rsCode.equals("1000")) {
						String token = response.getString("token");
						org.json.JSONObject json =  response.getJSONObject("user");
						UserManager.saveUserData(json);
						RegisterDialog.this.dismiss();
					}
				} catch (org.json.JSONException e) {
					Log.i("wz"," onFailure  "+e);
				}
			}
			@Override
			public void onFailure(int statusCode, Header[] headers,
								  Throwable throwable, org.json.JSONObject errorResponse) {
				super.onFailure(statusCode, headers, throwable, errorResponse);
				Log.i("wz"," onFailure  errorResponse "+errorResponse+"; statusCode is "+statusCode);
			}
		};
		try {
			JSONObject postData;
			ObjectMapper objmapper = new ObjectMapper();
			postData = new JSONObject();
			postData.put("regFlag",1);
			postData.put("phone", phonenum);
			postData.put("password",pwd);
			postData.put("regPostion", "ShenZhen");
			Map<String, String> map=new HashMap<String, String>();
			map.put("model", "ios");
			map.put("brand", "苹果");
			map.put("os", "ios");
			map.put("ver", "10.3");
			map.put("inch", "5.5");
			postData.put("phoneInfo",map);
			StringEntity stringEntity = new StringEntity(objmapper.writeValueAsString(postData),"UTF-8");
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/usr/registerAccount", stringEntity, jsonhandler);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendLoginData() {
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
						Log.i("wz", "token is " + token);
					}
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
			postData.put("phone", "18682180714");
			postData.put("password", "Wz123456");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/usr/userLogin", stringEntity, jsonhandler);
		}  catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
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

	public void sendModifyPasswd() {
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
			postData.put("password", "Wz654321");
			postData.put("token", "oiHZuwvntK3z6sqOiWO");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/usr/updatePwd", stringEntity, jsonhandler);
		}  catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void sendReportBind() {
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
			postData.put("did", "t96800000002");
			postData.put("deviceName", "t96800000002");
			postData.put("token", "oiHZuwvntK3z6sqOiWO");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/device/reportBind", stringEntity, jsonhandler);
		}  catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}


	public void sendUnBind() {
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
		ObjectMapper objmapper = new ObjectMapper();
		JSONObject postData;
		postData = new JSONObject();
		String[] str = {"t96800000002","t96800000002"};
		try {
			postData.put("userId", "1000100003");
			postData.put("dids", str);
			postData.put("token", "oiHZuwvntK3z6sqOiWO");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/device/unBind", stringEntity, jsonhandler);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void sendGetBind() {
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
					if(rsCode.equals("2000"))
					{
						org.json.JSONArray bindList = response.getJSONArray("bindList");
						List<Map> didlist = JSONObject.parseArray(bindList.toString(), Map.class);
						for(int i =0;i<didlist.size();i++)
						{
							Map m = didlist.get(i);
							Log.i("wz","did "+m.get("did")+" ;deviceName "+m.get("deviceName"));
						}
					}
				} catch (org.json.JSONException e) {
					Log.i("wz","JSONException  "+e);
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
			postData.put("token", "oiHZuwvntK3z6sqOiWO");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/device/getUserDeviceBindList", stringEntity, jsonhandler);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendReportStatus() {
		JsonHttpResponseHandler jsonhandler = new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(int statusCode, Header[] headers, org.json.JSONObject response) {
				super.onSuccess(statusCode, headers, response);
				Log.i("wz"," json data is "+response+"; statusCode is "+statusCode);
				String resultMsg = null;
				try {
					resultMsg = response.getString("rsMsg");
					String rsCode = response.getString("rsCode");
				} catch (org.json.JSONException e) {
					Log.i("wz","JSONException  "+e);
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
			postData.put("did", "t96800000002");
			postData.put("status", "1");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/device/reportDeviceOnlineStatus", stringEntity, jsonhandler);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendModifyDeviceInfo() {
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
			postData.put("did", "t96800000002");
			postData.put("deviceName", "t96800000002-0001");
			Log.i("wz","postData "+postData.toString());
			StringEntity stringEntity = new StringEntity(postData.toString());
			HttpHub.postjson(mContext,"http://cmsapi.tclcs.cn:8090/cms/device/updateDeviceInfo", stringEntity, jsonhandler);
		}  catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
