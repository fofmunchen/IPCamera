package com.rockchip.tutk.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;


import com.rockchip.tutk.activity.PhotoViewActivity;
import com.rockchip.tutk.activity.VideoPlayerActivity;
import com.rockchip.tutk.utils.L;
import com.rockchip.tutk.R;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.rockchip.tutk.adapter.GalleryAdapter;
public class FragmentGallery extends Fragment
{
	private HashMap<String, List<String>> mGruopMap = new HashMap<String, List<String>>();
	private final static int SCAN_OK = 1;
	private ProgressDialog mProgressDialog;
	private GridView mGridView;
	private TextView txt_title_gallery,txt_title_photo;
	private ArrayList<String> mPathList = new ArrayList<String>();
	private ArrayList<MediaInfo> mMediaList = new ArrayList<MediaInfo>();
	private GalleryAdapter mAdapter;
	private static int UPLOADTYPE = 0;
	private String lock = "lock";
	public class MediaInfo
	{
		public String path;
		public String type;
		public MediaInfo(String path, String type) {
			this.path = path;
			this.type = type;
		}
	}
	private Handler mHandler = new Handler(){
		private final static int SCAN_OK = 1;
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {  
			case SCAN_OK:
				//�رս�����
				mProgressDialog.dismiss();
				Collections.reverse(mPathList);
				Collections.reverse(mMediaList);
				mAdapter = new GalleryAdapter(getActivity(), mMediaList);
				mGridView.setAdapter(mAdapter);
				break;
			}
		}
	}; 
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_gallery, null);
		mGridView = (GridView) view.findViewById(R.id.gird_upload_gallerystyle);
		mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				MediaInfo info = mMediaList.get(((int)id));
				if(info.type.equals("img")) {
					Intent mIntent = new Intent(getActivity(), PhotoViewActivity.class);
					Log.i("wz", "photo path is " + mPathList.get(((int) id)));
					mIntent.putExtra("path", info.path);
					getActivity().startActivity(mIntent);
				}
				else if(info.type.equals("video")){
					Intent mIntent = new Intent(getActivity(), VideoPlayerActivity.class);
					Log.i("wz", "photo path is " + mMediaList.get(((int) id)));
					mIntent.putExtra("path", info.path);
					getActivity().startActivity(mIntent);
				}
			}
		});
		txt_title_photo = (TextView)view.findViewById(R.id.txt_title_photo);
		txt_title_gallery = (TextView)view.findViewById(R.id.txt_title_gallery);
		UPLOADTYPE = getActivity().getIntent().getIntExtra("type", 1);
		if(UPLOADTYPE == 1)
		{	
			txt_title_photo.setTextColor(Color.parseColor("#ffffff"));
			txt_title_gallery.setTextColor(Color.parseColor("#0389c1"));
		}
		else if(UPLOADTYPE == 2)
		{	
			txt_title_photo.setTextColor(Color.parseColor("#0389c1"));
			txt_title_gallery.setTextColor(Color.parseColor("#ffffff"));
		}
		scanMedias();
        return view;
    }
    
    @Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		// TODO Auto-generated method stub
		super.onDestroyView();
	}

	/**
	 * ����ContentProviderɨ���ֻ��е�ͼƬ���˷��������������߳���
	 */
	private void scanMedias() {
		if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
			Toast.makeText(getActivity(), "�����ⲿ�洢", Toast.LENGTH_SHORT).show();
			return;
		}
		//��ʾ������
		mProgressDialog = ProgressDialog.show(getActivity(), null, "���ڼ���...");
		new Thread(new Runnable() {
			@Override
			public void run() {
					scanImage();
					scanVideo();
			}
		}).start();
	}
	private void scanVideo()
	{
		Uri mImageUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
		ContentResolver mContentResolver = getActivity().getContentResolver();
		Cursor mCursor = mContentResolver.query(mImageUri, null,null,null, MediaStore.Video.Media.DATE_MODIFIED);
		while (mCursor.moveToNext()) {
			String path = mCursor.getString(mCursor
					.getColumnIndex(MediaStore.Video.Media.DATA));
	//		File file = new File(path);
			MediaInfo info = new MediaInfo("file://" + path,"video");
			mPathList.add(path);
			L.i(2,"add file "+path);
			mMediaList.add(info);
		}
		mCursor.close();
		mHandler.sendEmptyMessage(SCAN_OK);
	}
	
	private void scanImage()
	{
		Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		ContentResolver mContentResolver = getActivity().getContentResolver();
		Cursor mCursor = mContentResolver.query(mImageUri, null,
				MediaStore.Images.Media.MIME_TYPE + "=? or "
						+ MediaStore.Images.Media.MIME_TYPE + "=?",
				new String[] { "image/jpeg", "image/png" }, MediaStore.Images.Media.DATE_MODIFIED);
		while (mCursor.moveToNext()) {
			String path = mCursor.getString(mCursor
					.getColumnIndex(MediaStore.Images.Media.DATA));
			MediaInfo info = new MediaInfo("file://"+path,"img");
			mPathList.add(path);
			L.i(2,"add file "+path);
			mMediaList.add(info);
		}
		mCursor.close();
	}
}
