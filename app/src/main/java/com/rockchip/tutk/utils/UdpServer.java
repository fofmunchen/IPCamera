package com.rockchip.tutk.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.rockchip.tutk.TUTKManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketException;

/**
 * Created by xiac on 2017/8/22.
 */
public class UdpServer implements Runnable {
    private String ip = null;
    private int port = 0;
    private MulticastSocket ds = null;
    InetAddress receiveAddress;

    private DatagramPacket dpRcv = null, dpSend = null;
   // private static DatagramSocket ds = null;
    private InetSocketAddress inetSocketAddr = null;
    private byte[] msgRcv = new byte[16];
    private boolean udpLife = true;
    private boolean udbLifeOver = true;
    private Context context;
    String multicastHost = "224.0.0.1";
    public UdpServer(Context mcontext, int mPort) {
        this.context = mcontext;
       // this.ip = mIp;
        this.port = mPort;
        try {
            ds = new MulticastSocket(port);
            receiveAddress=InetAddress.getByName(multicastHost);
            ds.joinGroup(receiveAddress);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        dpRcv = new DatagramPacket(msgRcv, msgRcv.length);
        while(udpLife){
            try {
                Log.i("SocketInfo", "UDP监听中");
                ds.receive(dpRcv);
                String rcv = new String(dpRcv.getData(), dpRcv.getOffset(), dpRcv.getLength());
                Log.e("SocketInfo", "收到信息：" + rcv);
                String uid = rcv.trim();
                Log.e("SocketInfo", "send uid：" + uid);
                Intent intent = new Intent(TUTKManager.ACTION_TUTK_DEVICE_ATTACHED);
                intent.putExtra("UID", uid);
                intent.putExtra("CONFIG", "OK");
                intent.putExtra("EXTRA", "REFRESH");
                //intent.putExtra("UID", "1234567");
                if (context != null) {
                    context.sendBroadcast(intent);
                    udpLife = false;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        Log.e("SocketInfo","UDP监听关闭");
        ds.close();
        //udp生命结束
    }

//    @Override
//    public void run() {
//        Log.e("","---------run----");
//        while (udpLife) {
//        inetSocketAddr = new InetSocketAddress("224.0.0.1", port);
//        try {
//            Log.e("SocketInfo", "UDP服务器已经启动");
//            ds = new DatagramSocket(inetSocketAddr);
//
//
//            //设置超时，不需要可以删除
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//        dpRcv = new DatagramPacket(msgRcv, msgRcv.length);
//
//        Log.e("","---------while----");
//        try {
//            Log.i("SocketInfo", "UDP监听中");
//            ds.receive(dpRcv);
//
//            String uid = new String(dpRcv.getData(), dpRcv.getOffset(), dpRcv.getLength());
//            Log.e("SocketInfo", "收到信息：" + uid);
//            Intent intent = new Intent(TUTKManager.ACTION_TUTK_DEVICE_ATTACHED);
//            intent.putExtra("UID", uid);
//            intent.putExtra("CONFIG", "OK");
//            //intent.putExtra("UID", "1234567");
//            if (context != null) {
//                context.sendBroadcast(intent);
//                udpLife = false;
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        }
//        Log.e("SocketInfo","UDP监听关闭");
//        ds.close();
//        //udp生命结束
//    }

    public void UdpSockteClose(){
        Log.e("","---UdpSockteClose---");
        udpLife = false;
        ds.close();
    }
}
