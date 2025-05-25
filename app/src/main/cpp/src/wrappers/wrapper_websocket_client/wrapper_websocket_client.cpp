/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include "wrapper_websocket_client.h"

#include <jni.h>

#ifndef FPTN_CLIENT_DEFAULT_ADDRESS_IP6
#define FPTN_CLIENT_DEFAULT_ADDRESS_IP6 "fd00::1"
#endif

using fptn::protocol::websocket::WebsocketClient;
using fptn::wrapper::WrapperWebsocketClient;

WrapperWebsocketClient::WrapperWebsocketClient(JNIEnv* env,
    jobject wrapper,
    std::string server_ip,
    int server_port,
    std::string tun_ipv4,
    std::string sni,
    std::string access_token,
    std::string expected_md5_fingerprint,
    CallbackConnectionOpened on_connection_opened,
    CallbacRecvIpPacket on_recv_ip_packet,
    CallbackConnectionClosed on_connection_closed)
    : running_(false),
      env_(env),
      wrapper_(std::move(wrapper)),
      server_ip_(std::move(server_ip)),
      server_port_(server_port),
      tun_ipv4_(std::move(tun_ipv4)),
      sni_(std::move(sni)),
      access_token_(std::move(access_token)),
      expected_md5_fingerprint_(std::move(expected_md5_fingerprint)),
      on_connection_opened_(std::move(on_connection_opened)),
      on_recv_ip_packet_(std::move(on_recv_ip_packet)),
      on_connection_closed_(std::move(on_connection_closed)) {
  (void)wrapper_;
}

WrapperWebsocketClient::~WrapperWebsocketClient() { Stop(); }

bool WrapperWebsocketClient::Start() {
  const std::unique_lock<std::mutex> lock(mutex_);  // mutex

  running_ = true;
  th_ = std::thread(&WrapperWebsocketClient::Run, this);
  return th_.joinable();

  return true;
}

bool WrapperWebsocketClient::Stop() {
  const std::unique_lock<std::mutex> lock(mutex_);  // mutex

  if (client_) {
    running_ = false;
    client_->Stop();
    if (th_.joinable()) {
      th_.join();
    }
    client_.reset();
  }
  return true;
}

bool WrapperWebsocketClient::IsStarted() {
  const std::unique_lock<std::mutex> lock(mutex_);  // mutex

  return client_ && client_->IsStarted() && running_;
}

void WrapperWebsocketClient::Run() {
  client_ = std::make_shared<WebsocketClient>(server_ip_, server_port_,
      pcpp::IPv4Address(tun_ipv4_),
      pcpp::IPv6Address(FPTN_CLIENT_DEFAULT_ADDRESS_IP6),
      std::bind(
          &WrapperWebsocketClient::onIPPacket, this, std::placeholders::_1),
      sni_, access_token_, expected_md5_fingerprint_);

  running_ = false;
  if (on_connection_closed_) {
    on_connection_closed_();
  }
}

void WrapperWebsocketClient::onIPPacket(
    fptn::common::network::IPPacketPtr packet) {
  if (packet && running_ && on_recv_ip_packet_) {
    std::string serialized_packet = packet->ToString();
    on_recv_ip_packet_(std::move(serialized_packet));
  }
}

void WrapperWebsocketClient::onConnectedCallback() {
  if (running_ && on_connection_opened_) {
    on_connection_opened_();
  }
}

bool WrapperWebsocketClient::Send(std::string pkt) {
  auto ip_packet = fptn::common::network::IPPacket::Parse(std::move(pkt));
  if (ip_packet) {
    const std::unique_lock<std::mutex> lock(mutex_);  // mutex

    if (client_ && client_->IsStarted()) {
      client_->Send(std::move(ip_packet));
      return true;
    }
  }
  return false;
}
