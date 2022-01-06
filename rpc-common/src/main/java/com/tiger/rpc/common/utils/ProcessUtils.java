package com.tiger.rpc.common.utils;

import com.google.common.io.Closeables;
import com.sun.jna.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;

/**
 * @ClassName: ProcessUtils.java
 *
 * @Description: 进程工具
 *
 * @Author: Tiger
 *
 * @Date: 2019/5/29
 */
@Slf4j
public class ProcessUtils {

    public static synchronized void exit(int code) {
        Runtime.getRuntime().exit(code);
    }

    public static synchronized void exit(int code, String msg) {
        exit(code, new RuntimeException(msg));
    }

    public static synchronized void exit(int code, Throwable t) {
        log.error("exit process. code: " + code, t);
        if (code != 0) {
            exit(code);
        }
    }

    public static synchronized void halt(int code) {
        Runtime.getRuntime().halt(code);
    }

    public static synchronized void halt(int code, String msg) {
        halt(code, new RuntimeException(msg));
    }

    public static synchronized void halt(int code, Throwable t) {
        log.error("halt process. code: " + code, t);
        if (code != 0) {
            halt(code);
        }
    }

    public static int pid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();

        String[] nameSplit = StringUtils.split(name, "@");
        return Integer.parseInt(nameSplit[0]);
    }

    /**
     * 获取进程编号
     * @param process   进程参数
     * @return
     */
    public static long getPid(Process process) {
        long pid = -1;
        if (process == null) {
            log.warn("process can not be null.");
            return pid;
        }
        if (Platform.isWindows()) {
            try {
                Field field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);
                pid = Kernel32.INSTANCE.GetProcessId((Long) field.get(process));
            } catch (Exception e) {
                log.error("Get pid of Windows platform process error", e);
            }
        } else if (Platform.isLinux() || Platform.isAIX() || Platform.isMac()) {
            try {
                Class<?> clazz = Class.forName("java.lang.UNIXProcess");
                Field field = clazz.getDeclaredField("pid");
                field.setAccessible(true);
                pid = (Integer) field.get(process);
            } catch (Throwable e) {
                log.error("Get pid of Linux platform process error", e);
            }
        }
        return pid;
    }

    /**
     * 关闭Linux进程
     * @param pid 进程的PID
     */
    public static boolean forceKillByPid(long pid) {
        if(pid <= 0){
            log.warn("pid is illegal, and response ok.");
            return true;
        }
        Process process = null;
        BufferedReader reader =null;
        String command = "";
        boolean result = false;
        if (Platform.isWindows()) {
            command = "cmd.exe /c taskkill /PID " + pid + " /F /T ";
        } else if (Platform.isLinux() || Platform.isAIX()) {
            command = "kill -9 " + pid;
        }
        try{
            //杀掉进程
            process = Runtime.getRuntime().exec(command);
            reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Constants.DEFAULT_CHARSET));
            String line;
            while((line = reader.readLine()) != null){
                log.info("kill PID return info: {}", line);
                System.out.println("kill PID return info -----> " + line);
            }
            result = true;
        }catch(Exception e){
            log.info("kill process error", e);
        }finally{
            if(process!=null){
                process.destroy();
            }
            if(reader!=null){
                try {
                    Closeables.closeQuietly(reader);
                } catch (Exception e) {
                    log.debug(" Release resources", e);
                }
            }
        }
        return result;
    }

    /**
     * 关闭Linux进程: 1.对象不存在时，true 2.获取进程失败，false 3.响应kill结果
     * @param process 进程对象
     * @return
     */
    public static boolean forceKillByPid(Process process) {
        if(process == null){
            //进程对象不存在，则返回true
            return true;
        }
        //获取进程号
        long pid = getPid(process);
        //获取进程失败，返回false
        if (pid <= 0) {
            return false;
        }
        //force kill pid
        return forceKillByPid(pid);
    }

    /**
     * 检测linux进程
     * @param pid   进程编号
     * @return
     */
    public static boolean exist(long pid){
        Process process = null;
        boolean result = false;
        String command = "kill -0 " + pid;
        try{
            //杀掉进程
            process = Runtime.getRuntime().exec(command);
            result = process.waitFor() == 0 ? true : false;
        }catch(Exception e){
            log.info("Check process error", e);
        }finally{
            if(process != null) {
                process.destroy();
            }
        }
        return result;
    }

    public static void main(String[] args) {
//        System.out.println(forceKillByPid(27864));
        System.out.println(exist(1674));
        System.out.println(exist(27864));
    }

}
