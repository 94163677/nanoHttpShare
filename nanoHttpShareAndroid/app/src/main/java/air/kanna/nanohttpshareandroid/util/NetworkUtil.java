package air.kanna.nanohttpshareandroid.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;

public class NetworkUtil {

    private static Logger logger = null;

    public static boolean checkNetwork(Context context){
        if(context == null){
            throw new NullPointerException("Context is null");
        }
        NetworkInfo networkInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if(networkInfo == null || !networkInfo.isConnected()){
            return false;
        }
        return true;
    }

    public static String getIPAddress(Context context){
        if(context == null){
            throw new NullPointerException("Context is null");
        }
        initLogger();
        NetworkInfo networkInfo = ((ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if(networkInfo == null || !networkInfo.isConnected()){
            return null;
        }
        //使用移动网络
        if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
            return getMobileIP();
        }else
        if(networkInfo.getType() == ConnectivityManager.TYPE_WIFI){
            return getWifiIP(context);
        }else{
            logger.error("cannot support network type：" + networkInfo.getType());
        }

        return null;
    }

    private static String getWifiIP(Context context){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ipAddress = intIPV4ToString(wifiInfo.getIpAddress());
        return ipAddress;
    }

    private static String getMobileIP(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            logger.error("get Mobile IpAddress error", ex);
        }
        return null;
    }

    private static void initLogger(){
        if(logger != null){
            return;
        }
        logger = LoggerProvider.getLogger(NetworkUtil.class);
    }

    private static String intIPV4ToString(int ip) {
        return new StringBuilder().append(ip & 0xFF).append('.')
                .append((ip >> 8) & 0xFF).append('.')
                .append((ip >> 16) & 0xFF).append('.')
                .append(ip >> 24 & 0xFF)
                .toString();
    }
}
