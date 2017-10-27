package com.rockchip.tutk.utils;

import java.util.Arrays;

import android.util.Log;

public class L  
{  
  
    private L()  
    {  
        /* cannot be instantiated */  
        throw new UnsupportedOperationException("cannot be instantiated");  
    }  
  
    public static boolean isDebug = true;// 是否�?要打印bug，可以在application的onCreate函数里面初始�?  
    private static final String TAG = "HomeCloudClient";  
    private static final String SIGN1 = "nathan === ";  
    private static final String SIGN2 = "wangz === "; 
    private static final String SIGN3 = "backgroung === "; 
    private static final String SIGN4 = "http === "; 
    private static final int[] arrayLogFilter = {};
    
    private static boolean LogLevleFilter(int level)
    {
    	if(Arrays.asList(arrayLogFilter).contains(level))
    		return false;
    	else
    		return true;
    				
    }
    // 下面四个是默认tag的函�?  
    public static void i(int level,String msg)  
    {  
        if (isDebug&&LogLevleFilter(level))  
        {
        	String sign = setSign(level);
    		Log.i(TAG, sign+msg);  
        }
            
    }  
  
    public static void d(int level,String msg)  
    {  
        if (isDebug&&LogLevleFilter(level))  
        {
        	String sign = setSign(level);
    		Log.d(TAG, sign+msg); 
        } 
    }  
  
    public static void e(int level,String msg)  
    {  
    	String sign = setSign(level);
		Log.e(TAG, sign+msg);
    }  
  
    public static void v(int level,String msg)  
    {  
    	String sign = setSign(level);
		Log.v(TAG, sign+msg);
    }  
  
    // 下面是传入自定义tag的函�?  
    public static void i(String tag, String msg)  
    {  
        if (isDebug)  
            Log.i(tag, msg);  
    }  
  
    public static void d(String tag, String msg)  
    {  
        if (isDebug)  
            Log.i(tag, msg);  
    }  
  
    public static void e(String tag, String msg)  
    {  
        if (isDebug)  
            Log.i(tag, msg);  
    }  
  
    public static void v(String tag, String msg)  
    {  
        if (isDebug)  
            Log.i(tag, msg);  
    }  
    
    public static String setSign(int i)
    {
    	String sign = null;
    	switch(i)
    	{
    	case 1:
    		sign = SIGN1;
    		break;
    	case 2:
    		sign = SIGN2;
    		break;
    	case 3:
    		sign = SIGN3;
    		break;
    	case 4:
    		sign = SIGN4;
    		break;
    	}
    	return sign;
    }
} 