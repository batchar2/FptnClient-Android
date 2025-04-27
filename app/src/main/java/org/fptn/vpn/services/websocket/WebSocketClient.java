package org.fptn.vpn.services.websocket;

import org.fptn.vpn.services.websocket.callback.OnFailureCallback;
import org.fptn.vpn.services.websocket.callback.OnMessageReceivedCallback;
import org.fptn.vpn.services.websocket.callback.OnOpenCallback;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

public interface WebSocketClient {
    String getDnsServerIPv4() throws PVNClientException;

    void startWebSocket(OnOpenCallback onOpenCallback, OnMessageReceivedCallback onMessageReceivedCallback, OnFailureCallback onFailureCallback) throws PVNClientException, WebSocketAlreadyShutdownException;

    void stopWebSocket();

    void shutdown();

    void send(byte[] data);
}
