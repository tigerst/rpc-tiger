package com.tiger.rpc.common.utils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: VMStat.java
 *
 * @Description: 计算系统资源：cpu、内存
 *
 * @Author: Tiger
 *
 * @Date: 2019/4/25
 */
@Slf4j
public class VMStat {
    private static final String PROCFS_STAT = "/proc/stat";
    private static final String PROCFS_MEMINFO = "/proc/meminfo";
    private static final String PROCFS_NETSTAT = "/proc/net/dev";

    private static final Map<String, Long> cache = Maps.newHashMapWithExpectedSize(3);
    private static final String CPU_TOTAL = "cpu_total";
    private static final String CPU_IDLE = "cpu_idle";
    private static final String CPU_COLLECT_TIME = "cpu_collect_time";

    /**
     * 获取操作系统名称
     * @return
     */
    public static String getOSName(){
        return System.getProperty("os.name");
    }


    /**
     * 虚拟机的最大可用的处理器数量
     * @return
     */
    public static int getProcessNum() {
        int cpuNum = 0;
        try {
            cpuNum = Runtime.getRuntime().availableProcessors();
        } catch (Exception e) {
            log.info("Failed to get CPU cores.", e);
        }
        return cpuNum;
    }

    /**
     * 分析cpu文件信息
     * @param file
     * @return
     */
    private static Map<String, Long> analyseCpuInfo(String file){
        Map<String, Long> result = Maps.newHashMap();
        List<String> lines;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            lines = IOUtils.readLines(fis, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Read cpu file[{}] error: {}", file, e.getMessage());
            result.put(CPU_TOTAL, cache.getOrDefault(CPU_TOTAL, -1L));
            result.put(CPU_IDLE, cache.getOrDefault(CPU_IDLE, -1L));
            return result;
        } finally {
            if (fis == null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.warn("Close fis error: {}", e.getMessage());
                }
                //置空加速回收
                fis = null;
            }
        }
        String[] values = lines.get(0).split("\\s+");
        long idleCpuTime = 0, totalCpuTime = 0;
        for (int i = 1; i < values.length; i++) {
            totalCpuTime += NumberUtils.toLong(values[i], 0L);
        }
        idleCpuTime = Long.valueOf(values[4]);
        log.debug("{}: {}, {}: {}", CPU_IDLE, idleCpuTime, CPU_TOTAL, totalCpuTime);
        result.put(CPU_TOTAL,totalCpuTime);
        result.put(CPU_IDLE, idleCpuTime);
        return result;
    }

    /**
     * 获取cpu瞬时使用率
     * @return
     */
    public static double getInstantCpuUsage(){
        double cpuUsage = 0.0;
        if (isLinux()) {
            //第一次采集CPU时间
            Map<String, Long> cpuInfo1 = analyseCpuInfo(PROCFS_STAT);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.warn("CpuUsage sleep InterruptedException.");
            }
            //第二次采集CPU时间
            Map<String, Long> cpuInfo2 = analyseCpuInfo(PROCFS_STAT);
            //分别为系统启动后空闲的CPU时间和总的CPU时间
            long idleCpuTime1 = cpuInfo1.getOrDefault(CPU_IDLE, 0L),
                    totalCpuTime1 = cpuInfo1.getOrDefault(CPU_TOTAL, 0L);
            long idleCpuTime2 = cpuInfo2.getOrDefault(CPU_IDLE, 0L),
                    totalCpuTime2 = cpuInfo2.getOrDefault(CPU_TOTAL, 0L);
            if(totalCpuTime1 != totalCpuTime2 && totalCpuTime1 != 0 && idleCpuTime1 != 0 && idleCpuTime2 != 0){
                //计算cpu使用率，规则：
                cpuUsage = 1 - (double)(idleCpuTime2 - idleCpuTime1)/(double)(totalCpuTime2 - totalCpuTime1);
                log.debug("Current cpu usage: {}", cpuUsage);
                //设置当前查询的数据
                cache.put(CPU_IDLE, idleCpuTime2);
                cache.put(CPU_TOTAL, totalCpuTime2);
                cache.put(CPU_COLLECT_TIME, System.currentTimeMillis());
            } else {
                log.warn("Collect info error. first collection: {}, second collection: {}.",
                        JSON.toJSONString(cpuInfo1), JSON.toJSONString(cpuInfo2));
            }
        }
        return cpuUsage;
    }

    /**
     * 计算cpu使用率
     * @return
     */
    public static double getCpuUsage() {
        if (isLinux()) {
            try {
                List<String> lines = IOUtils.readLines(new FileInputStream(PROCFS_STAT), StandardCharsets.UTF_8);
                long lastTotalCpuTime = 0, lastIdleCpuTime = 0;
                String[] values = lines.get(0).split("\\s+");
                for (int i = 1; i < values.length; i++) {
                    lastTotalCpuTime += Long.valueOf(values[i]);
                }
                lastIdleCpuTime = Long.valueOf(values[4]);

                long cacheIdle = cache.getOrDefault(CPU_IDLE, -1L);
                long cacheTotal = cache.getOrDefault(CPU_TOTAL, -1L);

                // first time return 0.0
                if (cacheTotal == -1L) {
                    cache.put(CPU_IDLE, lastIdleCpuTime);
                    cache.put(CPU_TOTAL, lastTotalCpuTime);
                    cache.put(CPU_COLLECT_TIME, System.currentTimeMillis());
                    return 0.0;
                }

                double totalCpuTime = lastTotalCpuTime - cacheTotal;
                double idle = lastIdleCpuTime - cacheIdle;
                if (totalCpuTime <= 0) {
                    return 0.0;
                }
                return 1 - (idle / totalCpuTime);
            } catch (Exception ignored) {
                log.warn("Failed to get cpu usage.", ignored);
            }
        }
        return 0.0;
    }

    /**
     * 计算内存使用率
     * @return
     */
    public static double getMemUsage() {
        if (isLinux()) {
            try {
                Map<String, String> memInfo = Maps.newHashMap();
                List<String> lines = IOUtils.readLines(new FileInputStream(PROCFS_MEMINFO), StandardCharsets.UTF_8);
                for (String line : lines) {
                    String key = line.split("\\s+")[0];
                    String value = line.split("\\s+")[1];
                    memInfo.put(key, value);
                }
                String total = memInfo.get("MemTotal:");
                String free = memInfo.get("MemFree:");
                String buffer = memInfo.get("Buffers:");
                String cache = memInfo.get("Cached:");
                return 1 - (Double.valueOf(free) + Double.valueOf(buffer) + Double.valueOf(cache)) / Double.valueOf(total);
            } catch (Exception ignored) {
                log.warn("Failed to get memory usage.", ignored);
            }
        } else {
            //非linux计算方式
            OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long physicalFree = osmxb.getFreePhysicalMemorySize();
            long physicalTotal = osmxb.getTotalPhysicalMemorySize();
            if(physicalTotal <= 0){
                return 0.0;
            }
            return 1 - (Double.valueOf(physicalFree) / Double.valueOf(physicalTotal)) ;
        }
        return 0.0;
    }

    /**
     * 计算可用物理内存
     * @return
     */
    public static long getFreePhysicalMem() {
        if (isLinux()) {
            try {
                List<String> lines = IOUtils.readLines(new FileInputStream(PROCFS_MEMINFO), StandardCharsets.UTF_8);
                String free = lines.get(1).split("\\s+")[1];
                return Long.valueOf(free);
            } catch (Exception ignored) {
                log.warn("Failed to get free memory.");
            }
        } else {
            //非linux计算方式，转换成KB
            OperatingSystemMXBean osmxb = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            return osmxb.getFreePhysicalMemorySize() / 1024;
        }
        return 0L;
    }

    /**
     * 计算进程当前磁盘使用率
     */
    public static double getDiskUsage() {
        if (isLinux() || isMac()) {
            //linux or mac都可以使用该命令
            try {
                String output = exec("df", "-h", "./");
                if (output != null) {
                    String[] lines = output.split("[\\r\\n]+");
                    if (lines.length >= 2) {
                        String[] parts = lines[1].split("\\s+");
                        if (parts.length >= 5) {
                            String pct = parts[4];
                            if (pct.endsWith("%")) {
                                return Integer.valueOf(pct.substring(0, pct.length() - 1)) / 100.0;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to get disk usage.");
            }
        }
        return 0.0;
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    public static boolean isMac() {
        String OS = System.getProperty("os.name").toLowerCase();
        return OS.contains("mac") && OS.indexOf("os") > 0;
    }

    private static String exec(String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();
            byte[] bytes = new byte[process.getInputStream().available()];
            process.getInputStream().read(bytes);
            return new String(bytes);
        } catch (Exception e) {
            log.error("Failed to run command: " + command.toString(), e);
        }
        return "";
    }

    /**
     * jvm 堆内存占用（已使用堆内存 / 最大堆内存）
     * @return
     */
    public static double getJvmHeap2MaxUsage() {
        try {
            MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long used = heapMemoryUsage.getUsed();
            long max = heapMemoryUsage.getMax();
            return Double.valueOf(used) / Double.valueOf(max);
        } catch (Exception e) {
            log.warn("Failed to get jvm heap usage.", e);
            return 0.0;
        }
    }

    /**
     * jvm 堆内存占用已申请到的内存（已使用堆内存 / 已申请堆内存）
     * @return
     */
    public static double getJvmHeap2CommittedUsage(){
        try {
            MemoryUsage heapMemoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            long used = heapMemoryUsage.getUsed();
            long committed = heapMemoryUsage.getCommitted();
            return Double.valueOf(used) / Double.valueOf(committed);
        } catch (Exception e) {
            log.warn("Failed to get jvm heap usage.", e);
            return 0.0;
        }
    }

    /**
     * jvm内存空间，单位为M
     * @return
     */
    public static long getJvmMem() {
        try {
            return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getCommitted() / (1024 * 1024);
        } catch (Exception e) {
            log.warn("Failed to get jvm mem.", e);
            return 0;
        }
    }

    /**
     *
     * @return
     */
    public static long getJvmMaxMem(){
        try {
            return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() / (1024 * 1024);
        } catch (Exception e) {
            log.warn("Failed to get jvm mem.", e);
            return 0;
        }
    }

}
