package com.rockchip.tutk.adapter;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.rockchip.tutk.fragment.FragmentGallery;
import com.rockchip.tutk.utils.L;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.rockchip.tutk.R;

public class GalleryAdapter extends BaseAdapter {
//	private Point mPoint = new Point(0, 0);//用来封装ImageView的宽和高的对象
	/**
	 * 用来存储图片的选中情况
	 */
	private HashMap<Integer, Boolean> mSelectMap = new HashMap<Integer, Boolean>();
	private List<FragmentGallery.MediaInfo> list;
	protected LayoutInflater mInflater;
	protected ImageLoader imageLoader;
	private DisplayImageOptions options;
    private Context mContext;
	public GalleryAdapter(Context context, ArrayList<FragmentGallery.MediaInfo> list) {
		this.list = list;
		L.i(2,"GalleryStyleAdapter ");
        mContext = context;
		mInflater = LayoutInflater.from(context);
		imageLoader = ImageLoader.getInstance();
		imageLoader.init(ImageLoaderConfiguration.createDefault(context));
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.images_load) //设置图片在下载期间显示的图片
				.showImageForEmptyUri(R.drawable.images_interrupt)//设置图片Uri为空或是错误的时候显示的图片
				.showImageOnFail(R.drawable.ic_error)  //设置图片加载/解码过程中错误时候显示的图片
				.cacheOnDisc(true)//设置下载的图片是否缓存在SD卡中
				.considerExifParams(true)  //是否考虑JPEG图像EXIF参数（旋转，翻转）
				.imageScaleType(ImageScaleType.IN_SAMPLE_INT)//设置图片以如何的编码方式显示
				.bitmapConfig(Bitmap.Config.RGB_565)//设置图片的解码类型//
				.cacheInMemory(true)
//		.decodingOptions(BitmapFactory.Options decodingOptions)//设置图片的解码配置
				//.delayBeforeLoading(int delayInMillis)//int delayInMillis为你设置的下载前的延迟时间
				//设置图片加入缓存前，对bitmap进行设置
				//.preProcessor(BitmapProcessor preProcessor)
//		.resetViewBeforeLoading(true)//设置图片在下载前是否重置，复位
//		.displayer(new RoundedBitmapDisplayer(20))//是否设置为圆角，弧度为多少
//		.displayer(new FadeInBitmapDisplayer(100))//是否图片加载好后渐入的动画时间
				.build();//构建完成
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}


	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder;
		String path = ((FragmentGallery.MediaInfo)this.list.get(position)).path;
		String type = ((FragmentGallery.MediaInfo)this.list.get(position)).type;
		if(convertView == null){
//			convertView = mInflater.inflate(R.layout.grid_child_item, null);
			convertView = mInflater.inflate(R.layout.adapter_gallery, parent, false);
			viewHolder = new ViewHolder();
			viewHolder.mImageView = (ImageView) convertView.findViewById(R.id.child_image);
			viewHolder.mIconView = (ImageView) convertView.findViewById(R.id.child_icon);

//			//用来监听ImageView的宽和高
//			viewHolder.mImageView.setOnMeasureListener(new OnMeasureListener() {
//
//				@Override
//				public void onMeasureSize(int width, int height) {
//					mPoint.set(width, height);
//				}
//			});
//
			convertView.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder) convertView.getTag();
		}
        if (type.equals("img"))
            viewHolder.mIconView.setBackground(mContext.getResources().getDrawable(R.drawable.picture));
        else
            viewHolder.mIconView.setBackground(mContext.getResources().getDrawable(R.drawable.videos));
		//利用NativeImageLoader类加载本地图片
//		Bitmap bitmap = NativeImageLoader.getInstance().loadNativeImage(path, mPoint, new NativeImageCallBack() {
//
//			@Override
//			public void onImageLoader(Bitmap bitmap, String path) {
//				ImageView mImageView = (ImageView) mGridView.findViewWithTag(path);
//				if(bitmap != null && mImageView != null){
//					mImageView.setImageBitmap(bitmap);
//				}
//			}
//		});
		L.i(2,"path is "+path);
		imageLoader.displayImage(path, viewHolder.mImageView, options);
//		if(bitmap != null){
//			viewHolder.mImageView.setImageBitmap(bitmap);
//		}else{
//			viewHolder.mImageView.setImageResource(R.drawable.friends_sends_pictures_no);
//		}

		return convertView;
	}

	/**
	 * 给CheckBox加点击动画，利用开源库nineoldandroids设置动画
	 * @param view
	 */
	private void addAnimation(View view){
		float [] vaules = new float[]{0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.25f, 1.2f, 1.15f, 1.1f, 1.0f};
		AnimatorSet set = new AnimatorSet();
		set.playTogether(ObjectAnimator.ofFloat(view, "scaleX", vaules),
				ObjectAnimator.ofFloat(view, "scaleY", vaules));
		set.setDuration(150);
		set.start();
	}


	public void clearMemoryCache()
	{
		imageLoader.clearMemoryCache();
	}

	public static class ViewHolder{
		public ImageView mImageView;
		public ImageView mIconView;
	}
}
