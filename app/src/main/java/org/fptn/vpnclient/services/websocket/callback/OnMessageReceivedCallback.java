package org.fptn.vpnclient.services.websocket.callback;

public interface OnMessageReceivedCallback {
    void onMessageReceived(byte[] data);
}