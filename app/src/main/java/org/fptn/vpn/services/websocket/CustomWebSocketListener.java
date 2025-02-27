package org.fptn.vpn.services.websocket;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;

import org.fptn.protocol.Protocol;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class CustomWebSocketListener extends WebSocketListener {

    private final OnMessageReceivedCallback onMessageReceivedCallback;
    private final OnFailureCallback onFailureCallback;
    private final OnOpenCallback onOpenCallback;

    public CustomWebSocketListener(OnMessageReceivedCallback onMessageReceivedCallback,
                                   OnFailureCallback onFailureCallback,
                                   OnOpenCallback onOpenCallback) {
        this.onMessageReceivedCallback = onMessageReceivedCallback;
        this.onFailureCallback = onFailureCallback;
        this.onOpenCallback = onOpenCallback;
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Log.d(getTag(), "=== WebSocketListener.onClosing ===");
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Log.d(getTag(), "=== WebSocketListener.onClosing ===");
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        Log.d(getTag(), "=== WebSocketListener.onFailure ===");
        onFailureCallback.onFailure();
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Log.d(getTag(), "=== WebSocketListener.onMessage(text) ===");
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        try {
            Protocol.Message message = Protocol.Message.parseFrom(bytes.toByteArray());
            if (message.getMsgType() == Protocol.MessageType.MSG_IP_PACKET) {
                byte[] rawData = message.getPacket().getPayload().toByteArray();
                onMessageReceivedCallback.onMessageReceived(rawData);
            } else {
                Log.i(getTag(), "Received a non-IP packet message type.");
            }
        } catch (IOException e) {
            Log.e(getTag(), "onMessage.error: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        Log.i(getTag(), "=== OnOpen Thread: ===" + Thread.currentThread().getId());
        onOpenCallback.onOpen();
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
