package org.fptn.vpn.enums;

import android.net.IpPrefix;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.net.InetAddress;
import java.net.UnknownHostException;

import lombok.Getter;

@Getter
public enum ConnectionSubnets {
    TUN_ADDRESS("10.10.0.1", 32),
    TUN_INTERFACE_SUBNET("10.10.0.0", 16),
    FPTN_SUBNET("172.16.0.0", 12),
    LOCAL_SUBNET("192.168.0.0", 16),
    ALL_SUBNET("0.0.0.0", 0),

    // todo: rename me!
    HZ_WHAT_IS_THIS_IP("172.20.0.1", 32);

    private final String ipAddress;
    private final int prefix;

    ConnectionSubnets(String ipAddress, int prefix) {
        this.ipAddress = ipAddress;
        this.prefix = prefix;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public IpPrefix getAsIpPrefix() throws UnknownHostException {
        return new IpPrefix(InetAddress.getByName(ipAddress), prefix);
    }

    public String getAsIPWithPrefix() {
        return ipAddress + "/" + prefix;
    }
}
