package com.tiger.rpc.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: NetworkUtils.java
 *
 * @Description: 网络工具
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/29
 */
@Slf4j
public class NetworkUtils {

    /**
     * 网络ip: 只计算一次
     */
    private static String ip = null;

    /**
     * 网络主机名：只计算一次
     */
    private static String hostName = null;

    /**
     * 主机名：只计算一次
     */
    private static String localHostName = null;

    /**
     * ip格式
     */
    private static Pattern IP_PATTERN = Pattern.compile(Constants.IP_PATTERN);

    public static String host(boolean useHostName) {
        return useHostName ? host() : ip();
    }

    /**
     * 获取网络主机名
     * @return
     */
    public static String host() {
        try {
            if(hostName == null){
                hostName = InetAddress.getLocalHost().getCanonicalHostName();
            }
        } catch (UnknownHostException e) {
            log.error("unknown host name.", e);
        }
        return hostName;
    }

    /**
     * 获取ip
     * @return
     */
    public static String ip() {
        try {
            if(ip == null){
                ip = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException e) {
            log.error("unknown host ip.", e);
        }
        return ip;
    }

    /**
     * 获取主机名
     * @return
     */
    public static String localHostName(){
        try {
            if(localHostName == null){
                localHostName = InetAddress.getLocalHost().getHostAddress();
            }
        } catch (UnknownHostException e) {
            log.error("unknown host local name.", e);
        }
        return localHostName;
    }

    /**
     * 判断host是否是ip地址
     * @param sourceIp  源地址
     * @return
     */
    public static boolean isIp(String sourceIp){
        if(StringUtils.isBlank(sourceIp)){
            return false;
        }
        try {
            Matcher matcher = IP_PATTERN.matcher(sourceIp);
            return matcher.matches();
        } catch (Exception e) {
            return false;
        }
    }

}
