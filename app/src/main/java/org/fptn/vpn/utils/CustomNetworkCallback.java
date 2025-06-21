package org.fptn.vpn.utils;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

public class CustomNetworkCallback extends ConnectivityManager.NetworkCallback {

    private final ConnectivityManager connectivityManager;

    public CustomNetworkCallback(ConnectivityManager connectivityManager) {
        this.connectivityManager = connectivityManager;
    }

    @Override
    public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities);


    }
}
