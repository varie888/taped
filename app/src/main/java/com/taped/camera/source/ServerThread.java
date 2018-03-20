package com.taped.camera.source;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.taped.taped.ChatActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by varie on 16/03/2018.
 */
public class ServerThread{//} implements Runnable {
    /*private int mServerPort;
    private String mServerIP;
    private Context mContext;
    private Handler mHandler;
    private ChatActivity mActivityInstance;
    public ServerThread(Context context,String serverip,int serverport,Handler handler){
        super();
        mContext=context;
        mServerIP = serverip;
        mServerPort = serverport;
        mHandler = handler;
        mActivityInstance = (ChatActivity)mContext;
    }
    public void run(){
        try {
            ServerSocket ss = new ServerSocket(mServerPort);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //mActivityInstance.setServerStatusText("Listening on IP: " + mServerIP);
                }
            });
            while (true){
                Socket s = ss.accept();
                new Thread(new ServerSocketThread(s)).start();
            }
        }catch(Exception e){
            Log.d("ServerThread", "run: erro");
        }
    }

    public class ServerSocketThread implements Runnable{
        Socket s = null;;
        OutputStream os = null;
        public ServerSocketThread(Socket s) throws IOException {
            this.s = s;
        }
        @Override
        public void run() {
            if(s !=null){
                String clientIp = s.getInetAddress().toString().replace("/", "");
                int clientPort = s.getPort();
                System.out.println("====client ip====="+clientIp);
                System.out.println("====client port====="+clientPort);
                try {

                    s.setKeepAlive(true);
                    os = s.getOutputStream();
                    while(true){
                        //mActivityInstance.getPreview().takePicture();

                        if (mActivityInstance.getPreview().getFrameBuffer() == null)
                            continue;

                        DataOutputStream dos = new DataOutputStream(os);
                        dos.writeInt(4);
                        dos.writeUTF("#@@#");
                        dos.writeInt(mActivityInstance.getPreview().getFrameBuffer().size());
                        dos.writeUTF("-@@-");
                        dos.flush();
                        System.out.println(mActivityInstance.getPreview().getFrameBuffer().size());
                        dos.write(mActivityInstance.getPreview().getFrameBuffer().toByteArray());
                        dos.flush();
                        Thread.sleep(1000/10);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    try {
                        if (os!= null)
                            os.close();

                    } catch (Exception e2) {
                        e.printStackTrace();
                    }

                }
            }
            else{
                System.out.println("socket is null");

            }
        }

    }*/
}