#include "wrapper_websocket_client.h"


#pragma once

#include <jni.h>

#ifndef FPTN_CLIENT_DEFAULT_ADDRESS_IP6
#define FPTN_CLIENT_DEFAULT_ADDRESS_IP6 "fd00::1"
#endif


using fptn::wrapper::WrapperWebsocketClient;
using fptn::protocol::websocket::WebsocketClient;



WrapperWebsocketClient::WrapperWebsocketClient(JNIEnv *env,
    jobject wrapper,
    std::string server_ip,
    int server_port,
    std::string tun_ipv4,
    std::string sni,
    std::string access_token,
    std::string expected_md5_fingerprint
)
    : env_(env),
    wrapper_(std::move(wrapper)),
    server_ip_(std::move(server_ip)),
    server_port_(server_port),
    tun_ipv4_(std::move(tun_ipv4)),
    sni_(std::move(sni)),
    access_token_(std::move(access_token)),
    expected_md5_fingerprint_(std::move(expected_md5_fingerprint))
{
    (void)wrapper_;
}

bool WrapperWebsocketClient::Start()
{
    client_ = std::make_shared<WebsocketClient>(
            server_ip_,
            server_port_,
            pcpp::IPv4Address(tun_ipv4_),
            pcpp::IPv6Address(FPTN_CLIENT_DEFAULT_ADDRESS_IP6),
            nullptr,
            sni_,
            access_token_,
            expected_md5_fingerprint_
    );
    return true;
}

bool WrapperWebsocketClient::Stop()
{
    const std::unique_lock<std::mutex> lock(mutex_);  // mutex

    return client_ && client_->IsStarted();
}

bool WrapperWebsocketClient::IsStarted()
{
    const std::unique_lock<std::mutex> lock(mutex_);  // mutex

    return client_ && client_->IsStarted();
}

void WrapperWebsocketClient::onIPPacket(fptn::common::network::IPPacketPtr packet)
{
    (void)packet;
}

