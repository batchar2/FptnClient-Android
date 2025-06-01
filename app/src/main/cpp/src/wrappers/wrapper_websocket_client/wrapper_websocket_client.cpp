/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include "wrapper_websocket_client.h"

#include <jni.h>

#include "jnienv/jnienv.h"

#ifndef FPTN_CLIENT_DEFAULT_ADDRESS_IP6
#define FPTN_CLIENT_DEFAULT_ADDRESS_IP6 "fd00::1"
#endif

using fptn::protocol::websocket::WebsocketClient;
using fptn::wrapper::WrapperWebsocketClient;

WrapperWebsocketClient::WrapperWebsocketClient(jobject wrapper,
    std::string server_ip,
    int server_port,
    std::string tun_ipv4,
    std::string sni,
    std::string access_token,
    std::string expected_md5_fingerprint)
    : running_(false),
      reconnection_attempts_(kMaxReconnectionAttempts_),
      wrapper_(std::move(wrapper)),
      server_ip_(std::move(server_ip)),
      server_port_(server_port),
      tun_ipv4_(std::move(tun_ipv4)),
      sni_(std::move(sni)),
      access_token_(std::move(access_token)),
      expected_md5_fingerprint_(std::move(expected_md5_fingerprint)) {
  (void)wrapper_;
}

WrapperWebsocketClient::~WrapperWebsocketClient() { Stop(); }

bool WrapperWebsocketClient::Start() {
  const std::unique_lock<std::mutex> lock(mutex_);  // mutex

  running_ = true;
  th_ = std::thread(&WrapperWebsocketClient::Run, this);
  return th_.joinable();

  // todo: for what second return need?
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
  // Maximum allowed reconnection attempts
  // constexpr int kMaxAttempts = 3;
  // Time window for counting attempts (1 minute)
  constexpr auto kReconnectionWindow = std::chrono::seconds(60);
  // Delay between reconnection attempts
  constexpr auto kReconnectionDelay = std::chrono::milliseconds(300);

  // Current count of reconnection attempts
  reconnection_attempts_ = kMaxReconnectionAttempts_;
  auto window_start_time = std::chrono::steady_clock::now();

  while (running_ && reconnection_attempts_) {
    {
      const std::unique_lock<std::mutex> lock(mutex_);  // mutex

      client_ = std::make_shared<WebsocketClient>(server_ip_, server_port_,
          pcpp::IPv4Address(tun_ipv4_),
          pcpp::IPv6Address(FPTN_CLIENT_DEFAULT_ADDRESS_IP6),
          std::bind(
              &WrapperWebsocketClient::onIPPacket, this, std::placeholders::_1),
          sni_, access_token_, expected_md5_fingerprint_,
          [this]() { onConnectedCallback(); });
    }
    client_->Run();

    if (!running_) {
      break;
    }

    // Calculate time since last window start
    auto current_time = std::chrono::steady_clock::now();
    auto elapsed = current_time - window_start_time;
    // Reconnection attempt counting logic
    if (elapsed >= kReconnectionWindow) {
      // Reset counter if we're past the time window
      reconnection_attempts_ = kMaxReconnectionAttempts_;
      window_start_time = current_time;
    } else {
      // Decrement counter if within time window
      reconnection_attempts_--;
    }

    // Log connection failure and wait before retrying
    SPDLOG_ERROR(
        "Connection closed (attempt {}/{} in current window). Reconnecting in "
        "{}ms...",
        kMaxReconnectionAttempts_ - reconnection_attempts_,
        kMaxReconnectionAttempts_, kReconnectionDelay.count());
    std::this_thread::sleep_for(kReconnectionDelay);
  }

  if (running_ && !reconnection_attempts_) {
    SPDLOG_ERROR("Connection failure: Could not establish connection");
    JNIEnv* env = getJniEnv();
    jclass cls_foo = env->GetObjectClass(wrapper_);
    jmethodID on_close_impl = env->GetMethodID(cls_foo, "onFailureImpl", "()V");
    env->CallVoidMethod(wrapper_, on_close_impl);
  }
  running_ = false;
}

void WrapperWebsocketClient::onIPPacket(
    fptn::common::network::IPPacketPtr packet) {
  if (packet && running_) {
    JNIEnv* env = getJniEnv();

    const std::string serialized_packet = packet->ToString();
    jbyteArray jpacket = env->NewByteArray(serialized_packet.size());
    env->SetByteArrayRegion(jpacket, 0, serialized_packet.size(),
        reinterpret_cast<const jbyte*>(serialized_packet.data()));

    jclass cls_foo = env->GetObjectClass(wrapper_);
    jmethodID on_message_impl =
        env->GetMethodID(cls_foo, "onMessageImpl", "([B)V");

    env->CallVoidMethod(wrapper_, on_message_impl, jpacket);
  }
}

void WrapperWebsocketClient::onConnectedCallback() {
  if (running_) {
    JNIEnv* env = getJniEnv();
    jclass cls_foo = env->GetObjectClass(wrapper_);
    jmethodID on_open_impl = env->GetMethodID(cls_foo, "onOpenImpl", "()V");

    env->CallVoidMethod(wrapper_, on_open_impl);
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
