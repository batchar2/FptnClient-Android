package com.filantrop.pvnclient.websocket;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.filantrop.pvnclient.CustomVpnConnection;

import org.fptn.protocol.Protocol;

import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class CustomWebSocketListener extends WebSocketListener {


    private final FileOutputStream outputStream;

    public CustomWebSocketListener(FileOutputStream outputStream) {
        super();
        this.outputStream = outputStream;
    }

    @Override
    public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Log.d(getTag(), "=== WebSocketListener.onClosing ===");
        super.onClosed(webSocket, code, reason);
    }

    @Override
    public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
        Log.d(getTag(), "=== WebSocketListener.onClosing ===");
        super.onClosing(webSocket, code, reason);
    }

    @Override
    public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
        Log.d(getTag(), "=== WebSocketListener.onFailure ===");
        super.onFailure(webSocket, t, response);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
        Log.d(getTag(), "=== WebSocketListener.onMessage(text) ===");
        super.onMessage(webSocket, text);
    }

    @Override
    public void onMessage(@NonNull WebSocket webSocket, @NonNull ByteString bytes) {
        // Вообще не вызывается
        //Log.i(getTag(), "Thread: " + Thread.currentThread().getId());
        try {
            Protocol.Message message = Protocol.Message.parseFrom(bytes.toByteArray());
            if (message.getMsgType() == Protocol.MessageType.MSG_IP_PACKET) {
//                Log.d(getTag(), "========================================================================================================");
//                Log.d(getTag(), "=== WebSocketListener.recv(bytes) ===" + message.toString());
                byte[] rawData = message.getPacket().getPayload().toByteArray();
//                Log.i(getTag(), "+Read get packet TunInterface: " + rawData.length);
                outputStream.write(rawData);
            } else {
                Log.i(getTag(), "Received a non-IP packet message type.");
            }
//            outputStream.write(bytes.toByteArray());
        } catch (IOException e) {
            Log.e(getTag(), "onMessage.error: " + e.getMessage());
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
        // Вызывается в другом потоке
        Log.i(getTag(), "=== OnOpen Thread: ===" + Thread.currentThread().getId());
        super.onOpen(webSocket, response);
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
