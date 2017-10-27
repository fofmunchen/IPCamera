package com.rockchip.tutk.media;

/**
 * Created by qiujian on 2017/1/19.
 */

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.net.rtp.AudioCodec;
import android.util.Log;

import com.tutk.IOTC.AVFrame;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.rockchip.tutk.media.AudioCodec.AUDIO_FORMAT;
import static com.rockchip.tutk.media.AudioCodec.BUFFFER_SIZE;
import static com.rockchip.tutk.media.AudioCodec.KEY_AAC_PROFILE;
import static com.rockchip.tutk.media.AudioCodec.KEY_BIT_RATE;
import static com.rockchip.tutk.media.AudioCodec.KEY_CHANNEL_COUNT;
import static com.rockchip.tutk.media.AudioCodec.KEY_SAMPLE_RATE;
import static com.rockchip.tutk.media.AudioCodec.MIME_TYPE;
import static com.rockchip.tutk.media.AudioCodec.WAIT_TIME;

/**
 * @author zhangsutao
 * @file AudioDecoder
 * @brief aac音频解码播放器
 * @date 2016/8/7
 */
public class AudioDecoder  {
    private static final String TAG ="AudioDecoder";
    private Worker mWorker;
    //private Server mServer;
    private byte[] mPcmData;
    public AudioDecoder()  {


    }

    public void start(){
        if(mWorker==null){
            mWorker=new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }
    public void stop(){
        if(mWorker!=null){
            mWorker.setRunning(false);
            mWorker=null;
        }

    }

    private class Worker extends Thread{
        private boolean isRunning=false;
        private AudioTrack mPlayer;
        private MediaCodec mDecoder;
        MediaCodec.BufferInfo mBufferInfo;
        public void setRunning(boolean run){
            isRunning=run;
        }
        @Override
        public void run() {
            super.run();
            if(!prepare(null)){
                isRunning=false;
                Log.d(TAG,"音频解码器初始化失败");
            }
            while(isRunning){
                decode(null);
            }
            release();
        }

        /**
         * 等待客户端连接，初始化解码器
         * @return 初始化失败返回false，成功返回true
         */
        public boolean prepare(AVFrame frame) {
            //等待客户端
            //mServer.start();
            mBufferInfo = new MediaCodec.BufferInfo();
            mPlayer=new AudioTrack(AudioManager.STREAM_MUSIC,KEY_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AUDIO_FORMAT,BUFFFER_SIZE,AudioTrack.MODE_STREAM);
            mPlayer.play();
            try {
                mDecoder = MediaCodec.createDecoderByType(MIME_TYPE);
                byte[] csd_0=frame.frmData;
                if(csd_0==null){
                    return false;
                }
                MediaFormat format = new MediaFormat();
                format.setString(MediaFormat.KEY_MIME,MIME_TYPE);
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, KEY_CHANNEL_COUNT);
                format.setInteger(MediaFormat.KEY_SAMPLE_RATE, KEY_SAMPLE_RATE);
                format.setInteger(MediaFormat.KEY_BIT_RATE, KEY_BIT_RATE);
                format.setInteger(MediaFormat.KEY_IS_ADTS,1);
                format.setInteger(MediaFormat.KEY_AAC_PROFILE,KEY_AAC_PROFILE);
                byte[] bytes = new byte[]{csd_0[7],csd_0[8]};
                ByteBuffer bb = ByteBuffer.wrap(bytes);
                format.setByteBuffer("csd-0", bb);
                mDecoder.configure(format, null, null, 0);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if (mDecoder == null) {
                Log.e(TAG, "create mediaDecode failed");
                return false;
            }
            mDecoder.start();
            return true;
        }

        /**
         * aac解码+播放
         */
        public void decode(AVFrame frame) {

            boolean isEOF=false;
            while(!isEOF){
                //获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
                int inputIndex = mDecoder.dequeueInputBuffer(-1);
                if(inputIndex>=0){
                    ByteBuffer inputBuffer = mDecoder.getInputBuffer(inputIndex);
                    if(inputBuffer==null){
                        return ;
                    }
                    inputBuffer.clear();
                    //Frame frame=mServer.readFrameWidthCache();
                    if(frame==null){
                        mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOF = true;
                        isRunning=false;
                        //服务已经断开，释放服务端
                        //mServer.release();
                    }else {
                        inputBuffer.put(frame.frmData,0,frame.getFrmSize());
                        mDecoder.queueInputBuffer(inputIndex, 0, frame.getFrmSize(), 0, 0);
                    }
                }else {
                    isEOF=true;
                }
                int outputIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, WAIT_TIME);
                Log.d(TAG,"audio decoding .....");
                ByteBuffer outputBuffer;
                //每次解码完成的数据不一定能一次吐出 所以用while循环，保证解码器吐出所有数据
                while (outputIndex >= 0) {
                    outputBuffer = mDecoder.getOutputBuffer(outputIndex);
                    if(mPcmData==null||mPcmData.length<mBufferInfo.size){
                        mPcmData=new byte[mBufferInfo.size];
                    }
//                        chunkPCM = new byte[mBufferInfo.size];
                    outputBuffer.get(mPcmData,0,mBufferInfo.size);
                    outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
                    //播放音乐
                    mPlayer.write(mPcmData,0,mBufferInfo.size);
                    mDecoder.releaseOutputBuffer(outputIndex, false);//此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
                    outputIndex = mDecoder.dequeueOutputBuffer(mBufferInfo, WAIT_TIME);//再次获取数据，如果没有数据输出则outputIndex=-1 循环结束
                }
            }
        }

        /**
         * 释放资源
         */
        private void release(){
            if(mDecoder!=null){
                mDecoder.stop();
                mDecoder.release();
            }
            if(mPlayer!=null){
                mPlayer.stop();
                mPlayer.release();
                mPlayer=null;
            }

        }
    }
}