package org.fptn.vpn.vpnclient.exception

class PVNClientException(
    @JvmField val errorCode: ErrorCode,
    @JvmField val errorMessage: String,
) : Exception(errorCode.value) {
    constructor(errorCode: ErrorCode) : this(errorCode, errorCode.value)
    constructor(errorMessage: String) : this(ErrorCode.UNKNOWN_ERROR, errorMessage)
}
