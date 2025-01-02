package com.filantrop.pvnclient.utils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

// https://github.com/altEr1125/ShiroAttack2/blob/5bd4ec1b1cf749ff684792cdcbe0976755e297ca/src/main/java/com/summersec/attack/utils/HttpUtil_bak.java#L348
public class MySSLSocketFactory extends SSLSocketFactory {
    private SSLSocketFactory sf;
    private String[] enabledCiphers;

    public MySSLSocketFactory(SSLSocketFactory sf, String[] enabledCiphers) {
        this.sf = null;
        this.enabledCiphers = null;
        this.sf = sf;
        this.enabledCiphers = enabledCiphers;
    }

    private Socket getSocketWithEnabledCiphers(Socket socket) {
        if (this.enabledCiphers != null && socket != null && socket instanceof SSLSocket) {
            ((SSLSocket)socket).setEnabledCipherSuites(this.enabledCiphers);
        }

        return socket;
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return this.getSocketWithEnabledCiphers(this.sf.createSocket(s, host, port, autoClose));
    }

    public String[] getDefaultCipherSuites() {
        return this.sf.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return this.enabledCiphers == null ? this.sf.getSupportedCipherSuites() : this.enabledCiphers;
    }

    public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
        return this.getSocketWithEnabledCiphers(this.sf.createSocket(host, port));
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return this.getSocketWithEnabledCiphers(this.sf.createSocket(address, port));
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException, UnknownHostException {
        return this.getSocketWithEnabledCiphers(this.sf.createSocket(host, port, localAddress, localPort));
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localaddress, int localport) throws IOException {
        return this.getSocketWithEnabledCiphers(this.sf.createSocket(address, port, localaddress, localport));
    }

    MySSLSocketFactory(SSLSocketFactory x0, String[] x1, Object x2) {
        this(x0, x1);
    }
}