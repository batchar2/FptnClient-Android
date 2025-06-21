/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include "wrapper_https_client.h"

#include "fptn-protocol-lib/time/time_provider.h"

using fptn::wrapper::WrapperHttpsClient;

using fptn::protocol::https::Response;

WrapperHttpsClient::WrapperHttpsClient(JNIEnv* env,
    jobject wrapper,
    std::string host,
    int port,
    std::string sni,
    std::string md5_fingerprint)
    : env_(env),
      wrapper_(std::move(wrapper)),
      https_client_(
          std::move(host), port, std::move(sni), std::move(md5_fingerprint)) {
  // Synchronize VPN client time with NTP servers
  fptn::time::TimeProvider::Instance()->NowTimestamp();
}

Response WrapperHttpsClient::Get(const std::string& handle, int timeout) {
  return https_client_.Get(handle, timeout);
}

Response WrapperHttpsClient::Post(
    const std::string& handle, const std::string& request, int timeout) {
  return https_client_.Post(handle, request, "application/json", timeout);
}
