package com.rockchip.tutk.fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


import com.rockchip.tutk.R;
import com.rockchip.tutk.utils.L;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;


public class FragmentShare extends Fragment
{
    WebView mWebview;
    WebSettings mWebSettings;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_share, null);
        mWebview = (WebView)view.findViewById(R.id.webView);
        mWebSettings = mWebview.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        // 设置允许JS弹窗
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        mWebSettings.setBuiltInZoomControls(true);// 隐藏缩放按钮
        mWebSettings.setUseWideViewPort(true);// 可任意比例缩放
        mWebSettings.setLoadWithOverviewMode(true);// setUseWideViewPort方法设置webview推荐使用的窗口。setLoadWithOverviewMode方法是设置webview加载的页面的模式。
        mWebSettings.setSavePassword(true);
        mWebSettings.setSaveFormData(true);// 保存表单数据
        mWebSettings.setDomStorageEnabled(true);
        mWebSettings.setSupportMultipleWindows(true);// 新加
        mWebSettings.setPluginState(WebSettings.PluginState.ON);
        mWebview.loadUrl("http://www.tclcs.cn:8080/camera/discover.do ");
//        mWebview.post(new Runnable() {
//            @Override
//            public void run() {
//
//                // 注意调用的JS方法名要对应上
//                // 调用javascript的callJS()方法
//                mWebview.loadUrl("http://www.sina.com/");
//            }
//        });
        //设置不用系统浏览器打开,直接显示在当前Webview
        mWebview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        //设置WebChromeClient类
        mWebview.setWebChromeClient(new WebChromeClient() {
            //获取网站标题
            @Override
            public void onReceivedTitle(WebView view, String title) {
                System.out.println("标题在这里");
              //  Log.i("wz","webview title is "+ title);
            }
            //获取加载进度
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    String progress = newProgress + "%";
                   // Log.i("wz","webview title is "+ progress);
                } else if (newProgress == 100) {
                    String progress = newProgress + "%";
                 //   Log.i("wz","webview title is "+ progress);
                }
            }
        });
        //设置WebViewClient类
        mWebview.setWebViewClient(new WebViewClient() {
            //设置加载前的函数
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {

            //    Log.i("wz","webview 开始加载了 ");
            }

            //设置结束加载函数
            @Override
            public void onPageFinished(WebView view, String url) {
         //       Log.i("wz","webview 结束加载了 ");
            }
        });
        return view;
    }

    @Override
    public void onPause() {
        Log.i("wz","fragement pause");
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        //销毁Webview
        if (mWebview != null) {
                mWebview.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
                mWebview.clearHistory();
                ((ViewGroup) mWebview.getParent()).removeView(mWebview);
                mWebview.destroy();
                mWebview = null;
        }
        super.onDestroyView();
    }
}