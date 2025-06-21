package org.fptn.vpn.services;

import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class CustomVpnServiceState {
    private final ConnectionState connectionState;
    private final PVNClientException exception;

    public static final CustomVpnServiceState INITIAL = CustomVpnServiceState.builder()
            .connectionState(ConnectionState.DISCONNECTED)
            .exception(null)
            .build();

    public static final CustomVpnServiceState FAKE_CONNECTING = CustomVpnServiceState.builder()
            .connectionState(ConnectionState.CONNECTING)
            .exception(null)
            .build();
}
