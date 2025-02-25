package org.fptn.vpn.enums;

import android.util.Pair;

import java.time.Instant;
import java.util.Set;

public enum ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING;

    private final static Set<ConnectionState> ACTIVE_STATES = Set.of(
            CONNECTING,
            CONNECTED,
            RECONNECTING
    );

    public boolean isActiveState() {
        return ACTIVE_STATES.contains(this);
    }

    public Pair<ConnectionState, Instant> getWithTime() {
        return Pair.create(this, Instant.now());
    }

    public Pair<ConnectionState, Instant> getWithTime(Instant instant) {
        return Pair.create(this, instant);
    }

}
