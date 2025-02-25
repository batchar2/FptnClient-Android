package org.fptn.vpnclient.enums;

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
}
