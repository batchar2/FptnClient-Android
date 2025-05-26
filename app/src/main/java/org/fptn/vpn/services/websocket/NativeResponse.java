package org.fptn.vpn.services.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NativeResponse {
    public int code;
    public String body;
    public String errorMessage;
}
