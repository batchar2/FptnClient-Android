package org.fptn.vpn.viewmodel.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class FptnToken {
    Integer version;
    @JsonProperty("service_name")
    String serviceName;
    String username;
    String password;

    List<FptnTokenServer> servers;
    @JsonProperty("censored_zone_servers")
    List<FptnTokenServer> censoredServers;
}
