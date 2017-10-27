package com.rockchip.tutk.http;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;

import android.app.Activity;
import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
 
public class HttpHub {
    private static final String BASE_URL = "http://192.168.17.99/";
 
    private static AsyncHttpClient client = new AsyncHttpClient();
 
    public static void setTimeout(){
        client.setTimeout(60000);
    }
 
    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }
 
    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }
 
    public static void postjson(Context context,String url, HttpEntity entity, AsyncHttpResponseHandler responseHandler) 
    {
    	client.post(context, url, entity, "application/json", responseHandler);
    }
    public static void download(String url, RequestParams params, FileAsyncHttpResponseHandler fileAsyncHttpResponseHandler){
        client.get(getAbsoluteUrl(url), params, fileAsyncHttpResponseHandler);
    }
 
    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}

