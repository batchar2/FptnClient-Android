package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

public class NativeWebSocketClientImpl {
    static {
        System.loadLibrary("fptn_native_lib");
    }

    private final OnOpenCallback onOpenCallback;
    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;

    private final long nativeHandle;

    public NativeWebSocketClientImpl(
            String host,
            int port,
            String tunAddress,
            String sniHostName,
            String accessToken,
            String md5ServerFingerprint,
            OnOpenCallback onOpenCallback,
            OnMessageReceivedCallback onMessageReceivedCallback,
            OnFailureCallback onFailureCallback) throws PVNClientException {
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;

        this.nativeHandle = nativeCreate(
                host,
                port,
                tunAddress,
                sniHostName,
                accessToken,
                md5ServerFingerprint
        );

        if (this.nativeHandle == 0L) {
            throw new PVNClientException(ErrorCode.CONNECT_TO_SERVER_ERROR);
        }
    }

    public void start() {
        if (!nativeIsStarted(nativeHandle)) {
            nativeRun(nativeHandle);
        }
    }

    public void stop() {
        if (nativeIsStarted(nativeHandle)) {
            nativeStop(nativeHandle);
        }
    }

    public boolean isStarted() {
        return nativeIsStarted(nativeHandle);
    }

    public void send(byte[] data) {
        if (nativeHandle != 0L && nativeIsStarted(nativeHandle)) {
            nativeSend(nativeHandle, data);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        Log.d(getTag(), "NativeWebSocketClientImpl.finalize()");
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle);
        }
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    public void onOpenImpl() {
        Log.d(getTag(), "NativeWebSocketClientImpl.onOpenImpl():start()");
        if (this.onOpenCallback != null) {
            this.onOpenCallback.onOpen();
        }
        Log.d(getTag(), "NativeWebSocketClientImpl.onOpenImpl():end()");
    }

    public void onFailureImpl() {
        Log.d(getTag(), "NativeWebSocketClientImpl.onFailureImpl():start()");
        if (this.onFailureCallback != null) {
            this.onFailureCallback.onFailure();
        }
        Log.d(getTag(), "NativeWebSocketClientImpl.onFailureImpl():end()");
    }

    public void onMessageImpl(byte[] msg) {
        if (this.onMessageReceivedCallback != null) {
            this.onMessageReceivedCallback.onMessageReceived(msg);
        }
    }

    private native long nativeCreate(String server_ip,
                                     int server_port,
                                     String tun_ipv4,
                                     String sni,
                                     String access_token,
                                     String expected_md5_fingerprint);

    private native void nativeDestroy(long nativeHandle);

    private native boolean nativeRun(long nativeHandle);

    private native boolean nativeStop(long nativeHandle);

    private native boolean nativeSend(long nativeHandle, byte[] data);

    private native boolean nativeIsStarted(long nativeHandle);
}
