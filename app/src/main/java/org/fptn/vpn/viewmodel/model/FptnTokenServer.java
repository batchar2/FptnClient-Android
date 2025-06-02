package org.fptn.vpn.viewmodel.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class FptnTokenServer {
    String name;
    String host;
    @JsonProperty("md5_fingerprint")
    String md5Fingerprint;
    @JsonProperty("country_code")
    String countryCode;
    Integer port;
}
