package org.fptn.vpnclient.utils;

import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Optional;

public class NetworkMonitor {
    private static final String TAG = NetworkMonitor.class.getSimpleName();

    private final ConnectivityManager connectivityManager;

    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkMonitor(Context context) {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return Optional.ofNullable(connectivityManager.getActiveNetwork())
                .map(connectivityManager::getNetworkCapabilities)
                .map(networkCapabilities -> networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
                .orElse(false);
    }

    //todo: use it later - add reconnect on changing network
    // from TRANSPORT_CELLULAR (limited) to TRANSPORT_WIFI (unlimited)
    public void startMonitoring() {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        this.networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                Log.d(TAG, "onAvailable(): " + network);
                Log.d(TAG, "getNetworkHandle(): " + network.getNetworkHandle());
                super.onAvailable(network);
            }

            @Override
            public void onLosing(@NonNull Network network, int maxMsToLive) {
                Log.d(TAG, "onLosing(): " + network);
                Log.d(TAG, "getNetworkHandle(): " + network.getNetworkHandle());
                super.onLosing(network, maxMsToLive);
            }

            @Override
            public void onLost(@NonNull Network network) {
                Log.d(TAG, "onLost(): " + network);
                Log.d(TAG, "getNetworkHandle(): " + network.getNetworkHandle());
                super.onLost(network);
            }

            @Override
            public void onUnavailable() {
                Log.d(TAG, "onUnavailable()");
                super.onUnavailable();
            }

            @Override
            public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                // Network capabilities have changed
                Log.d(TAG, "Wifi connected: " + networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                Log.d(TAG, "Mobile connected: " + networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
                Log.d(TAG, "Network have internet: " + networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET));
            }

            @Override
            public void onLinkPropertiesChanged(@NonNull Network network, @NonNull LinkProperties linkProperties) {
                super.onLinkPropertiesChanged(network, linkProperties);
            }

            @Override
            public void onBlockedStatusChanged(@NonNull Network network, boolean blocked) {
                super.onBlockedStatusChanged(network, blocked);
            }
        };

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    public void stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}
