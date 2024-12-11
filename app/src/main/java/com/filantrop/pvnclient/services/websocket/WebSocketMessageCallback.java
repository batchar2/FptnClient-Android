package com.filantrop.pvnclient.services.websocket;

public interface WebSocketMessageCallback {
    void onMessageReceived(byte[] data);
}