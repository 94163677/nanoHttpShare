package air.kanna.nanoHttpSharePC.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 参考网址：https://cloud.tencent.com/developer/article/1407559
 *
 */
public class NetworkUtil {

    /**
     * 获取IPV4地址列表
     * @return
     * @throws SocketException
     */
    public static List<Inet4Address> getLocalIpv4Address() throws SocketException {
        List<Inet4Address> addresses = new ArrayList<>(1);
        Enumeration<NetworkInterface> networkEnum = NetworkInterface.getNetworkInterfaces();
        
        if(networkEnum == null) {
            return addresses;
        }
        
        while(networkEnum.hasMoreElements()) {
            NetworkInterface network = networkEnum.nextElement();
            if(!isValidInterface(network)) {
                continue;
            }
            
            Enumeration<InetAddress> inetEnum = network.getInetAddresses();
            while (inetEnum.hasMoreElements()) {
                InetAddress ipAdd = inetEnum.nextElement();
                if (isValidAddress(ipAdd)) {
                    addresses.add((Inet4Address)ipAdd);
                }
            }
        }
        return addresses;
    }
    
    /**
     * 过滤回环网卡、点对点网卡、非活动网卡、虚拟网卡
     * 取网卡名字是eth或者ens开头
     * 排除网卡描述有VMware的
     * @param ni 网卡
     * @return 如果满足要求则true，否则false
     * @throws SocketException
     */
    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() 
                && !ni.isPointToPoint() 
                && ni.isUp() 
                && !ni.isVirtual()
                && (ni.getName().startsWith("eth") 
                        || ni.getName().startsWith("ens"))
                && !(ni.getDisplayName().indexOf("VMware") >= 0);
    }
    
    /**
     * 判断是否是IPv4，并且内网地址并过滤回环地址.
     * @param address
     * @return
     */
    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address 
                && address.isSiteLocalAddress() 
                && !address.isLoopbackAddress();
    }
}
