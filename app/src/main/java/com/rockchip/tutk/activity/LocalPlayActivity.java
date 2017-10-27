package com.rockchip.tutk.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
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

import com.rockchip.media.Audio;
import com.rockchip.tutk.R;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.RecordUtils;
import com.rockchip.tutk.view.CustomSeekbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static com.rockchip.tutk.media.AacCodec.SAMPLE_RATE;

/**
 * Created by waha on 2017/6/6.
 */

public class LocalPlayActivity extends AppCompatActivity implements
        SurfaceHolder.Callback, View.OnClickListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = "LocalPlay";
    public static final String EXTRA_FILENAME = "extra_filename";
    public static final String EXTRA_FILEPATH = "extra_filepath";
    private final int CHECK_TIME = 1000;
    private static int mAudioSource = MediaRecorder.AudioSource.MIC;    /* 录音源 */
    private static int mAudioChannel = AudioFormat.CHANNEL_IN_MONO;    /* 录音的声道，单声道 */
    private static int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;  /* 量化的深度 */
    private static int mBufferSize = Audio.getBufferSize(mAudioChannel, mAudioFormat);    /* 缓存的大小 */

    private SurfaceView surfaceView;
    private ProgressBar pbWait;
    private TextView txtCurDuration;
    private TextView txtTotalDuration;
    private CustomSeekbar sbDuration;
    private ImageButton btnPlay;
    private String mVideoPath;
    private String mAudioPath;
    private boolean mCancel;
    private MediaPlayer mMediaPlayer;
    private boolean mIsTracking;
    private int mOldDuration = 0;
    private AudioThread mAudioThread;
    private MediaCodec mAudioCodec = null;
    private AudioTrack mAudioTrack;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
    private int inputBufferIndex;
    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] decodeOutputBuffers;
    private static byte[] chunkPCM = new byte[4096];
    private Queue<byte[]> dataQueue = new ArrayDeque<>();
    private static final int AUDIO_BUF_SIZE = 1024;
    private static byte[] audioBuffer = new byte[AUDIO_BUF_SIZE];
    private final int DELAYED_PLAY_TIME = 1000;

    private int mScreenWidth = 0;
    private int mScreenHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private boolean mPause;
    private boolean mPlayComplete;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MsgDatas.MSG_PLAY_CHECK:
                    if (mCancel || null == mMediaPlayer) {
                        break;
                    }
                    int curDuration = mMediaPlayer.getCurrentPosition();
                    if (!mIsTracking) {
                        txtCurDuration.setText(secondFormat(curDuration / 1000));
                        sbDuration.setProgress(curDuration);
                    }
                    /*if (mOldDuration == curDuration && (mMediaPlayer.isPlaying() || curDuration == 0)
                            && View.GONE == pbWait.getVisibility()) {
                        pbWait.setVisibility(View.VISIBLE);
                    } else if (View.VISIBLE == pbWait.getVisibility()) {
                        pbWait.setVisibility(View.GONE);
                    }*/
                    mOldDuration = curDuration;
                    if (!isFinishing() && !mCancel) {
                        mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_PLAY_CHECK, CHECK_TIME);
                    }
                    break;
                case MsgDatas.MSG_PLAY_START:
                    if (!mCancel && null != mMediaPlayer) {
                        mMediaPlayer.start();
                    }
                    pbWait.setVisibility(View.GONE);
                    sbDuration.setCanTrack(true);
                    break;
            }
        }
    };
    //中间为空的情况要考虑，要sleep，是否预读后面在skip
    //拖动时候声音没停止

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_play);

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

        String fileName = intent.getStringExtra(EXTRA_FILENAME);
        mVideoPath = intent.getStringExtra(EXTRA_FILEPATH);
        if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(mVideoPath)) {
            Toast.makeText(this, R.string.txt_empty, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAudioPath = RecordUtils.getRecordAudioSaveName(mVideoPath, ".rkaac");
        File file = new File(mAudioPath);
        if (!file.exists()) {
            mAudioPath = null;
        }

        if (null != getSupportActionBar()) {
            //getSupportActionBar().setTitle(fileName);
            getSupportActionBar().hide();
        }
        Audio.init(mBufferSize);
    }

    private boolean prepare() {
        if (null == mMediaPlayer) {
            try {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.reset();
                mMediaPlayer.setDataSource(mVideoPath);
                mMediaPlayer.setDisplay(surfaceView.getHolder());
                mMediaPlayer.setOnPreparedListener(this);
                mMediaPlayer.setOnErrorListener(this);
                mMediaPlayer.setOnCompletionListener(this);
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

    private void startCheck() {
        mVideoWidth = mMediaPlayer.getVideoWidth();
        mVideoHeight = mMediaPlayer.getVideoHeight();
        scaleSurface();
        int totalDuration = mMediaPlayer.getDuration();
        txtTotalDuration.setText(secondFormat(totalDuration / 1000));
        sbDuration.setMax(totalDuration);
        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
        mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_CHECK);
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
    protected void onDestroy() {
        mCancel = true;
        mHandler.removeMessages(MsgDatas.MSG_PLAY_START);
        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
        if (null != mAudioThread) {
            mAudioThread.setCancel(true);
            if (mAudioThread.isAlive()) {
                mAudioThread.interrupt();
            }
        }
        if (null != mAudioCodec) {
            try {
                mAudioCodec.stop();
                mAudioCodec.release();
                mAudioCodec = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (null != mAudioTrack) {
            try {
                mAudioTrack.stop();
                mAudioTrack.release();
                mAudioTrack = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Audio.destory();
        super.onDestroy();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.v(TAG, "MediaPlayer onPrepared");
        if (null != mMediaPlayer) {
            mMediaPlayer.start();
            startAudio(0);
        }
        startCheck();
        sbDuration.setCanTrack(true);
        pbWait.setVisibility(View.GONE);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //mCancel = true;
        //mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_FINISH);
        Log.e(TAG, "onError what=" + what + ", extra=" + extra);
        finish();
        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
        //mCancel = true;
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
        prepare();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v(TAG, "surfaceDestroyed");
        //mCancel = true;
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
        mMediaPlayer.pause();
        mIsTracking = true;
        if (null != mAudioThread) {
            mAudioThread.setCancel(true);
            if (mAudioThread.isAlive()) {
                mAudioThread.interrupt();
                mAudioThread = null;
            }
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (null != mMediaPlayer && !mPause) {
            mMediaPlayer.seekTo(seekBar.getProgress());
            if (TextUtils.isEmpty(mAudioPath)) {
                //only h264
                mMediaPlayer.start();
            } else {
                sbDuration.setCanTrack(false);
                pbWait.setVisibility(View.VISIBLE);
                startAudio(seekBar.getProgress());
            }
        }
        mIsTracking = false;
        Log.v(TAG, "onStopTrackingTouch");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_play:
                if (View.VISIBLE == pbWait.getVisibility() || mIsTracking) {
                    return;
                }
                if (null != mMediaPlayer && mMediaPlayer.isPlaying()) {
                    mPause = true;
                    sbDuration.setCanTrack(false);
                    mMediaPlayer.pause();
                    btnPlay.setImageResource(R.drawable.play_24);
                } else if (null != mMediaPlayer) {
                    if (mPlayComplete) {
                        mMediaPlayer.seekTo(0);
                        sbDuration.setProgress(0);
                        txtCurDuration.setText("00:00");
                        mPlayComplete = false;
                        // mCancel = false;
                        mHandler.removeMessages(MsgDatas.MSG_PLAY_CHECK);
                        mHandler.sendEmptyMessage(MsgDatas.MSG_PLAY_CHECK);
                        startAudio(0);
                    }
                    mMediaPlayer.start();
                    mPause = false;
                    btnPlay.setImageResource(R.drawable.pause_24);
                    sbDuration.setCanTrack(true);
                }
                break;
        }
    }

    private void startAudio(int seekmsec) {
        if (null != mAudioPath) {
            if (null != mAudioThread) {
                mAudioThread.setCancel(true);
            }
            synchronized (dataQueue) {
                if (null != dataQueue) {
                    dataQueue.clear();
                }
                mAudioThread = new AudioThread(mAudioPath);
                mAudioThread.seekTo(seekmsec);
                mAudioThread.start();
            }
        }
    }

    class AudioThread extends Thread {
        private boolean cancel;
        private String audioPath;
        private int seekTime;

        public AudioThread(String audioPath) {
            this.audioPath = audioPath;
        }

        public void seekTo(int msec) {
            Log.v(TAG, "seekTo:" + msec);
            seekTime = msec;
        }

        public void setCancel(boolean cancel) {
            this.cancel = cancel;
        }

        @Override
        public void run() {
            doing();
        }

        private void doing() {
            FileInputStream fis = null;
            try {
                File file = new File(audioPath);
                fis = new FileInputStream(file);
                byte[] header = new byte[8];
                int readLen = 0;
                initAACMediaEncode();
                initAudioTrack();
                long usedTime = 0;
                int dataLen = 0;
                int dataInterval = 0;
                int skipTime = 0;
                int skipPos = 0;
                while (!cancel && !mPlayComplete) {
                    //pause
                    if (mPause) {
                        sleep(200);
                        continue;
                    }
                    if (seekTime > 0) {
                        synchronized (dataQueue) {
                            if (null != dataQueue) {
                                dataQueue.clear();
                            }
                        }
                        if (null != fis) {
                            try {
                                fis.close();
                            } catch (Exception e) {

                            }
                        }
                        fis = new FileInputStream(file);
                        while (seekTime > 0) {
                            //read header
                            readLen = fis.read(header);
                            if (readLen == -1) {
                                return;
                            }
                            dataLen = RecordUtils.bytesToInt(header, 0, 4);
                            dataInterval = RecordUtils.bytesToInt(header, 4, 4);
                            skipTime += dataInterval;
                            if (seekTime <= skipTime) {
                                usedTime = seekTime - (skipTime - dataInterval);
                                Log.v(TAG, "send play start after seek");
                                mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_PLAY_START, DELAYED_PLAY_TIME);
                                try {
                                    fis.close();
                                } catch (Exception e) {

                                }
                                fis = new FileInputStream(file);
                                seekTime = 0;
                                sleep(DELAYED_PLAY_TIME);
                                break;
                            }
                            fis.skip(dataLen);
                            skipPos = skipPos + 8 + dataLen;
                        }
                    }
                    if (skipPos > 0) {
                        Log.v(TAG, "skippos=" + skipPos);
                        fis.skip(skipPos);
                        skipPos = 0;
                    }
                    //read header
                    readLen = fis.read(header);
                    if (readLen == -1) {
                        break;
                    }
                    dataLen = RecordUtils.bytesToInt(header, 0, 4);
                    dataInterval = RecordUtils.bytesToInt(header, 4, 4);
                    //Log.v(TAG, dataLen + "=======" + dataInterval);
                    long sleepTime = dataInterval - usedTime;
                    if (sleepTime > 0) {
                        sleep(sleepTime);
                    }
                    byte[] audioData = new byte[dataLen];
                    //read audiodata
                    long startTime = System.currentTimeMillis();
                    readLen = fis.read(audioData);
                    if (readLen == -1) {
                        break;
                    }
                    if (!mCancel && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        synchronized (dataQueue) {
                            byte[] bytes = Arrays.copyOf(audioData, dataLen);
                            dataQueue.add(bytes);
                            if (dataQueue.size() > 40) {
                                dataQueue.clear();
                            }
                            //Log.d(TAG, String.format("byte=  %d  dataQueue= %d", dataLen, dataQueue.size()));
                        }
                    } else if (!mCancel) {
                        onAudioFrame(audioData);
                    }
                    usedTime = System.currentTimeMillis() - startTime;
                }
            } catch (Exception e) {
                Log.e(TAG, "happen error when play audio");
                e.printStackTrace();
            } finally {
                if (null != fis) {
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fis = null;
                }
                if (null != mAudioCodec) {
                    try {
                        mAudioCodec.stop();
                        mAudioCodec.release();
                        mAudioCodec = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (null != mAudioTrack) {
                    try {
                        mAudioTrack.stop();
                        mAudioTrack.release();
                        mAudioTrack = null;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void initAACMediaEncode() {
            try {
                MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 2);//参数对应-> mime type、采样率、声道数
                ByteBuffer wrap = null;
                if (SAMPLE_RATE == 44100) {
                    wrap = ByteBuffer.wrap(new byte[]{0x12, 0x10});
                } else if (SAMPLE_RATE == 16000) {
                    wrap = ByteBuffer.wrap(new byte[]{0x14, 0x10});
                }
                audioFormat.setByteBuffer("csd-0", wrap);
                audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);//比特率
                audioFormat.setInteger(MediaFormat.KEY_IS_ADTS, 0);
                audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//作用于inputBuffer的大小
                mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioCodec.setCallback(new MediaCodec.Callback() {
                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onInputBufferAvailable(MediaCodec mediaCodec, int i) {
                            synchronized (dataQueue) {
                                try {
                                    byte[] poll = null;//
                                    try {
                                        poll = dataQueue.poll();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    if (poll != null) {
                                        //Log.v(TAG, "mediaCodec.queueInputBuffer poll.length=" + poll.length);
                                        ByteBuffer inputBuffer = mediaCodec.getInputBuffer(i);
                                        inputBuffer.clear();
                                        inputBuffer.put(poll, 7, poll.length - 7);
                                        mediaCodec.queueInputBuffer(i, 7, poll.length - 7, 0, 0);
                                        // Log.v(TAG, "mediaCodec.queueInputBuffer(i,7,poll.length-7,0,0);");
                                    } else {
                                        mediaCodec.queueInputBuffer(i, 0, 0, 0, 0);
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "mediaCodec.error");
                                    e.printStackTrace();
                                }
                            }
                        }

                        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                        @Override
                        public void onOutputBufferAvailable(MediaCodec mediaCodec, int i, MediaCodec.BufferInfo bufferInfo) {
                            //Log.e(TAG, "onOutputBufferAvailable");
                            try {
                                ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
                                outputBuffer.get(chunkPCM);
                                outputBuffer.clear();
                                playAudio(chunkPCM);
                                mediaCodec.releaseOutputBuffer(i, false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {
                            Log.e(TAG, "onError");
                        }

                        @Override
                        public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
                            Log.e(TAG, "onOutputFormatChanged");
                        }
                    });
                }
                mAudioCodec.configure(audioFormat, null, null, 0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (mAudioCodec == null) {
                Log.e(TAG, "create mediaEncode failed");
                return;
            }
            mAudioCodec.start();
        }

        private void initAudioTrack() {
            if (mAudioTrack == null) {
                mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        mBufferSize,
                        AudioTrack.MODE_STREAM);
                mAudioTrack.play();
            }
        }

        public boolean onAudioFrame(byte[] audioData) {
            if (mAudioCodec == null) {
                return false;
            }
            try {
                inputBufferIndex = mAudioCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    inputBuffers = mAudioCodec.getInputBuffers();
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();

                    inputBuffer.put(audioData, 7, audioData.length - 7);
                    mAudioCodec.queueInputBuffer(inputBufferIndex, 7, audioData.length - 7, 0, 0);

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 10000);
            ByteBuffer outputBuffer;
            if (outputBufferIndex >= 0) {
                decodeOutputBuffers = mAudioCodec.getOutputBuffers();
                outputBuffer = decodeOutputBuffers[outputBufferIndex];//拿到用于存放PCM数据的Buffer
                if (chunkPCM == null || chunkPCM.length < bufferInfo.size) {
                    chunkPCM = new byte[bufferInfo.size];//BufferInfo内定义了此数据块的大小
                }
                outputBuffer.get(chunkPCM);//将Buffer内的数据取出到字节数组中
                outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buff

                playAudio(chunkPCM);
                mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                Log.e(TAG, "OutputBuffer INFO_OUTPUT_BUFFERS_CHANGED");
                decodeOutputBuffers = mAudioCodec.getOutputBuffers();
            } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.e(TAG, "OutputBuffer INFO_OUTPUT_FORMAT_CHANGED");
                // Subsequent data will conform to new format.
                MediaFormat format = mAudioCodec.getOutputFormat();
            } else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                Log.e(TAG, "dequeueOutputBuffer timed out!");
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                //break;
            }

            //Log.i("onAudioFrame", "onFrame end");
            return true;
        }

        private void playAudio(byte[] pcm) {
            synchronized (mAudioTrack) {
                if (mAudioTrack != null) {
                    byte destData[] = new byte[pcm.length];
                    int audio_size = Audio.audio_rx(pcm, pcm.length, destData, destData.length);
                    mAudioTrack.write(destData, 0, audio_size);
                }
            }
        }
    }
}
