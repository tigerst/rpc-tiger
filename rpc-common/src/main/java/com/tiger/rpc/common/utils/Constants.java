package com.tiger.rpc.common.utils;

/**
 * @ClassName: Constants.java
 *
 * @Description: 常量池
 *
 * @Author: Tiger
 *
 * @Date: 2019/1/24
 */
public class Constants {

    /**
     * 基路径
     */
    public static final String ROOT_PATH = "/soa";

    /**
     * 路径分隔符
     */
    public static final String PATH_SEPARATOR = "/";

    /**
     * 应用和版本分割符
     */
    public static final String APPLICATION_VERSION_SEPARATOR = "_";

    /**
     * 服务和版本分割符
     */
    public static final String SERVICE_VERSION_SEPARATOR = "_";

    /**
     * ip:port:weight
     */
    public static final String HOST_PORT_SEPARATOR = ":";

    /**
     * 协议与host分割符
     */
    public static final String PROTOCOL_HOST_SEPARATOR = "://";

    /**
     * 空字符串
     */
    public static final String NULL_STR = "null";

    /**
     * 最大端口号
     */
    public static final int PORT_MAX_VALUE = 65535;

    /**
     * 端口号正则匹配（1-65535）
     */
    public static final String PORT_VALUE_REGEX = "[1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]{1}|6553[0-5]$";

    /**
     * 行的开头正则
     */
    public static final String LINE_START_REGEX = "^";

    /**
     * 应用owner分割符
     */
    public static final String OWNER_SEPARATOR = "\\|";

    /**
     * 通知时间格式
     */
    public static final String NOTICE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 默认编码
     */
    public static final String DEFAULT_CHARSET = "UTF-8";

    /**
     * ip地址格式
     */
    public static final String IP_PATTERN = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

    /**
     * 逗号
     */
    public static final String COMMA = ",";

}
