package com.rockchip.tutk.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKDevice.RDTChannelUsedStatus;
import com.rockchip.tutk.command.FileTransferCommand;
import com.rockchip.tutk.command.StartFileTransferCommand;
import com.tutk.IOTC.RDTAPIs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by waha on 2017/5/25.
 */

public class AsyncLoadRemotepicTask extends AsyncTask<String, Void, Bitmap> {
    private ImageView mImageView;
    private String mUrl;
    private final int PIC_WIDTH = 100;
    private final int PIC_HEIGHT = 100;
    private static final String TAG = "RemoteRecord";
    private TUTKDevice mTutkDevice;
    private boolean mCancel;
    private int mRdtId;
    private static final int READ_TIMROUT = 2000;

    public AsyncLoadRemotepicTask(ImageView imageView, TUTKDevice tutkDevice) {
        mImageView = imageView;
        mTutkDevice = tutkDevice;
    }

    @Override
    protected Bitmap doInBackground(String... params) {
        mUrl = params[0];
        Bitmap bitmap = loadRemoteBitmap(mUrl);
        if (RDTChannelUsedStatus.CHANNEL_DOWNLOAD_MP4_THUMBS == mTutkDevice.getRdtChannelUsedStatus()) {
            mTutkDevice.setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
        }
        if (mRdtId > -1) {
            int retDestory = RDTAPIs.RDT_Abort(mRdtId);
            Log.v(TAG, "abort rdtid:" + mRdtId + " after doing ret=" + retDestory);
            if (retDestory > -1 || retDestory == RDTAPIs.RDT_ER_INVALID_RDT_ID
                    || retDestory == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                mTutkDevice.getSession().setRdtId(-1);
            }
            mRdtId = -1;
        }
        return bitmap;
    }

    private Bitmap loadRemoteBitmap(String pathRemote) {
        Log.v(TAG, "begin to load remote thumbnail");
        int countNo = 0;
        while (!isCancelled()) {
            RDTChannelUsedStatus rdtChannelStatus = mTutkDevice.getRdtChannelUsedStatus();
            if (RDTChannelUsedStatus.CHANNEL_FREE != rdtChannelStatus) {
                try {
                    Log.v(TAG, "rdt channel is used " + rdtChannelStatus + ", sleep time:" + countNo);
                    countNo++;
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (countNo > 5) {
                    mCancel = true;
                    return null;
                }
                continue;
            }
            break;
        }
        mTutkDevice.setRdtChannelUsed(TUTKDevice.RDTChannelUsedStatus.CHANNEL_DOWNLOAD_MP4_THUMBS);
        int sid = mTutkDevice.getSession().getSID();
        if (sid < 0) {
            return null;
        }
        mRdtId = mTutkDevice.getSession().getRdtId();
        if (mRdtId >= 0) {
            int ret = RDTAPIs.RDT_Abort(mRdtId);
            Log.w(TAG, "RDT_Abort old rdtid:" + mRdtId + ", ret=" + ret);
            if (ret > -1 || ret == RDTAPIs.RDT_ER_INVALID_RDT_ID
                    || ret == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                mTutkDevice.getSession().setRdtId(-1);
            }
        }
        mRdtId = RDTAPIs.RDT_Create(sid, 5000, 1);
        if (mRdtId < 0) {
            Log.e(TAG, "failed sid=" + sid + " ,mRdtId=" + mRdtId);
            return null;
        }
        mTutkDevice.getSession().setRdtId(mRdtId);
        Log.v(TAG, "downloadFile retId=" + mRdtId + ", pathRemote=" + pathRemote);
        if (mCancel) {
            return null;
        }
        FileOutputStream fos = null;
        try {
            FileTransferCommand fileTransferCommand = new FileTransferCommand(pathRemote);
            String json = fileTransferCommand.Json();
            int write = RDTAPIs.RDT_Write(mRdtId, json.getBytes(), json.getBytes().length);
            if (!mCancel && write < 0) {
                Log.e(TAG, "cancel" + mCancel + ", FileTransfer write=" + write);
                return null;
            }
            Log.d(TAG, "write:" + write);
            byte[] buff = new byte[1024 * 128];
            int rdt_read = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, READ_TIMROUT);
            String fileInfo = new String(buff, 0, rdt_read);
            JSONArray jsonArray = new JSONArray(fileInfo);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            long fileSize = jsonObject.getLong("FileSize");
            if (fileSize < 1) {
                Log.e(TAG, "not remote file " + pathRemote);
                return null;
            }
            String localPath = RecordUtils.getMp4ThumbnailsPath();
            File file = new File(localPath);
            if (file.exists()) {
                file.delete();
            }
            StartFileTransferCommand startFileTransferCommand = new StartFileTransferCommand(pathRemote);
            String commad = startFileTransferCommand.Json();
            write = RDTAPIs.RDT_Write(mRdtId, commad.getBytes(), commad.getBytes().length);
            if (!mCancel && write < 0) {
                Log.e(TAG, "cancel" + mCancel + ", StartFileTransfer write=" + write);
                return null;
            }

            int receive = 0;
            fos = new FileOutputStream(file);
            while (!mCancel && receive < fileSize) {
                int i = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, READ_TIMROUT);
                if (i <= 0) {
                    Log.e(TAG, "break receive " + i);
                    return null;
                }
                fos.write(buff, 0, i);
                receive += i;
                if (receive == fileSize) {
                    Log.d(TAG, "receive end");
                    break;
                }
            }
            return decodeSampledBitmapFromPath(localPath, PIC_WIDTH, PIC_HEIGHT);
        } catch (Exception e) {
            Log.e(TAG, "happen error when download");
            e.printStackTrace();
        } finally {
            try {
                if (null != fos) {
                    fos.flush();
                    fos.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
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
