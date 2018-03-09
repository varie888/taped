package com.taped.utils;

import android.content.Context;
import android.hardware.camera2.*;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

/**
 * Created by varie on 08/03/2018.
 */

public class Utils {
    /**
     * Get IP address from first non-localhost interface
     * @return  address or empty string
     */
    public static String getIPAddress(Context appContext) {
        try {
            final WifiManager mWifiManager = (WifiManager) appContext.getSystemService(Context.WIFI_SERVICE);
            if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
                int ip = mWifiManager.getConnectionInfo().getIpAddress();
                return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "."
                        + ((ip >> 24) & 0xFF);
            }
            return null;
        } catch (Exception ex) { } // for now eat exceptions
        return null;
    }
}
