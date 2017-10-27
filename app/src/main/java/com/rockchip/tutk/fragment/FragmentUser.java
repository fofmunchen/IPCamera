package com.rockchip.tutk.fragment;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.RecoverySystem;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.rockchip.tutk.activity.PhotoViewActivity;
import com.rockchip.tutk.constants.Constants;
import com.rockchip.tutk.constants.GlobalValue;
import com.rockchip.tutk.R;
import com.rockchip.tutk.activity.BbsActivity;
import com.rockchip.tutk.activity.SettingActivity;
import com.rockchip.tutk.adapter.AdminListAdapter;
import com.rockchip.tutk.dialog.QRCodeDialog;
import com.rockchip.tutk.dialog.UserInfoDialog;
import com.rockchip.tutk.utils.CustomMultipartEntity;
import com.rockchip.tutk.utils.UserManager;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;


public class FragmentUser extends Fragment implements View.OnClickListener,AdapterView.OnItemClickListener
{
    private ListView mMenuList;
    private TextView txt_admin_name;
    private ImageView img_admin_pic;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_user, null);
        txt_admin_name = (TextView)view.findViewById(R.id.txt_admin_name);
        if(GlobalValue.userNickname != null )
            txt_admin_name.setText(GlobalValue.userNickname);
        else
            txt_admin_name.setText("unknow");
        img_admin_pic = (ImageView)view.findViewById(R.id.img_admin_pic);
        img_admin_pic.setOnClickListener(this);
    /*    new Thread(new Runnable() {
            @Override
            public void run() {
                WXTools.LoadImageFromWebOperationsWithHandler(WXTools.headimgurl,MyHandler);
            }
        }).start();*/
        mMenuList = (ListView)view.findViewById(R.id.list_admin_menulist);
        mMenuList.setOnItemClickListener(this);
        AdminListAdapter adapter = new AdminListAdapter(getActivity());
        mMenuList.setAdapter(adapter);
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.img_admin_pic:
                if(UserManager.getUserLoginStatus() == Constants.LoginStatus.OK) {
                    UserInfoDialog dialog = new UserInfoDialog(getActivity());
                    dialog.show();
                    break;
                }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0: {
                Intent mIntent = new Intent(getActivity(), BbsActivity.class);
                getActivity().startActivity(mIntent);
                break;
            }
            case 1: {
//                Intent mIntent = new Intent(getActivity(), SettingActivity.class);
//                getActivity().startActivity(mIntent);
                break;
            }
            case 2:{
//                QRCodeDialog dialog = new QRCodeDialog(getActivity());
//                dialog.show();
                break;
            }
            case 3: {
                new Thread(){
                    @Override
                    public void run() {
                        try {
                            String url = "http://cmsapi.tclcs.cn:8080/camera/upload.action?uid=1000100002";
                            HttpPost httpPost = new HttpPost(url);
                            HttpClient httpClient = new DefaultHttpClient();
                            HttpContext httpContext = new BasicHttpContext();
                            File file = new File("/sdcard/DCIM/Camera/IMG20170428104257.jpg");
                            long totalSize = file.length();
                            String serverResponse = null;
                            //自定义一个文件实体，继承MultipartEntity即可完成对文件进行form表单封装，自行增强进度条功能。
                            final long finalTotalSize = totalSize;
                            CustomMultipartEntity multipartContent = new CustomMultipartEntity(new CustomMultipartEntity.ProgressListener() {
                                @Override
                                public void transferred(long num) {
                                    int process = (int) ((num /(float) finalTotalSize) * 100);
                                    Log.i("wz", "process is " + process+";num is "+num+";finalTotalSize is "+finalTotalSize);
                                }
                            });
			                /*MultipartEntity multipartContent = new MultipartEntity();*/

                            totalSize = multipartContent.getContentLength();
                            multipartContent.addPart("uploadFile", new FileBody(file));
                            httpPost.setEntity(multipartContent);
                            HttpResponse response = httpClient.execute(httpPost, httpContext);
                            serverResponse = EntityUtils.toString(response.getEntity()); //得到服务器返回的响应数据
                            System.out.println(serverResponse);
                            int statusCode = response.getStatusLine().getStatusCode();
                            if (statusCode == HttpStatus.SC_OK) {
                                Log.i("wz", "上传成功");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        super.run();
                    }
                }.start();
            }
        }
    }
}