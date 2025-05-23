#pragma once

#include <jni.h>

#include "fptn-protocol-lib/websocket/websocket_client.h"


namespace fptn::wrapper {

    class WrapperWebsocketClient final
    {
    public:
        explicit WrapperWebsocketClient(JNIEnv *env,
            jobject wrapper,
            std::string server_ip,
            int server_port,
            std::string tun_ipv4,
            std::string sni,
            std::string access_token,
            std::string expected_md5_fingerprint
        );
        bool Start();
        bool Stop();
        bool IsStarted();
    protected:
        void onIPPacket(fptn::common::network::IPPacketPtr);
    private:
        mutable std::mutex mutex_;

        const JNIEnv *env_;
        const jobject wrapper_;

        const std::string server_ip_;
        const int server_port_;
        const std::string tun_ipv4_;
        const std::string sni_;
        const std::string access_token_;
        const std::string expected_md5_fingerprint_;

        fptn::protocol::websocket::WebsocketClientSPtr client_;
    };

}
