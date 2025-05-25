#pragma once

#include <jni.h>

#include "fptn-protocol-lib/websocket/websocket_client.h"


namespace fptn::wrapper {

    using CallbackConnectionOpened = std::function<void()>;
    using CallbacRecvIpPacket = std::function<void(std::string pkt)>;
    using CallbackConnectionClosed = std::function<void()>;

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
            std::string expected_md5_fingerprint,
            CallbackConnectionOpened on_connection_opened,
            CallbacRecvIpPacket on_recv_ip_packet,
            CallbackConnectionClosed on_connection_closed
        );
        ~WrapperWebsocketClient();

        bool Start();
        bool Stop();
        bool IsStarted();
        bool Send(std::string pkt);
    protected:
        void Run();
        void onIPPacket(fptn::common::network::IPPacketPtr);
        void onConnectedCallback();
    private:
        std::thread th_;
        mutable std::mutex mutex_;
        mutable std::atomic<bool> running_;

        const JNIEnv *env_;
        const jobject wrapper_;

        const std::string server_ip_;
        const int server_port_;
        const std::string tun_ipv4_;
        const std::string sni_;
        const std::string access_token_;
        const std::string expected_md5_fingerprint_;

        const CallbackConnectionOpened on_connection_opened_;
        const CallbacRecvIpPacket on_recv_ip_packet_;
        const CallbackConnectionClosed on_connection_closed_;

        fptn::protocol::websocket::WebsocketClientSPtr client_;
    };
}
