package com.rockchip.tutk.utils;

import android.content.Context;
import android.os.Message;
import android.util.Log;

import com.rockchip.tutk.constants.Constants;
import com.rockchip.tutk.constants.GlobalValue;

import org.json.JSONException;

import java.util.Set;

/**
 * Created by wangzheng on 2017/7/26.
 */

public class UserManager {
    public static Context mContext;
    public static void init(Context context)
    {
        mContext = context;
        GlobalValue.userId = SharedPreference.getString(mContext, "user", "currentuser", null);
        if (GlobalValue.userId != null)
        {
            GlobalValue.userPhone = SharedPreference.getString(mContext, GlobalValue.userId, "phone", null);
            GlobalValue.userNickname = SharedPreference.getString(mContext, GlobalValue.userId, "nickname", null);
        }
    }
    public static int getUserLoginStatus()
    {
        if (SharedPreference.getString(mContext, "user", "currentuser", null) != null)
            return Constants.LoginStatus.OK;
        else
            return  Constants.LoginStatus.NONE;
    }

    public static void saveUserData(org.json.JSONObject paramJSONObject )
    {
        try
        {
            Log.i("wz", " userId  is " + paramJSONObject.getString("userId") + "; phone is " + paramJSONObject.getString("phone") + " ;nickName is " + paramJSONObject.getString("nickname"));
            String str = paramJSONObject.getString("userId");
            SharedPreference.putString(mContext, "user", "currentuser", str);
            SharedPreference.putString(mContext, str, "phone", paramJSONObject.getString("phone"));
            SharedPreference.putString(mContext, str, "userId", paramJSONObject.getString("userId"));
            SharedPreference.putString(mContext, str, "nickname", paramJSONObject.getString("nickname"));
            GlobalValue.userId = str;
            GlobalValue.userPhone = paramJSONObject.getString("phone");
            GlobalValue.userNickname = paramJSONObject.getString("nickname");
            Message msg = new Message();
            msg.what = 0;
            MsgManager.sendToHandler("FragementCamera", msg);
            return;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    public static boolean logout()
    {
        return mContext.getSharedPreferences("user", Context.MODE_PRIVATE).edit().remove("currentuser").commit();
    }

    public static void saveUserDeviceList(String paramString)
    {
        Set localSet = null;
        if (GlobalValue.userId != null) {
            localSet = mContext.getSharedPreferences(GlobalValue.userId,  Context.MODE_PRIVATE).getStringSet("devicelist", null);
        }
        localSet.add(paramString);
        mContext.getSharedPreferences(GlobalValue.userId,  Context.MODE_PRIVATE).edit().putStringSet("devicelist", localSet).commit();
    }

    public static Set<String> getUserDeviceList()
    {
        Set localSet = null;
        if (GlobalValue.userId != null) {
            localSet = mContext.getSharedPreferences(GlobalValue.userId,  Context.MODE_PRIVATE).getStringSet("devicelist", null);
        }
        return localSet;
    }

}
