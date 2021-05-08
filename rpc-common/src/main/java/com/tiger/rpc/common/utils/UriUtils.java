package com.tiger.rpc.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: UriUtils.java
 *
 * @Description: uri处理工具
 *
 * @Author: Tiger
 *
 * @Date: 2021/5/8
 */
public class UriUtils {

    /**
     * 根据传入的host:port列表，获取uris
     * @param hostPorts	protocol://host:port 或者 host:port 或者 host
     * @param protocol
     * @return	protocol://host:port or protocol://host:null
     */
    public static List<String> getUris(List<String> hostPorts, String protocol) {
        List<String> uris = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        String[] elements;
        String portRegex = Constants.LINE_START_REGEX + Constants.PORT_VALUE_REGEX;
        for (String hostPort : hostPorts) {
            if (StringUtils.isBlank(hostPort)) {
                //跳过为空的
                continue;
            }
            sb.setLength(0);
            //拼接协议部分： protocol://
            sb.append(protocol).append(Constants.PROTOCOL_HOST_SEPARATOR);
            //协议分割
            elements = hostPort.split(Constants.PROTOCOL_HOST_SEPARATOR);
            //host:port分割，无协议第1个元素分割，有协议第2个元素分割，
            elements = elements.length == 1 ? elements[0].split(Constants.HOST_PORT_SEPARATOR) : elements[1].split(Constants.HOST_PORT_SEPARATOR) ;
            if (StringUtils.isBlank(elements[0])) {
                //host为空，跳过
                continue;
            }
            if (elements.length == 1) {
                //只有host，拼接：host:null
                sb.append(elements[0]).append(Constants.HOST_PORT_SEPARATOR).append("null");
            } else if (Constants.NULL_STR.equalsIgnoreCase(elements[1].trim()) || elements[1].trim().matches(portRegex)) {
                //有host和port，port为'null' or 在[1~65535]，拼接：host:port
                sb.append(elements[0]).append(Constants.HOST_PORT_SEPARATOR).append(elements[1]);
            } else {
                //跳过端口校验不通过的元素
                continue;
            }
            uris.add(sb.toString());
        }
        return uris;
    }

    /**
     * 根据传入的host:port列表，获取uris
     * @param hostPorts	protocol://host:port 或者 host:port 或者 host
     * @param protocol
     * @return	protocol://host:port or protocol://host:null
     */
    public static List<String> getUrisNotNullPort(List<String> hostPorts, String protocol) {
        List<String> uris = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        String[] elements;
        String portRegex = Constants.LINE_START_REGEX + Constants.PORT_VALUE_REGEX;
        for (String hostPort : hostPorts) {
            if (StringUtils.isBlank(hostPort)) {
                //跳过为空的
                continue;
            }
            sb.setLength(0);
            //拼接协议部分： protocol://
            sb.append(protocol).append(Constants.PROTOCOL_HOST_SEPARATOR);
            //协议分割
            elements = hostPort.split(Constants.PROTOCOL_HOST_SEPARATOR);
            //host:port分割，无协议第1个元素分割，有协议第2个元素分割，
            elements = elements.length == 1 ? elements[0].split(Constants.HOST_PORT_SEPARATOR) : elements[1].split(Constants.HOST_PORT_SEPARATOR) ;
            if (StringUtils.isBlank(elements[0])) {
                //host为空，跳过
                continue;
            }
            if (elements.length >= 2 && elements[1].trim().matches(portRegex)) {
                //有host和port，port在[1~65535]，拼接：host:port
                sb.append(elements[0]).append(Constants.HOST_PORT_SEPARATOR).append(elements[1]);
            } else {
                //只有host or port非法，跳过
                continue;
            }
            uris.add(sb.toString());
        }
        return uris;
    }

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("thrift://stream001.tiger.vm:8081");
        list.add("thrift://stream002.tiger.vm");
        list.add("stream003.tiger.vm:8081");
        list.add("stream004.tiger.vm");
        list.add("stream005.tiger.vm:null");
        list.add("thrift://:null");
        List<String> list1 = getUris(list, "thrift");
        System.out.println(StringUtils.join(list1, ";"));
        System.out.println("--------------------------------------------");
        List<String> list2 = getUrisNotNullPort(list, "thrift");
        System.out.println(StringUtils.join(list2, ";"));

    }

}
