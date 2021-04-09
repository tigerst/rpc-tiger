package com.tiger.rpc.common.enums;

import java.util.Objects;

/**
 * @ClassName: ServiceStatus.java
 *
 * @Description: 服务状态码
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/25
 */
public enum ServiceStatus {

    ENABLED(1),

    DISABLED(2),

    KILLED(3)
    ;

    Integer status;

    ServiceStatus(int status) {
        this.status = status;
    }

    private static final ServiceStatus[] WORKER_STATUSES = ServiceStatus.values();

    public static ServiceStatus of(Integer status) {
        for (ServiceStatus ac : WORKER_STATUSES) {
            if (Objects.equals(ac.getStatus(), status)) {
                return ac;
            }
        }
        throw new IllegalArgumentException("Unsupported WorkerStatus:" + status);
    }


    public Integer getStatus() {
        return status;
    }
}
