package org.fptn.vpn.services.websocket;

import android.util.Log;

import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

public class NativeWebSocketClientImpl implements WebSocketClient {
    private static final String TAG = NativeWebSocketClientImpl.class.getName();
    private final FptnServerDto fptnServerDto;
    private final String sniHostName;

    private OnOpenCallback onOpenCallback;
    private OnMessageReceivedCallback onMessageReceivedCallback;
    private OnFailureCallback onFailureCallback;

    private long nativeHandle = 0L;

    static {
        System.loadLibrary("fptn_native_lib");
    }

    public NativeWebSocketClientImpl(FptnServerDto fptnServerDto, String sniHostName, String token) {
        this.fptnServerDto = fptnServerDto;
        this.sniHostName = sniHostName;
        // FIX
        this.nativeHandle = nativeCreate(
                fptnServerDto.host,
                fptnServerDto.port,
                "10.10.0.1",
                sniHostName,
                token,
                "expected_md5_fingerprint"
        );
    }

    @Override
    public String getDnsServerIPv4() throws PVNClientException {
        return "";
    }

    @Override
    public void startWebSocket(OnOpenCallback onOpenCallback, OnMessageReceivedCallback onMessageReceivedCallback, OnFailureCallback onFailureCallback) throws PVNClientException, WebSocketAlreadyShutdownException {
        this.onOpenCallback = onOpenCallback;
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;
        if (!nativeIsStarted(nativeHandle)){
            nativeRun(nativeHandle);
        }
    }

    @Override
    public void stopWebSocket() {
        if (nativeIsStarted(nativeHandle)){
            nativeStop(nativeHandle);
        }
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void send(byte[] data) {
        if (nativeHandle!=0 && nativeIsStarted(nativeHandle)){
            nativeSend(nativeHandle, data);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        Log.d(TAG, "NativeWebsocketWrapper.finalize()");
        nativeDestroy(nativeHandle);
    }

    public void onOpenImpl()
    {
        if (this.onOpenCallback != null) {
            this.onOpenCallback.onOpen();
        }
    }

    public void onCloseImpl()
    {
        if (this.onFailureCallback != null) {
            this.onFailureCallback.onFailure();
        }
    }

    public void onMessageImpl(byte[] msg)
    {
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
