#pragma once

#include <jni.h>

#include "fptn-protocol-lib/https/https_client.h"


namespace fptn::wrapper {
    using fptn::protocol::https::Response;
    using fptn::protocol::https::HttpsClient;

    class WrapperHttpsClient
    {
    public:
        WrapperHttpsClient(JNIEnv *env,
            jobject wrapper,
            std::string host,
            int port,
            std::string sni,
            std::string md5_fingerprint
        );

        Response Get(const std::string& handle, int timeout = 5);
        Response Post(const std::string& handle,
                      const std::string& request,
                      int timeout = 5);
    private:
        const JNIEnv *env_;
        const jobject wrapper_;
        HttpsClient https_client_;
    };
}
