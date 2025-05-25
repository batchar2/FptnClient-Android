package org.fptn.vpn.services.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NativeResponse {
    private int code;
    private String body;
    private String errorMessage;
}
