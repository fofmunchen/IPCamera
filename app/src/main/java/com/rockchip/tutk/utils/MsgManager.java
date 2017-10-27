package com.rockchip.tutk.utils;

import android.os.Handler;
import android.os.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wangzheng on 2017/7/27.
 */

public class MsgManager {
    public static HashMap<String , Handler> map = new HashMap<String , Handler>();
    public static void registerHandler(Handler handler, String name)
    {
        map.put(name,handler);
    }

    public static void sendToHandler(String name, Message msg)
    {
        map.get(name).sendMessage(msg);
    }
}
