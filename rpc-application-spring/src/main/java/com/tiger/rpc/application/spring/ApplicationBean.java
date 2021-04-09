package com.tiger.rpc.application.spring;

import com.tiger.rpc.common.config.ApplicationConfig;
import com.tiger.rpc.common.config.MonitorConfig;
import com.tiger.rpc.common.config.ZkConfig;
import com.tiger.rpc.common.provider.NoticeService;
import com.tiger.rpc.common.provider.SyncMachineService;
import com.tiger.rpc.common.register.ApplicationRegister;
import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.InitializingBean;

/**
 * @ClassName: ApplicationBean.java
 *
 * @Description: spring 方式管理rpc应用
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/14
 */
public class ApplicationBean extends ApplicationRegister implements InitializingBean {

    public ApplicationBean(ZkConfig zkConfig, ApplicationConfig appConf){
        super(zkConfig, appConf);
    }

    public ApplicationBean(ZkConfig zkConfig, CuratorFramework zkClient, ApplicationConfig appConf){
        super(zkConfig, zkClient, appConf);
    }

    public ApplicationBean(ZkConfig zkConfig, ApplicationConfig appConf, MonitorConfig monitorConfig, NoticeService noticeService, SyncMachineService syncMachineService){
        super(zkConfig, null, appConf, monitorConfig, noticeService, syncMachineService);
    }

    public ApplicationBean(ZkConfig zkConfig, CuratorFramework zkClient, ApplicationConfig appConf, MonitorConfig monitorConfig, NoticeService noticeService, SyncMachineService syncMachineService){
        super(zkConfig, zkClient, appConf, monitorConfig, noticeService, syncMachineService);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Preconditions.checkArgument(this.getZkConfig() != null, "ApplicationRegister.zkConfig can not be null");
        Preconditions.checkArgument(this.getAppConf() != null, "ApplicationRegister.appConfig can not be null");
        super.register();
    }


}
