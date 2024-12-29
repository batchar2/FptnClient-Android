package com.filantrop.pvnclient.enums;

import lombok.Getter;

@Getter
public enum HandlerMessageTypes {
    // цифры от балды
    CONNECTION_STATE(586),
    SPEED_DOWNLOAD(587),
    SPEED_UPLOAD(588),
    ERROR(9881),
    UNKNOWN(6000);

    final int value;

    HandlerMessageTypes(int value) {
        this.value = value;
    }
}
