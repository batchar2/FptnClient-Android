package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;

public class NativeHttpsClientImpl {
    private static final String TAG = NativeHttpsClientImpl.class.getName();

    private long nativeHandle = 0L;

    static {
        System.loadLibrary("https_client");
    }

    public NativeHttpsClientImpl(FptnServerDto fptnServerDto,
                                 String server_ip,
                                 int server_port,
                                 String sni,
                                 String md5_fingerprint) {
        this.nativeHandle = nativeCreate(
                server_ip,
                server_port,
                sni,
                md5_fingerprint
        );
    }


    // public Get(){}

    // public Post(){}

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String sni,
                                     String expected_md5_fingerprint);

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "NativeWebsocketWrapper.finalize()");
        nativeDestroy(nativeHandle);
    }


    private native void nativeDestroy(long nativeHandle);

    private native NativeResponse nativeGet(long nativeHandle, String url, int timeout);

    private native NativeResponse nativePost(long nativeHandle, String url, String requestBody, int timeout);

}
