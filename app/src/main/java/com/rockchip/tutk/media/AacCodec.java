package com.rockchip.tutk.media;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.rtp.AudioCodec;
import android.os.Build;
import android.util.Log;

import com.rockchip.tutk.TUTKDevice;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.AVFrame;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.Packet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class AacCodec {
    private static final String TAG = "AacCodec";
    public static final int SAMPLE_RATE = 16000;
    private String encodeType;
    private String dstPath;
    private MediaCodec mediaEncode;
    private MediaExtractor mediaExtractor;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;
    private MediaCodec.BufferInfo encodeBufferInfo;
    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private ArrayList<byte[]> chunkPCMDataContainer;//PCM数据块容器
    private OnCompleteListener onCompleteListener;
    private OnProgressListener onProgressListener;

    private static AacCodec mAacCodec;
    private TUTKDevice tutkDevice;
    AVAPIs av = new AVAPIs();
    int avIndex = -1;
    private int speakIndex = -1;
    private File aacfile;

    public class FrameInfo {
        public short codec_id;
        public byte flags;
        public byte cam_index;
        public byte onlineNum;
        public byte[] reserve1 = new byte[3];
        public int reserve2;
        public int timestamp;

        public byte[] parseContent(short _codec_id, byte _flags) {

            byte[] result = new byte[16];
            byte[] arg1 = Packet.shortToByteArray_Little(_codec_id);
            byte[] arg2 = new byte[1];
            arg2[0] = _flags;

            System.arraycopy(arg1, 0, result, 0, 2);
            System.arraycopy(arg2, 0, result, 2, 1);
            return result;
        }
    }

    public static AacCodec getInstance() {
        if (mAacCodec == null) {
            mAacCodec = new AacCodec();
        }
        return mAacCodec;
    }

    /**
     * 设置编码器类型
     *
     * @param encodeType
     */
    public void setEncodeType(String encodeType) {
        this.encodeType = encodeType;
    }

    /**
     * 设置输入输出文件位置
     *
     * @param dstPath
     */
    public void setIOPath(String dstPath) {
//      this.srcPath=srcPath;
        this.dstPath = dstPath;
    }

    public void setDevice(TUTKDevice device) {
        this.tutkDevice = device;
    }

    /**
     * 此类已经过封装
     * 调用prepare方法 会初始化Decode 、Encode 、输入输出流 等一些列操作
     */
    public void prepare() {
        if (encodeType == null) {
            throw new IllegalArgumentException("encodeType can't be null");
        }

        if (tutkDevice == null) {
            throw new IllegalArgumentException("tutkDevice can't be null");
        }

        String dir = "/sdcard/IPC/";
        new File(dir).mkdirs();
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String date = sDateFormat.format(new java.util.Date());
        aacfile = new File(String.format("%saudio_%s.aac", dir, date));
        if (aacfile.exists()) {
            aacfile.delete();
        }
        try {
            aacfile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*try {
            fos = new FileOutputStream(new File(dstPath));
            bos = new BufferedOutputStream(fos, 200*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        chunkPCMDataContainer = new ArrayList<>();

        if (encodeType == MediaFormat.MIMETYPE_AUDIO_AAC) {
            initAACMediaEncode();//AAC编码器 
        } else if (encodeType == MediaFormat.MIMETYPE_AUDIO_MPEG) {
            initMPEGMediaEncode();//mp3编码器 
        }

        if (avIndex < 0) {
            avIndex = tutkDevice.getSession().getAVIndex();
        }

        speakIndex = tutkDevice.getSession().startSpeakServer();
    }

    /**
     * 初始化AAC编码器
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initAACMediaEncode() {
        try {
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(encodeType, SAMPLE_RATE, 2);//参数对应-> mime type、采样率、声道数
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 32000);//比特率
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 8192 * 2);
            encodeFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.ENCODING_PCM_16BIT);
            mediaEncode = MediaCodec.createEncoderByType(encodeType);
            mediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaEncode == null) {
            Log.e(TAG, "create mediaEncode failed");
            return;
        }
        mediaEncode.start();
        encodeInputBuffers = mediaEncode.getInputBuffers();
        encodeOutputBuffers = mediaEncode.getOutputBuffers();
        encodeBufferInfo = new MediaCodec.BufferInfo();
    }

    /**
     * 初始化MPEG编码器
     */
    private void initMPEGMediaEncode() {

    }

    private static boolean codeOver = false;

    /**
     * 开始转码
     * PCM数据在编码成想要得到的{@link #encodeType}音频格式
     * PCM->aac
     */
    public void startAsync() {
        showLog("start");
        codeOver = false;
        new Thread(new EncodeRunnable()).start();
    }

    public void stopAsync() {
        release();
        codeOver = true;
    }

    /**
     * 将PCM数据存入{@link #chunkPCMDataContainer}
     *
     * @param pcmChunk PCM数据块
     */
    public void putPCMData(byte[] pcmChunk) {
        synchronized (AudioCodec.class) {//记得加锁
            chunkPCMDataContainer.add(pcmChunk);
        }
    }

    /**
     * 在Container中{@link #chunkPCMDataContainer}取出PCM数据
     *
     * @return PCM数据块
     */
    private byte[] getPCMData() {
        synchronized (AudioCodec.class) {//记得加锁
            //showLog("getPCM:" + chunkPCMDataContainer.size());
            if (chunkPCMDataContainer.isEmpty()) {
                return null;
            }

            byte[] pcmChunk = chunkPCMDataContainer.get(0);//每次取出index 0 的数据 
            chunkPCMDataContainer.remove(pcmChunk);//取出后将此数据remove掉 既能保证PCM数据块的取出顺序 又能及时释放内存 
            return pcmChunk;
        }
    }

    /**
     * 编码PCM数据 得到{@link #encodeType}格式的音频文件，并保存到{@link #dstPath}
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void dstAudioFormatFromPCM() {
        int inputIndex;
        ByteBuffer inputBuffer;
        int outputIndex;
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        byte[] chunkPCM;

        for (int i = 0; i < encodeInputBuffers.length - 1; i++) {
            chunkPCM = getPCMData();  //获取解码器所在线程输出的数据 代码后边会贴上
            if (chunkPCM == null) {
                break;
            }
            inputIndex = mediaEncode.dequeueInputBuffer(-1);//同解码器 
            inputBuffer = encodeInputBuffers[inputIndex];//同解码器 
            inputBuffer.clear();    //同解码器 
            inputBuffer.limit(chunkPCM.length);
            inputBuffer.put(chunkPCM);//PCM数据填充给inputBuffer
            mediaEncode.queueInputBuffer(inputIndex, 0, chunkPCM.length, 0, 0);//通知编码器 编码 
        }

        outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);//同解码器  
        while (outputIndex >= 0) {//同解码器 
            outBitSize = encodeBufferInfo.size;
            outPacketSize = outBitSize + 7; //7为ADTS头部的大小
            outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
            outputBuffer.position(encodeBufferInfo.offset);
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
            chunkAudio = new byte[outPacketSize];
            addADTStoPacket(chunkAudio, outPacketSize);//添加ADTS 代码后面会贴上
            outputBuffer.get(chunkAudio, 7, outBitSize);//将编码得到的AAC数据 取出到byte[]中 偏移量offset=7 你懂得 
            outputBuffer.position(encodeBufferInfo.offset);
            try {

                try {

                    OutputStream outputStream = new FileOutputStream(aacfile, true);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                    bufferedOutputStream.write(chunkAudio, 0, chunkAudio.length);
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //bos.write(chunkAudio, 0, chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
                /*if (avIndex<0){
                    avIndex=tutkDevice.getSession().getAVIndex();
                }*/
                FrameInfo frame = new FrameInfo();
                frame.codec_id = AVFrame.MEDIA_CODEC_VIDEO_MJPEG;
                frame.flags = AVFrame.IPC_FRAME_FLAG_IFRAME;
                byte[] buf_info = frame.parseContent(frame.codec_id, frame.flags);
                int i = av.avSendAudioData(speakIndex, chunkAudio, chunkAudio.length, buf_info, buf_info.length);
                if (i < 0) {
                    Log.d(TAG, "write to ipc:" + i);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaEncode.releaseOutputBuffer(outputIndex, false);
            outputIndex = mediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
        }
    }

    /**
     * 添加ADTS头
     *
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE
        if (SAMPLE_RATE == 44100) {
            freqIdx = 4;
        } else if (SAMPLE_RATE == 16000) {
            freqIdx = 8;
        }
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /**
     * 释放资源
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void release() {
        try {
            byte[] info = new byte[8];
            byte[] bytes = Packet.intToByteArray_Little(speakIndex);
            for (int i = 0; i < bytes.length; i++) {
                info[i] = bytes[i];
            }
            int i = av.avSendIOCtrl(avIndex, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTOP, info, info.length);
            Log.d(TAG, "IOTYPE_USER_IPCAM_SPEAKERSTOP=ret=" + i);

            tutkDevice.getSession().stopSpeakServer();
            speakIndex = -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            if (bos != null) {
                bos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bos = null;
                }
            }
        }

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos = null;
        }

        if (mediaEncode != null) {
            try{
            mediaEncode.stop();
            mediaEncode.release();
            }catch (Exception e){
                e.printStackTrace();
            }
            mediaEncode = null;
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
            mediaExtractor = null;
        }

        if (onCompleteListener != null) {
            onCompleteListener = null;
        }

        if (onProgressListener != null) {
            onProgressListener = null;
        }
        showLog("release");
    }

    /**
     * 编码线程
     */
    private class EncodeRunnable implements Runnable {

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            while (!codeOver) {
                if (!chunkPCMDataContainer.isEmpty()) {
                    try {
                        dstAudioFormatFromPCM();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            if (onCompleteListener != null) {
                onCompleteListener.completed();
            }
            showLog("time:" + (System.currentTimeMillis() - t));
        }
    }

    /**
     * 转码完成回调接口
     */
    public interface OnCompleteListener {
        void completed();
    }

    /**
     * 转码进度监听器
     */
    public interface OnProgressListener {
        void progress();
    }

    /**
     * 设置转码完成监听器
     *
     * @param onCompleteListener
     */
    public void setOnCompleteListener(OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    private void showLog(String msg) {
        Log.e("AudioCodec", msg);
    }
}
