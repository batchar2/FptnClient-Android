package com.filantrop.pvnclient.services.websocket;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.fptn.protocol.Protocol;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class CustomWebSocketListener extends WebSocketListener {

    private final WebSocketMessageCallback messageCallback;

    public CustomWebSocketListener(WebSocketMessageCallback messageCallback) {
        super();
        this.messageCallback = messageCallback;
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Log.d(getTag(), "=== WebSocketListener.onClosing ===");
        super.onClosed(webSocket, code, reason);
        //messageCallback.onConnectionClose();
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Log.d(getTag(), "=== WebSocketListener.onClosing ===");
        //super.onClosing(webSocket, code, reason);
        messageCallback.onConnectionClose();
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        Log.d(getTag(), "=== WebSocketListener.onFailure ===");
        super.onFailure(webSocket, t, response);
        messageCallback.onConnectionClose();
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Log.d(getTag(), "=== WebSocketListener.onMessage(text) ===");
        super.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        try {
            Protocol.Message message = Protocol.Message.parseFrom(bytes.toByteArray());
            if (message.getMsgType() == Protocol.MessageType.MSG_IP_PACKET) {
                byte[] rawData = message.getPacket().getPayload().toByteArray();
                messageCallback.onMessageReceived(rawData);
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
        super.onOpen(webSocket, response);
        messageCallback.onOpen();
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
