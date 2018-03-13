package com.taped.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by varie on 13/03/2018.
 */
public class ListenThread extends Thread {

    private Thread t;
    private IListenCallback _callback;
    ListenThread(IListenCallback callback) {
        _callback = callback;
    }

    private void listen()
    {
        try {
            DatagramSocket serverSocket = new DatagramSocket(9876);
            byte[] receiveData = new byte[1024];
            byte[] sendData = new byte[1024];
            while(true)
            {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String call = new String( receivePacket.getData());
                System.out.println("RECEIVED: " + call);
                InetAddress IPAddress = receivePacket.getAddress();
                int port = receivePacket.getPort();

                _callback.callback(call);

                String response = "ok";
                sendData = response.getBytes();
                DatagramPacket sendPacket =
                        new DatagramPacket(sendData, sendData.length, IPAddress, port);
                serverSocket.send(sendPacket);
            }
        }
        catch (Exception e)
        {

        }

    }

    public void run() {
        listen();
    }

    public void start () {
        if (t == null) {
            t = new Thread (this);
            t.start ();
        }
    }

}
