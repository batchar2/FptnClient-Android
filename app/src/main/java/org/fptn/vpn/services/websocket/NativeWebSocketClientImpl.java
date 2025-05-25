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

    private long nativeHandle = 0L;

    static {
        System.loadLibrary("websocket_client");
    }

    public NativeWebSocketClientImpl(FptnServerDto fptnServerDto, String sniHostName) {
        this.fptnServerDto = fptnServerDto;
        this.sniHostName = sniHostName;
        // FIX
        this.nativeHandle = nativeCreate(
                fptnServerDto.host,
                fptnServerDto.port,
                "10.10.0.1",
                sniHostName,
                "TOKEN",
                "expected_md5_fingerprint"
        );
    }

    @Override
    public String getDnsServerIPv4() throws PVNClientException {
        return "";
    }

    @Override
    public void startWebSocket(OnOpenCallback onOpenCallback, OnMessageReceivedCallback onMessageReceivedCallback, OnFailureCallback onFailureCallback) throws PVNClientException, WebSocketAlreadyShutdownException {
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
