package com.rockchip.tutk.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.rockchip.tutk.model.RecordModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by waha on 2017/2/27.
 */

public class RecordUtils {
    private static final String TAG = "RecordUtils";
    private static final String PATH_RECORD = "/sdcard/IPC/";
    private static final boolean SAVE_PIC_ERROR = true;
    private static final byte[] sps_pps_720p = new byte[]{
            103, 100, 0, 51, -84, 27, 26, -128, 80, 5,
            -70, 16, 0, 0, 3, 0, 16, 0, 0, 3,
            1, -24, -15, 66, -86, 104, -18, 60, -80
    };
    private static final byte[] sps_pps_1080p = new byte[]{
            103, 100, 0, 51, -84, 27, 26, -128, 120, 2,
            39, -27, -124, 0, 0, 3, 0, 4, 0, 0,
            3, 0, 122, 60, 80, -86, -128, 104, -18, 60,
            -80
    };
    private static final byte[] sps_pps_480p = new byte[]{
            103, 100, 0, 51, -84, 27, 26, -128, -96, 61,
            -95, 0, 0, 3, 0, 1, 0, 0, 3, 0,
            30, -113, 20, 42, -96, 104, -18, 60, -80
    };

    static {
        try {
            System.loadLibrary("mp4v2");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "loadLibrary error: " + e.getMessage());
        }

        try {
            System.loadLibrary("avutil-55");
            System.loadLibrary("swresample-2");
            System.loadLibrary("avcodec-57");
            System.loadLibrary("avformat-57");
            System.loadLibrary("swscale-4");
            System.loadLibrary("avfilter-6");
            System.loadLibrary("avdevice-57");
            System.loadLibrary("ffmpegjni");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "loadLibrary error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static native boolean mp4init(String path, int type,
                                         int video_width, int video_height,
                                         int fps, byte[] sps_pps, int sps_pps_len);

    public static native boolean mp4packVideo(byte[] data, int size, int keyFrame);

    public static native void mp4packAudio(byte[] data, int size);

    public static native void mp4savesps();

    public static native void mp4close();

    public static native void mp4release();

    /*ffmpeg*/
    public static native int DecodeInit(int width, int height);

    public static native int Decoding(byte[] in, int datalen, byte[] out);

    public static native int DecodeRelease();

    public static String getRecordSaveName(String suffix) {
        String path = getRecordPath();
        if (!TextUtils.isEmpty(path)) {
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String date = sDateFormat.format(new Date());
            return String.format("%s%s" + suffix, path, date);
        }
        return null;
    }

    public static String getStoragePath() {
        /*if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }*/

        return "sdcard";
    }

    public static String getRecordPath() {
        String storagePath = getStoragePath();
        if (!TextUtils.isEmpty(storagePath)) {
            return storagePath + "/IPC/";
        }

        return null;
    }

    public static String getDownloadPath() {
        String storagePath = getStoragePath();
        if (!TextUtils.isEmpty(storagePath)) {
            return storagePath + "/IPC/download/";
        }

        return null;
    }

    public static boolean mkdirPath(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            return true;
        }
        return file.mkdirs();
    }

    public static void checkPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissionList = new ArrayList<>();
            int state = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (PackageManager.PERMISSION_GRANTED != state) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            state = activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (PackageManager.PERMISSION_GRANTED != state) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            state = activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (PackageManager.PERMISSION_GRANTED != state) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            //check audio
            state = activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO);
            if (PackageManager.PERMISSION_GRANTED != state) {
                permissionList.add(Manifest.permission.RECORD_AUDIO);
            }

            if (permissionList.size() > 0) {
                activity.requestPermissions(
                        permissionList.toArray(new String[permissionList.size()]), 0);
            }
        }
    }

    public static ArrayList<RecordModel> getRecordList(final int type) {
        ArrayList list = new ArrayList();
        String path = getRecordPath();
        File root = new File(path);
        if (root.exists() && root.isDirectory()) {
            String[] childs = root.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (type == MsgDatas.TYPE_IMAGE && name.endsWith(".png")) {
                        return true;
                    } else if (type == MsgDatas.TYPE_VIDEO && name.endsWith(".mp4")) {
                        return true;
                    } else if (type == MsgDatas.TYPE_ALL
                            && (name.endsWith(".mp4") || name.endsWith(".png"))) {
                        return true;
                    }
                    return false;
                }
            });
            for (int i = 0; i < childs.length; i++) {
                String child = childs[i];
                RecordModel model = new RecordModel();
                model.setDisplayName(child.substring(
                        child.lastIndexOf("/") + 1/*, child.length() - suffix.length()*/));
                model.setPath(path + "/" + child);
                model.setType(child.endsWith(".png") ? MsgDatas.TYPE_IMAGE : MsgDatas.TYPE_VIDEO);
                list.add(model);
            }
        }
        return list;
    }

    public static String getPicSaveName(String suffix) {
        String path = getRecordPath();
        if (!TextUtils.isEmpty(path)) {
            SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS");
            String date = sDateFormat.format(new Date());
            return String.format("%s%s" + suffix, path, date);
        }
        return null;
    }

    public static String takePic(byte[] h264, int width, int height) {
        byte[] yuv = new byte[width * height * 2];
        Bitmap bitmap = null;
        int retPic = RecordUtils.Decoding(h264, h264.length, yuv);
        if (retPic > 0) {
            Log.v(TAG, "save pic ret: " + retPic);
            try {
                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                ByteBuffer bb = ByteBuffer.wrap(yuv);
                bitmap.copyPixelsFromBuffer(bb);
                String path = RecordUtils.getPicSaveName(".png");
                if (saveBitmap2File(path, bitmap)) {
                    return path;
                }
                return "";
            } catch (Exception e) {
                Log.v(TAG, "happen error when save bitmap from byte");
                e.printStackTrace();
            } finally {
                try {
                    if (null != bitmap && !bitmap.isRecycled()) {
                        bitmap.recycle();
                        bitmap = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v(TAG, "happen error when recycle bitmap");
                }
            }
        } else if (SAVE_PIC_ERROR) {
            FileOutputStream fos = null;
            try {
                String name = getPicSaveName(".error");
                fos = new FileOutputStream(name);
                fos.write(h264, 0, h264.length);
                fos.flush();
                Log.e(TAG, "save error pic " + name);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != fos) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fos = null;
                }
            }
        }
        return null;
    }

    public static boolean saveBitmap2File(String path, Bitmap bitmap) {
        if (TextUtils.isEmpty(path) || null == bitmap) {
            return false;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                    fos = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return false;
    }

    public static byte[] mp4init(String path, int type, int video_width, int video_height,
                                 int fps, byte[] flame) {
        Log.v(TAG, "fps:" + fps + ", " + video_width + "===" + video_height);
        boolean ret = false;
        byte[] sps_pps = getSpsPps(video_width, video_height, flame);
        if (null != sps_pps) {
            ret = mp4init(path, type, video_width, video_height, fps, sps_pps, sps_pps.length);
            if (ret) {
                return sps_pps;
            }
        }
        return null;
    }

    private static byte[] getSpsPps(int video_width, int video_height, byte[] flame) {
        int sps_start = 0;
        int sps_end = 0;
        int pps_start = 0;
        int pps_end = 0;
        byte[] sps_pps = null;
        try {
            for (int i = 0; i < flame.length - 3; i++) {
                if (flame[i] == 0 && flame[i + 1] == 0 && flame[i + 2] == 1) {
                    if (0 == sps_start) {
                        sps_start = i + 3;
                    } else if (0 == pps_start) {
                        sps_end = i;
                        pps_start = i + 3;
                    } else {
                        pps_end = i;
                        break;
                    }
                }
            }
            sps_pps = new byte[pps_end - 6];
            //Log.v(TAG, sps_pps.length + "====" + sps_start + "," + sps_end + "," + pps_start + "," + pps_end);
            System.arraycopy(flame, sps_start, sps_pps, 0, sps_end - sps_start);
            System.arraycopy(flame, pps_start, sps_pps, sps_end - sps_start, pps_end - pps_start);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < sps_pps.length; i++) {
                sb.append(sps_pps[i] + " ");
            }
            Log.v(TAG, sb.toString());
        } catch (Exception e) {
            Log.e(TAG, "happen error when create sps_pps");
            e.printStackTrace();
        }
        if (null == sps_pps || sps_pps.length < 20/*29*/) {
            sps_pps = getSpsPpsWithPreset(video_width, video_height);
        }
        return sps_pps;
    }

    private static byte[] getSpsPpsWithPreset(int video_width, int video_height) {
        if (video_width == 1920 && video_height == 1080) {
            Log.v(TAG, "use 1080p sps_pps");
            return sps_pps_1080p;
        } else if (video_width == 1280 && video_height == 720) {
            Log.v(TAG, "use 720p sps_pps");
            return sps_pps_720p;
        } else if (video_width == 640 && video_height == 480) {
            Log.v(TAG, "use 480p sps_pps");
            return sps_pps_480p;
        }
        return null;
    }

    public static String getThumbnailsPath() {
        String path = getDownloadPath();
        if (!TextUtils.isEmpty(path)) {
            return path + "thumbnails.jpg";
        }
        return null;
    }

    public static String getMp4ThumbnailsPath() {
        String path = getDownloadPath();
        if (!TextUtils.isEmpty(path)) {
            return path + "mp4thumbnails.jpg";
        }
        return null;
    }

    public static String getRecordAudioSaveName(String videoName, String suffix) {
        String path = getRecordAudioPath();
        if (!TextUtils.isEmpty(videoName)) {
            int startPos = videoName.lastIndexOf("/");
            int endPos = videoName.lastIndexOf(".");
            videoName = videoName.substring(startPos, endPos);
            return String.format("%s%s" + suffix, path, videoName);
        }
        return null;
    }

    public static String getRecordAudioPath() {
        String storagePath = getStoragePath();
        if (!TextUtils.isEmpty(storagePath)) {
            return storagePath + "/IPC/audio";
        }

        return null;
    }

    public static int bytesToInt(byte[] buffer, int pos, int bytes) {
        int retval = 0;
        for (int i = 0; i < bytes; ++i) {
            retval |= (buffer[pos + i] & 0xFF) << (8 * (bytes - i - 1));
        }
        return retval;
    }

    public static byte[] intToBytes(int num) {
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (num >>> (24 - i * 8));
        }
        return b;
    }

    public static String byteToString(byte[] buffer) {
        assert buffer.length == 4;
        String retval = new String();
        try {
            retval = new String(buffer, 0, buffer.length, "ascii");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return retval;
    }
}