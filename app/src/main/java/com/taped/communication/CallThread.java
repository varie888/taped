package com.taped.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by varie on 13/03/2018.
 */
public class CallThread extends Thread {
    private Thread t;

    private String _srcip;
    private String _targetip;
    private ICallCallback _callback;

    CallThread(ICallCallback callback, String srcip, String targetip) {
        _srcip = srcip;
        _targetip = targetip;
        _callback = callback;
    }

    private boolean call()
    {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(_targetip);
            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];
            String tuktuk = _srcip + " Calling";
            sendData = tuktuk.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            clientSocket.send(sendPacket);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            clientSocket.receive(receivePacket);
            String res = new String(receivePacket.getData());
            clientSocket.close();

            return (res.equals("ok"));
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public void run() {
        boolean res = call();

        _callback.callback(res);
    }

    public void start () {
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }
}
