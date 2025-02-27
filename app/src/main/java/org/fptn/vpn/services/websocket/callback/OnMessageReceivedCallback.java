package org.fptn.vpn.services.websocket.callback;

public interface OnMessageReceivedCallback {
    void onMessageReceived(byte[] data);
}