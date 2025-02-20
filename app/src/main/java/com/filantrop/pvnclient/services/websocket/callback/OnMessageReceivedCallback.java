package com.filantrop.pvnclient.services.websocket.callback;

public interface OnMessageReceivedCallback {
    void onMessageReceived(byte[] data);
}