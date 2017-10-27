package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKDevice.RDTChannelUsedStatus;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.command.Mp4CutRequest;
import com.rockchip.tutk.command.StartFileTransferCommand;
import com.rockchip.tutk.model.Mp4Info;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.RecordUtils;
import com.rockchip.tutk.view.CustomSeekbar;
import com.tutk.IOTC.RDTAPIs;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by waha on 2017/5/4.
 */

public class DownAndPlayActivity extends AppCompatActivity implements
        TUTKDevice.OnTutkError, SurfaceHolder.Callback,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener, View.OnClickListener, MediaPlayer.OnBufferingUpdateListener {
    private static final String TAG = "DownAndPlay";
    public static final String EXTRA_FILENAME = "extra_filename";
    public static final String EXTRA_FILEPATH = "extra_filepath";

    private SurfaceView surfaceView;
    private ProgressBar pbWait;
    private TextView txtCurDuration;
    private TextView txtTotalDuration;
    private CustomSeekbar sbDuration;
    private ImageButton btnPlay;
    private TUTKDevice mTUTKDevice;
    private int mRdtId = -1;
    private String mLocalPath;
    private String mRemotePath;
    private boolean mCancel;
    private final String OK = "";
    private DownLoadThread mDownLoadThread;
    private final int TIME_MAX_READ = 10000;
    private final int MAX_READ_TIMEOUT = 10;
    private Mp4Info mMp4Info = new Mp4Info();
    private final int RESERVED_TIME = 2000;//预留2s进行暂停
    private final int CACHE_TIME = 5000 + RESERVED_TIME;//缓冲
    private final int CHECK_TIME = 500;
    private int mDatPerDuration;
    private MediaPlayer mMediaPlayer;
    private boolean mIsTracking;
    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private boolean mDownloadFinish;
    private boolean mPlayComplete;
    private boolean mFirstPlay = true;
    private boolean mWaitingDownloadFinish;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MsgDatas.MSG_PLAY_PREPARED:
                    if (!mCancel && !prepare()) {
                        mCancel = true;
                        Toast.makeText(DownAndPlayActivity.this, R.string.mp4_prepare_error, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    break;
                case MsgDatas.MSG_PLAY_START:
                    if (!mCancel && mFirstPlay) {
                        Log.v(TAG, "first play");
                        mFirstPlay = false;
                        mMediaPlayer.start();
                        sbDuration.setCanTrack(true);
                        pbWait.setVisibility(View.GONE);
                    } else if (!mCancel && !mWaitingDownloadFinish) {
                        Log.v(TAG, "restart");
                        restart();
                        pbWait.setVisibility(View.GONE);
                    }
                    break;
                case MsgDatas.MSG_PLAY_CHECK:
                    if (!mCancel && null != mMediaPlayer && mDatPerDuration > 0
                            && !mWaitingDownloadFinish) {
                        int curDuration = mMediaPlayer.getCurrentPosition();
                        if (!mIsTracking) {
                            txtCurDuration.setText(secondFormat(curDuration / 1000));
                            sbDuration.setProgress(curDuration);
                        }
                        //Log.v(TAG, "curDuration " + curDuration);
                        float curPlayedDat = curDuration * mDatPerDuration;
                        float reservedDat = RESERVED_TIME * mDatPerDuration;
                        if (!mDownloadFinish && mMediaPlayer.isPlaying()
                                && mMp4Info.getDownLoadedMdat() - curPlayedDat <= reservedDat) {
                            Log.v(TAG, "pause");
                            pbWait.setVisibility(View.VISIBLE);
                            mMediaPlayer.pause();
                        }
                    }
                    if (!mCancel) {
                        mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_PLAY_CHECK, CHECK_TIME);
                    }
                    break;
                case MsgDatas.MSG_DOWNLOAD_FINISH:
                    String ret = msg.obj.toString();
                    if (TextUtils.isEmpty(ret)) {
                        mDownloadFinish = true;
                        if (mWaitingDownloadFinish) {
                            mWaitingDownloadFinish = false;
                            restart();
                            pbWait.setVisibility(View.GONE);
                        }
                        sbDuration.setCanTrack(true);
                        Log.v(TAG, "MSG_DOWNLOAD_FINISH");
                    } else if (!mCancel) {
                        Log.e(TAG, "MSG_DOWNLOAD_FINISH with some error");
                        mCancel = true;
                        Toast.makeText(DownAndPlayActivity.this, ret, Toast.LENGTH_SHORT).show();
                        if (!isFinishing()) {
                            finish();
                        }
                    }
                    break;
                case MsgDatas.MSG_CONNECT_ERROR:
                    if (!mCancel) {
                        Toast.makeText(DownAndPlayActivity.this, R.string.unconnect, Toast.LENGTH_SHORT).show();
                        setResult(Activity.RESULT_OK);
                        finish();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_down_play);

        initUI();
        initData();
    }

    private void initUI() {
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceView.getHolder().setKeepScreenOn(true);
        surfaceView.getHolder().addCallback(this);
        pbWait = (ProgressBar) findViewById(R.id.pb_wait);
        txtCurDuration = (TextView) findViewById(R.id.txt_curDuration);
        txtTotalDuration = (TextView) findViewById(R.id.txt_totalDuration);
        sbDuration = (CustomSeekbar) findViewById(R.id.sb_duration);
        sbDuration.setOnSeekBarChangeListener(this);
        sbDuration.setCanTrack(false);
        btnPlay = (ImageButton) findViewById(R.id.btn_play);
        btnPlay.setOnClickListener(this);
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) return;

        String uid = intent.getStringExtra(TUTKManager.TUTK_UID);
        String fileName = intent.getStringExtra(EXTRA_FILENAME);
        mRemotePath = intent.getStringExtra(EXTRA_FILEPATH);
        if (uid != null ){//&& uid.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(uid);
        }
        if (null == mTUTKDevice || TextUtils.isEmpty(fileName) || TextUtils.isEmpty(mRemotePath)) {
            Toast.makeText(this, R.string.txt_get_info_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (null != getSupportActionBar()) {
            getSupportActionBar().setTitle(fileName);
        }
        boolean ret = RecordUtils.mkdirPath(RecordUtils.getDownloadPath());
        if (!ret) {
            Toast.makeText(this, R.string.storage_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mLocalPath = RecordUtils.getDownloadPath() + "temp.mp4";
        delOldFile(mLocalPath);
    }

    private boolean prepare() {
        if (null == mMediaPlayer) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(mLocalPath);
                mMediaPlayer.setDisplay(surfaceView.getHolder());
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.setOnBufferingUpdateListener(this);
                mMediaPlayer.prepare();
                return true;
            } catch (Exception e) {
                Log.v(TAG, "happen error when prepare");
                e.printStackTrace();
            }
        }
        return false;
    }

    private void scaleSurface() {
        if (mVideoWidth < 1 || mVideoHeight < 1) {
            Log.w(TAG, "video not play");
            return;
        }
        if (0 == mScreenWidth || 0 == mScreenHeight) {
            mScreenWidth = surfaceView.getWidth();
            mScreenHeight = surfaceView.getHeight();
        }
        float scaleWidth = (float) mVideoWidth / mScreenWidth;
        float scaleHeight = (float) mVideoHeight / mScreenHeight;
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        if (scaleWidth > scaleHeight) {
            int h = (int) (mScreenHeight - (mVideoHeight / scaleWidth)) / 2;
            lp.setMargins(0, h, 0, h);
        } else {
            int w = (int) (mScreenWidth - (mVideoWidth / scaleHeight)) / 2;
            lp.setMargins(w, 0, w, 0);
        }
        surfaceView.setLayoutParams(lp);
    }

    private void restart() {
        if (null != mMediaPlayer) {
            try {
                mMediaPlayer.pause();
                int curPos = mMediaPlayer.getCurrentPosition();
                Log.v(TAG, "curPos to restart:" + curPos);
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(mLocalPath);
                mMediaPlayer.setDisplay(surfaceView.getHolder());
                mMediaPlayer.prepare();
                mMediaPlayer.seekTo(curPos);
                mMediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void startCheck() {
        if (mDatPerDuration > 0) {
            return;
        }
        mVideoWidth = mMediaPlayer.getVideoWidth();
        mVideoHeight = mMediaPlayer.getVideoHeight();
        scaleSurface();
        int totalDuration = mMediaPlayer.getDuration();
        long mp4DatSize = mMp4Info.getMdatSize() - 8;
        txtTotalDuration.setText(secondFormat(totalDuration / 1000));
        sbDuration.setMax(totalDuration);
        mDatPerDuration = (int) (mp4DatSize / totalDuration + 1);
        if (mDatPerDuration > 0) {
            Log.v(TAG, "mDatPerDuration=" + mDatPerDuration);
            mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
            mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_CHECK);
        }
    }

    private String secondFormat(int second) {
        if (second < 10) {
            return "00:0" + second;
        } else if (second < 60) {
            return "00:" + second;
        }
        StringBuilder sb = new StringBuilder();
        int minute = second / 60;
        int second2 = second % 60;
        if (minute < 10) {
            sb.append("0");
        }
        sb.append(minute);
        sb.append(":");
        if (second2 < 10) {
            sb.append("0");
        }
        sb.append(second2);
        return sb.toString();
    }

    @Override
    protected void onResume() {
        if (mTUTKDevice != null) {
            mTUTKDevice.addOnTutkErrorListener(this);
        }
        /*if (mPositionWhenPaused >= 0 && null != videoView) {
            videoView.seekTo(mPositionWhenPaused);
            mPositionWhenPaused = -1;
        }*/
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (null != mTUTKDevice) {
            mTUTKDevice.removeOnTutkErrorListener(this);
        }
        if (!isFinishing()) {
            Log.v(TAG, "finish when onpause");
            finish();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCancel = true;
        mHandler.removeMessages(MsgDatas.MSG_PLAY_START);
        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
        mHandler.removeMessages(MsgDatas.MSG_PLAY_TIMEOUT);
        if (null != mDownLoadThread) {
            mDownLoadThread.setCancel(true);
        }

        if (mRdtId > -1) {
            new Thread() {
                @Override
                public void run() {
                    int ret = RDTAPIs.RDT_Abort(mRdtId);
                    Log.v(TAG, "ondestory RDT_Abort rdtid:" + mRdtId + ",ret=" + ret);
                    //mTUTKDevice.getSession().setRdtId(-1);
                    mRdtId = -1;
                }
            }.start();
        }
        if (null != mTUTKDevice) {
            if (RDTChannelUsedStatus.CHANNEL_PLAY_MP4 == mTUTKDevice.getRdtChannelUsedStatus()) {
                mTUTKDevice.setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
            }
        }
        delOldFile(mLocalPath);
        super.onDestroy();
    }

    @Override
    public void onError(int code) {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "MediaPlayer onPrepared");
        startCheck();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mCancel = true;
        //mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_FINISH);
        Log.e(TAG, "onError what=" + what + ", extra=" + extra);
        finish();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
        mCancel = true;
        Log.v(TAG, "play completion");
        mPlayComplete = true;
        int curPos = mMediaPlayer.getCurrentPosition();
        sbDuration.setProgress(curPos);
        txtCurDuration.setText(txtTotalDuration.getText());
        btnPlay.setImageResource(R.drawable.play_24);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v(TAG, "surfaceCreated");
        if (null == mDownLoadThread) {
            mDownLoadThread = new DownLoadThread(mRemotePath, mLocalPath);
            mDownLoadThread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        mCancel = true;
        if (null != mMediaPlayer) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        txtCurDuration.setText(secondFormat(progress / 1000));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        Log.v(TAG, "onStartTrackingTouch");
        mIsTracking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mIsTracking = false;
        if (!mDownloadFinish) {
            pbWait.setVisibility(View.VISIBLE);
            sbDuration.setCanTrack(false);
            mWaitingDownloadFinish = true;
            mMediaPlayer.seekTo(seekBar.getProgress());
            mMediaPlayer.pause();
        } else {
            if (null != mMediaPlayer && mMediaPlayer.isPlaying()) {
                mMediaPlayer.seekTo(seekBar.getProgress());
                mMediaPlayer.start();
            }
        }
        Log.v(TAG, "onStopTrackingTouch");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                if (mWaitingDownloadFinish) {
                    return;
                }
                if (null != mMediaPlayer && mMediaPlayer.isPlaying()) {
                    mMediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.play_24);
                } else if (null != mMediaPlayer) {
                    if (mPlayComplete) {
                        mMediaPlayer.seekTo(0);
                        sbDuration.setProgress(0);
                        txtCurDuration.setText("00:00");
                        mPlayComplete = false;
                        mCancel = false;
                        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
                        mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_CHECK);
                    }
                    mMediaPlayer.start();
                    btnPlay.setImageResource(R.drawable.pause_24);
                }
                break;
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        Log.v(TAG, "percent:" + percent);
        throw new RuntimeException();
    }

    private class DownLoadThread extends Thread {
        private boolean cancel;
        private String pathRemote;
        private String pathLocal;

        public void setCancel(boolean cancel) {
            this.cancel = cancel;
        }

        public DownLoadThread(String pathRemote, String pathLocal) {
            this.pathRemote = pathRemote;
            this.pathLocal = pathLocal;
        }

        @Override
        public void run() {
            int countNo = 0;
            while (!cancel) {
                RDTChannelUsedStatus rdtChannelUsedStatus = mTUTKDevice.getRdtChannelUsedStatus();
                if (RDTChannelUsedStatus.CHANNEL_FREE != rdtChannelUsedStatus) {
                    try {
                        Log.v(TAG, "rdt channel is used " + rdtChannelUsedStatus + ", sleep time:" + countNo);
                        countNo++;
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (countNo > 10) {
                        cancel = true;
                    }
                    continue;
                }
                break;
            }
            mTUTKDevice.setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_PLAY_MP4);
            String ret = cancel ? getResources().getString(R.string.network_failed) : doing();

            if (mRdtId > -1) {
                int retDestory = RDTAPIs.RDT_Abort(mRdtId);
                Log.v(TAG, "abort rdtid:" + mRdtId + " after doing ret=" + retDestory);
                if (retDestory > -1 || retDestory == RDTAPIs.RDT_ER_INVALID_RDT_ID
                        || retDestory == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                    mTUTKDevice.getSession().setRdtId(-1);
                }
                mRdtId = -1;
            }
            mTUTKDevice.setRdtChannelUsed(TUTKDevice.RDTChannelUsedStatus.CHANNEL_FREE);
            Message message = new Message();
            message.what = MsgDatas.MSG_DOWNLOAD_FINISH;
            message.obj = ret;
            mHandler.sendMessage(message);
        }

//        private String doing() {
//            mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_PREPARED);
//            mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_START);
//            mMp4Info.setMdatSize(7081944);
//            return "";
//        }

        private String doing() {
            int sid = mTUTKDevice.getSession().getSID();
            if (sid < 0) {
                return getString(R.string.unconnect);
            }
            mRdtId = mTUTKDevice.getSession().getRdtId();
            if (mRdtId >= 0) {
                int ret = RDTAPIs.RDT_Abort(mRdtId);
                Log.w(TAG, "RDT_Abort old rdtid:" + mRdtId + ", ret=" + ret);
                if (ret > -1 || ret == RDTAPIs.RDT_ER_INVALID_RDT_ID
                        || ret == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                    mTUTKDevice.getSession().setRdtId(-1);
                }
            }
            mRdtId = RDTAPIs.RDT_Create(sid, MsgDatas.NETWORK_TIMEOUT * 2, 1);
            if (mRdtId < 0) {
                Log.e(TAG, "doing failed sid=" + sid + " ,mRdtId=" + mRdtId);
                return getString(R.string.unconnect);
            }
            mTUTKDevice.getSession().setRdtId(mRdtId);
            Log.v(TAG, "mRdtId:" + mRdtId);

            //send request
            String ret = sendMp4CutRequest(pathRemote);
            if (mCancel) {
                return "";
            } else if (OK.equals(ret)) {
                Log.v(TAG, "getmp4 info success");
            } else {
                return ret;
            }

            //rev ftyp and free
            ret = revRemoteFile(pathRemote, false, 0, mMp4Info.getHeadSize());
            if (OK.equals(ret)) {
                Log.v(TAG, "rev ftyp and free success");
            } else {
                return ret;
            }

            //rev moov
            ret = revRemoteFile(pathRemote, false, mMp4Info.getMoovStartPos(),
                    mMp4Info.getMoovStartPos() + mMp4Info.getMoovSize());
            if (OK.equals(ret)) {
                Log.v(TAG, "rev moov success");
            } else {
                return ret;
            }

            //prepare
            mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_PREPARED);

            //rev mdat
            return revRemoteFile(pathRemote, true, mMp4Info.getHeadSize(), mMp4Info.getHeadSize() + mMp4Info.getMdatSize());
        }

        private String sendMp4CutRequest(String pathRemote) {
            //build request
            Mp4CutRequest requestObj = new Mp4CutRequest(pathRemote);
            String strRequest = requestObj.Json();
            Log.v(TAG, "send msg: " + strRequest);
            byte[] data = strRequest.getBytes();
            //send request
            if (sendMsg(data, data.length) < 1) {
                return getString(R.string.network_failed);
            }
            //rev request result
            mMp4Info = revMsg(Mp4Info.class);
            if (null != mMp4Info && mMp4Info.isResult()) {
                return OK;
            }
            return getString(R.string.mp4_play_unopen);
        }

        private String revRemoteFile(String pathRemote, boolean isMdat, long startPos, long endPos) {
            //build request
            RandomAccessFile raf = null;
            try {
                StartFileTransferCommand startFileTransferCommand = new StartFileTransferCommand(pathRemote, startPos, endPos);
                String commad = startFileTransferCommand.Json();
                int write = RDTAPIs.RDT_Write(mRdtId, commad.getBytes(), commad.getBytes().length);
                if (write < 0) {
                    Log.e(TAG, "StartFileTransfer timeout");
                    return getString(R.string.network_failed);
                }

                long receive = 0;
                raf = new RandomAccessFile(pathLocal, "rw");
                long totalWantRead = endPos - startPos;
                raf.seek(startPos);
                byte[] buff = new byte[102400];//100kb
                long downloadCount = 0;
                while (!cancel && receive < totalWantRead) {
                    int i = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, MsgDatas.NETWORK_TIMEOUT);
                    //Log.d(TAG, "receive:" + i);
                    if (i <= 0) {
                        Log.e(TAG, "break receive");
                        return getString(R.string.network_failed);
                    }
                    raf.write(buff, 0, i);
                    receive += i;
                    if (isMdat) {
                        //Thread.sleep(2000);
                        downloadCount += i;
                        mMp4Info.addDownLoadedMdat(i);
                        //做个判断,如果下载大于CACHE_SIZE,就执行播放
                        if (mDatPerDuration > 0 && downloadCount >= CACHE_TIME * mDatPerDuration) {
                            Log.v(TAG, "downloadCount" + downloadCount);
                            mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_START);
                            downloadCount = 0;
                        }
                    }
                }
                Log.d(TAG, "finish totalWantRead:" + totalWantRead + ", received=" + receive);
                if (isMdat) {
                    mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_START);
                }
                return OK;
            } catch (Exception e) {
                Log.e(TAG, "happen error when download");
                e.printStackTrace();
            } finally {
                try {
                    if (null != raf) {
                        raf.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return getString(R.string.mp4_play_error);
        }

        private <T> T revMsg(Class<T> classOfT) {
            byte[] buff = new byte[1024];
            int ret = RDTAPIs.RDT_ER_TIMEOUT;
            int count = 0;
            while (ret == RDTAPIs.RDT_ER_TIMEOUT) {
                count++;
                ret = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, TIME_MAX_READ);
                if (ret < 0) {
                    Log.e(TAG, "rev read: " + ret);
                    if (count > MAX_READ_TIMEOUT) {
                        break;
                    }
                } else {
                    Log.v(TAG, "rev read: " + ret);
                }
            }
            if (ret < 0) {
                mHandler.sendEmptyMessage(MsgDatas.MSG_CONNECT_ERROR);
                return null;
            }
            String strRev = new String(buff, 0, ret);
            strRev = strRev.replace("[", "");
            strRev = strRev.replace("]", "");
            Log.v(TAG, "rev: " + strRev);
            Gson gson = new Gson();
            T rev = null;
            try {
                rev = gson.fromJson(strRev, classOfT);
            } catch (Exception e) {
                Log.e(TAG, "happen error when change gson");
                e.printStackTrace();
            }
            return rev;
        }

        private int sendMsg(byte[] buffer, int buffSize) {
            int ret;
            while (true) {
                if (mCancel) {
                    return 0;
                }
                ret = RDTAPIs.RDT_Write(mRdtId, buffer, buffSize);
                Log.d(TAG, "send byte write: " + ret);
                if (ret < 0) {
                    mHandler.sendEmptyMessage(MsgDatas.MSG_CONNECT_ERROR);
                    break;
                }
                break;
            }
            return ret;
        }
    }

    private void delOldFile(String path) {
        if (null != path) {
            File file = new File(path);
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
