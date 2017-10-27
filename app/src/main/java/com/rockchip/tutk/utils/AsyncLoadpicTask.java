package com.rockchip.tutk.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.widget.ImageView;

/**
 * Created by waha on 2017/3/15.
 */

public class AsyncLoadpicTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView mImageView;
    private String mUrl;
    private final int PIC_WIDTH = 100;
    private final int PIC_HEIGHT = 100;

    public AsyncLoadpicTask(ImageView imageView) {
        mImageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        mUrl = params[0];
        if (String.valueOf(MsgDatas.TYPE_VIDEO).equals(params[1])) {
            //return loadVideoBitmap(mUrl);
            return null;
        }
        return loadPicBitmap(mUrl);
    }

    private Bitmap loadVideoBitmap(String path) {
        Bitmap bitmap = null;
        Bitmap oldBitmap = null;
        try {
            oldBitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND);
            bitmap = ThumbnailUtils.extractThumbnail(oldBitmap, PIC_WIDTH, PIC_HEIGHT, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != oldBitmap && !oldBitmap.isRecycled()) {
                oldBitmap.recycle();
                oldBitmap = null;
            }
        }
        return bitmap;
    }

    private Bitmap loadPicBitmap(String path) {
        Bitmap bitmap = null;
        try {
            bitmap = decodeSampledBitmapFromPath(path, PIC_WIDTH, PIC_HEIGHT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bmp) {
        super.onPostExecute(bmp);

        if (mImageView != null && null != bmp
                && !isCancelled()
                && null != mImageView.getTag()
                && mUrl.equals(mImageView.getTag().toString())) {
            mImageView.setImageBitmap(bmp);
        }
    }

    private Bitmap decodeSampledBitmapFromPath(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = caculateInsampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    private int caculateInsampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
