package org.fptn.vpn.utils;

import org.fptn.vpn.core.common.Constants;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

// https://github.com/altEr1125/ShiroAttack2/blob/5bd4ec1b1cf749ff684792cdcbe0976755e297ca/src/main/java/com/summersec/attack/utils/HttpUtil_bak.java#L348
public class MySSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory socketFactory;
    private final String[] enabledCiphers;

    public MySSLSocketFactory(SSLSocketFactory socketFactory, String[] enabledCiphers) {
        this.socketFactory = socketFactory;
        this.enabledCiphers = enabledCiphers;
    }

    private Socket getSocketWithEnabledCiphers(Socket socket) {
        if (this.enabledCiphers != null && socket instanceof SSLSocket) {
            ((SSLSocket) socket).setEnabledCipherSuites(this.enabledCiphers);

            SSLParameters sslParameters = new SSLParameters();
            sslParameters.setServerNames(Collections.singletonList(new SNIHostName(Constants.DEFAULT_SNI)));
            ((SSLSocket) socket).setSSLParameters(sslParameters);
        }

        return socket;
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return this.getSocketWithEnabledCiphers(this.socketFactory.createSocket(s, host, port, autoClose));
    }

    public String[] getDefaultCipherSuites() {
        return this.socketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return this.enabledCiphers == null ? this.socketFactory.getSupportedCipherSuites() : this.enabledCiphers;
    }

    public Socket createSocket(String host, int port) throws IOException {
        return this.getSocketWithEnabledCiphers(this.socketFactory.createSocket(host, port));
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        return this.getSocketWithEnabledCiphers(this.socketFactory.createSocket(address, port));
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.getSocketWithEnabledCiphers(this.socketFactory.createSocket(host, port, localAddress, localPort));
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return this.getSocketWithEnabledCiphers(this.socketFactory.createSocket(address, port, localAddress, localPort));
    }

}