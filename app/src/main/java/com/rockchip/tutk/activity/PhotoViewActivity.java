package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.rockchip.tutk.R;
import com.rockchip.tutk.view.photoview.PhotoView;

import java.io.File;

public class PhotoViewActivity extends Activity implements View.OnClickListener{
    private PhotoView mPhotoView;
    private TextView txt_title;
    private String mFilePath;
    private LinearLayout layout_return;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photoview);
        mFilePath = getIntent().getStringExtra("path");
        mPhotoView = (PhotoView) findViewById(R.id.iv_photo);
        Drawable bitmap = Drawable.createFromPath(mFilePath);
        mPhotoView.setImageDrawable(bitmap);
        txt_title = (TextView)findViewById(R.id.txt_title);
        txt_title.setText((new File(mFilePath)).getName());
        layout_return = (LinearLayout)findViewById(R.id.btn_title_return) ;
        layout_return.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch(v.getId())
        {
            case R.id.btn_title_return: {
                PhotoViewActivity.this.finish();
                break;
            }
        }
    }
}
