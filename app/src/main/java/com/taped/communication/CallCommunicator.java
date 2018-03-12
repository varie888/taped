package com.taped.communication;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created by varie on 12/03/2018.
 */

public class CallCommunicator {
    public static boolean call(String srcip, String targetip)
    {
        try {
            DatagramSocket clientSocket = new DatagramSocket();
            InetAddress IPAddress = InetAddress.getByName(targetip);
            byte[] sendData = new byte[1024];
            byte[] receiveData = new byte[1024];
            String tuktuk = srcip + " Calling";
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
}
