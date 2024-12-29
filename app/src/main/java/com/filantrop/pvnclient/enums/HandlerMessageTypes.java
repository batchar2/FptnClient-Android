package com.filantrop.pvnclient.enums;

import lombok.Getter;

@Getter
public enum HandlerMessageTypes {
    CONNECTION_STATE(586),
    SPEED_DOWNLOAD(587),
    SPEED_UPLOAD(588),
    UNKNOWN(6000);

    final int value;

    HandlerMessageTypes(int value) {
        this.value = value;
    }
}
