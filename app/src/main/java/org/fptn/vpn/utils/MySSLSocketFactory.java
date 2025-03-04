package org.fptn.vpn.utils;

import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MySSLSocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory socketFactory;
    private final String[] enabledCiphers;

    SSLParameters sslParameters;

    public MySSLSocketFactory(SSLSocketFactory socketFactory, String[] enabledCiphers, String sni) {
        this.socketFactory = socketFactory;
        this.enabledCiphers = enabledCiphers;

        this.sslParameters = new SSLParameters();
        // SNI
        List sniHostNames = new ArrayList(1);
        sniHostNames.add(new SNIHostName(sni)); // SNIServerName ?
        sslParameters.setServerNames(sniHostNames);
        // HTTPS
        sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    }

    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket) this.socketFactory.createSocket(s, host, port, autoClose);
        setParameters(socket);
        return socket;
    }

    public String[] getDefaultCipherSuites() {
        return this.socketFactory.getDefaultCipherSuites();
    }

    public String[] getSupportedCipherSuites() {
        return this.socketFactory.getSupportedCipherSuites();
    }

    public Socket createSocket(String host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) this.socketFactory.createSocket(host, port);
        setParameters(socket);
        return socket;
    }

    public Socket createSocket(InetAddress address, int port) throws IOException {
        SSLSocket socket = (SSLSocket) this.socketFactory.createSocket(address, port);
        setParameters(socket);
        return socket;
    }

    public Socket createSocket(String host, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket)this.socketFactory.createSocket(host, port, localAddress, localPort);
        setParameters(socket);
        return socket;
    }

    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) this.socketFactory.createSocket(address, port, localAddress, localPort);
        setParameters(socket);
        return socket;
    }

    private void setParameters(SSLSocket socket) {
        socket.setEnabledProtocols(new String[]{"TLSv1.3", "TLSv1.2"});

        Log.i("TAG", "CIPHERS: " + TextUtils.join(", ", this.enabledCiphers));

        socket.setEnabledCipherSuites(this.enabledCiphers);
        // https://javabreaks.blogspot.com/2015/12/java-ssl-handshake-with-server-name.html
        socket.setSSLParameters(this.sslParameters);
    }
}
