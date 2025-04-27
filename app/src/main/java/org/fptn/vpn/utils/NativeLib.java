package org.fptn.vpn.utils;

public class NativeLib {
    static {
        System.loadLibrary("native-lib");
    }

    public native String stringFromJNI();

    public native String multipleStr(String inputString, int times, boolean withComma);
}
