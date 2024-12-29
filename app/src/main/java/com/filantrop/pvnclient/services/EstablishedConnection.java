package com.filantrop.pvnclient.services;

import android.os.ParcelFileDescriptor;
import android.util.Pair;

class EstablishedConnection extends Pair<CustomVpnConnection, ParcelFileDescriptor> {
    public EstablishedConnection(CustomVpnConnection thread, ParcelFileDescriptor pfd) {
        super(thread, pfd);
    }
}
