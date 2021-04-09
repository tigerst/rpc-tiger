package com.tiger.rpc.common.provider;

import java.util.List;

/**
 * @ClassName: NoticeService.java
 *
 * @Description: 通知服务接口
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/6
 */
public interface NoticeService {

    /**
     * 通知应用相关owner
     * @param persons   owner
     * @param title 标题
     * @param msg   消息体
     * @throws Exception
     */
    public void notice(List<String> persons, String title, String msg) throws Exception;

}
