package com.taped.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by varie on 12/03/2018.
 */

public class CallCommunicator {

    public static void call(ICallCallback callback, String srcip, String targetip){
        CallThread th = new CallThread(callback, srcip, targetip);
        th.start();
    }

    public static void listen(IListenCallback callback) {
        ListenThread th = new ListenThread(callback);
        th.start();
    }
}
