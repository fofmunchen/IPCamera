package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.utils.MsgDatas;

/**
 * Created by waha on 2017/4/13.
 */

public class RecordPlayActivity extends AppCompatActivity implements
        TUTKDevice.OnTutkError, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {
    private static final String TAG = "RecordPlay";
    private static final int CHECK_TIME = 500;
    private static final int OUT_TIME = 20000;
    public static final String EXTRA_FILENAME = "extra_filename";
    public static final String EXTRA_FILETYPE = "extra_filetype";

    private VideoView videoView;
    private ProgressBar pbWait;
    private TUTKDevice mTUTKDevice;
    private String mUrl;
    private MediaController mController;
    private int mOldDuration = 0;
    private boolean mCancel;
    private int mPositionWhenPaused;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MsgDatas.MSG_PLAY_FINISH:
                    mCancel = true;
                    if (!isFinishing()) {
                        finish();
                    }
                    break;
                case MsgDatas.MSG_PLAY_CHECK:
                    if (mCancel) {
                        break;
                    }
                    int duration = videoView.getCurrentPosition();
                    if (mOldDuration == duration && (videoView.isPlaying() || duration == 0)
                            && View.GONE == pbWait.getVisibility()) {
                        pbWait.setVisibility(View.VISIBLE);
                        mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_PLAY_TIMEOUT,
                                OUT_TIME);
                    } else if (View.VISIBLE == pbWait.getVisibility()) {
                        pbWait.setVisibility(View.GONE);
                        mHandler.removeMessages(MsgDatas.MSG_PLAY_TIMEOUT);
                    }
                    mOldDuration = duration;
                    if (!isFinishing() && !mCancel) {
                        mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_PLAY_CHECK, CHECK_TIME);
                    }
                    break;
                case MsgDatas.MSG_PLAY_TIMEOUT:
                    Log.e(TAG, "net work error, MSG_PLAY_TIMEOUT");
                    mCancel = true;
                    Toast.makeText(RecordPlayActivity.this,
                            R.string.network_failed, Toast.LENGTH_SHORT).show();
                    if (!isFinishing()) {
                        finish();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_record_play);

        initUI();
        initData();
    }

    private void initUI() {
        videoView = (VideoView) findViewById(R.id.videoView);
        videoView.setOnErrorListener(this);
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);
        pbWait = (ProgressBar) findViewById(R.id.pb_wait);
    }

    private void initData() {
        Intent intent = getIntent();
        if (intent == null) return;

        String uid = intent.getStringExtra(TUTKManager.TUTK_UID);
        String fileName = intent.getStringExtra(EXTRA_FILENAME);
        int fileType = intent.getIntExtra(EXTRA_FILETYPE, -1);
        if (uid != null){// && uid.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(uid);
        }
        if (null == mTUTKDevice || TextUtils.isEmpty(fileName) || -1 == fileType) {
            Toast.makeText(this, R.string.txt_get_info_failed, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (null != getSupportActionBar()) {
            getSupportActionBar().setTitle(fileName);
        }
        mController = new MediaController(this);
        //mUrl = "http://192.168.1.106:8080/video/20170414_105224_A.mp4";
        mUrl = String.format("http://%s:8080/%s/%s",
                mTUTKDevice.getSession().getIP(),
                fileType == MsgDatas.TYPE_MD ? "lock" : "video",
                fileName);
        Log.v(TAG, "url:" + mUrl);
        play();
    }

    private void play() {
        Log.v(TAG, "play url:" + mUrl);
        videoView.setVideoPath(mUrl);
        videoView.setMediaController(mController);
        videoView.requestFocus();
        videoView.start();
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
            /*mPositionWhenPaused = videoView.getCurrentPosition();
            videoView.stopPlayback();*/
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCancel = true;
        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
        mHandler.removeMessages(MsgDatas.MSG_PLAY_TIMEOUT);
        super.onDestroy();
    }

    @Override
    public void onError(int code) {
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "onPrepared");
        mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_CHECK);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mCancel = true;
        mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_FINISH);
        Log.e(TAG, "onError what=" + what + ", extra=" + extra);
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mCancel = true;
        mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_FINISH);
        Log.v(TAG, "play completion");
    }
}
