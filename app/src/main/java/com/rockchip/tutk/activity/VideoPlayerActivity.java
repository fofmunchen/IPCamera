package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.AssetFileDescriptor;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.rockchip.tutk.R;

import java.io.IOException;

public class VideoPlayerActivity extends Activity implements MediaPlayer.OnPreparedListener, SeekBar.OnSeekBarChangeListener {
    private String mFilePath;
    private SurfaceView mSurfaceView;
    private Button btnPlay, btnSwitch;
    private SeekBar seekProgress, seekVolume;
    private MediaPlayer mMediaPlayer;
    private RelativeLayout mRlVideo;
    private boolean canProgress;
    private AudioManager mAudioManager;
    private float mDefaultheight = 0;
    private float mDefaultwidth = 0;
    private float mScreenheight;
    private float mScreenwidth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videoplayer);
        mFilePath = getIntent().getStringExtra("path");
        Log.i("wz","mFilePath is "+mFilePath);
        initUI();
        initPlayer();
    }

    public void initUI() {
        mSurfaceView = (SurfaceView) findViewById(R.id.rl_sv);
        btnPlay = (Button) findViewById(R.id.bt_stop);
        this.btnPlay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View paramAnonymousView){
                if ((mMediaPlayer != null) && (mMediaPlayer.getDuration() > 0)){
                    if (mMediaPlayer.isPlaying()){
                        mMediaPlayer.pause();
                        btnPlay.setText("播放");
                    }
                    else {
                        mMediaPlayer.start();
                        btnPlay.setText("暂停");
                    }
                }
            }
        });
        btnSwitch = (Button) findViewById(R.id.bt_change);
        seekProgress = (SeekBar) findViewById(R.id.sb_progress);
        seekVolume = (SeekBar) findViewById(R.id.sb_vol);
        mRlVideo = (RelativeLayout) findViewById(R.id.rl);
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    //变成竖屏
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    //变成横屏了
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });
        seekProgress.setOnSeekBarChangeListener(this);
    }
    public void initPlayer() {
        this.mScreenwidth = getResources().getDisplayMetrics().widthPixels;
        this.mScreenheight = getResources().getDisplayMetrics().heightPixels;
        Log.e("wz", "mScreenwidth:" + this.mScreenwidth + " mScreenheight:" + this.mScreenheight);
        mMediaPlayer = new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        seekVolume.setMax(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        seekVolume.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        registerReceiver(new MyReceiver(), new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                //当画面可见的时候执行
                play();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                //当画面发生变化执行
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //当画面不见得时候执行
                stop();
            }
        });
    //    mSurfaceView.getHolder().setKeepScreenOn(true);
    }
    class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            seekVolume.setProgress(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }
    private void play() {
        try {
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mFilePath);
            //设置循环播放
            mMediaPlayer.setLooping(true);
            //设置播放区域
            mMediaPlayer.setDisplay(mSurfaceView.getHolder());
            //播放时屏幕保持唤醒
            mMediaPlayer.setScreenOnWhilePlaying(true);
            //异步准备播放视频
            mMediaPlayer.prepare();
            mMediaPlayer.setOnPreparedListener(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    @Override
    public void onPrepared(MediaPlayer mp) {
        //设置一个播放错误的监听
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return false;
            }
        });
        seekProgress.setMax(mMediaPlayer.getDuration());
        //先设置视频播放的大小
        setVideoParams(mMediaPlayer, getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        mp.start();
     //   startProgress();
    }

    /**
     * 设置SurfaceView的参数
     *
     * @param mediaPlayer
     * @param isLand
     */
    public void setVideoParams(MediaPlayer mediaPlayer, boolean isLand) {
        float videoWidth = mediaPlayer.getVideoWidth();
        float videoHeight = mediaPlayer.getVideoHeight();
        Log.e("wz", "vWidth:" + videoWidth + " vHeight:" + videoHeight);
        int outwidth,outheight;
        float inwidth,inheight;
        if (isLand)
        {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            inwidth = mScreenheight;
            inheight = mScreenwidth;
        }
        else
        {
            if ((this.mDefaultwidth == 0) || (this.mDefaultwidth == 0))
            {
                this.mDefaultwidth = this.mRlVideo.getMeasuredWidth();
                this.mDefaultheight = this.mRlVideo.getMeasuredHeight();
                Log.e("wz", "mDefaultwidth:" + this.mDefaultwidth + " mDefaultheight:" + this.mDefaultheight);
            }
            inwidth = mDefaultwidth;
            inheight = mDefaultheight;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        float f = Math.max(videoWidth / inwidth, videoHeight / inheight);
        outwidth = (int)Math.ceil(videoWidth / f);
        outheight = (int)Math.ceil(videoHeight / f);

        Log.e("wz", "outwidth:" + outwidth + " outheight:" + outheight);

        ViewGroup.LayoutParams videoparams = new RelativeLayout.LayoutParams(outwidth, outheight);
        this.mRlVideo.setLayoutParams(videoparams);
        ViewGroup.LayoutParams surfaceparams = this.mSurfaceView.getLayoutParams();
        surfaceparams.width = outwidth;
        surfaceparams.height = outheight;
        this.mSurfaceView.setLayoutParams(surfaceparams);

    }

    /**
     * 视频播放的同时进度条开始一起走
     */
    public void startProgress() {
        canProgress = true;
        new Thread() {
            @Override
            public void run() {
                while (canProgress) {
                    try {
                        seekProgress.setProgress(mMediaPlayer.getCurrentPosition());
                        //这里为了进度条更加明显点
                        sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //变成横屏了
            setVideoParams(mMediaPlayer, true);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //变成竖屏了
            setVideoParams(mMediaPlayer, false);
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        switch (seekBar.getId()) {
            case R.id.sb_progress:
                try {
                    mMediaPlayer.seekTo(progress);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.sb_vol:
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    /**
     * 停止视频播放的方法
     */
    public void stop() {
        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.pause();
                mMediaPlayer.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            canProgress = false;
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
}


