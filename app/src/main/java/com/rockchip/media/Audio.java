package com.rockchip.media;

import android.media.AudioRecord;
import static com.rockchip.tutk.media.AacCodec.SAMPLE_RATE;
/**
 * Created by qiujian on 2017/3/25.
 */

public class Audio {
    static {
        System.loadLibrary("RK_VoiceProcess");
        System.loadLibrary("native-lib");
    }
    static  int MAX_BUFF=1280;
    public static int getBufferSize(int  channel, int format){
        int minBufSize =AudioRecord.getMinBufferSize(SAMPLE_RATE, channel, format);
        if ( minBufSize >MAX_BUFF){
            return MAX_BUFF;
        }
        return minBufSize;
    }
    public native static int init(int buffer_size);
    public native static void destory();
    public native static void begin_tx();
    public native static void end_tx();
    public native static  int audio_rx(byte[] src,int src_size,byte[] dest,int dest_size);
    public native static  int audio_tx(byte[] src,int src_size,byte[] dest,int dest_size,byte[] rx_out);
}
