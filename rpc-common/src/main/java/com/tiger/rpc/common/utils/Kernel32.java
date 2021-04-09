package com.tiger.rpc.common.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

/**
 * @ClassName: Kernel32.java
 *
 * @Description: windows的Kernel32
 *
 * @Author: Tiger
 *
 * @Date: 2019/12/6
 */
public interface Kernel32 extends Library {

    public static Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);

    public long GetProcessId(Long hProcess);

}
