package com.filantrop.pvnclient.services;

import android.os.ParcelFileDescriptor;
import android.util.Pair;

class EstablishedConnection extends Pair<Thread, ParcelFileDescriptor> {
    public EstablishedConnection(Thread thread, ParcelFileDescriptor pfd) {
        super(thread, pfd);
    }
}
