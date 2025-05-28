/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include <jni.h>

#include "wrappers/utils/utils.h"
#include "wrappers/wrapper_https_client/wrapper_https_client.h"

namespace {
jobject create_response(JNIEnv* env,
    const std::string& response_body,
    int code,
    const std::string& error_message) {

  fptn::wrapper::init_logger(); // will call only once

  jclass clazz =
      env->FindClass("org/fptn/vpn/services/websocket/NativeResponse");
  jmethodID constructor = env->GetMethodID(
      clazz, "<init>", "(ILjava/lang/String;Ljava/lang/String;)V");
  jstring body_str = env->NewStringUTF(response_body.c_str());
  jstring error_str = env->NewStringUTF(error_message.c_str());

  jobject response_obj =
      env->NewObject(clazz, constructor, code, body_str, error_str);
  return response_obj;
}
}  // namespace

using fptn::wrapper::WrapperHttpsClient;

/**
 * @brief Creates a new native HTTPS client instance
 *
 * @param[in] env JNI environment pointer
 * @param[in] thiz Java object reference
 * @param[in] host_param Server hostname as Java string
 * @param[in] port_param Server port number
 * @param[in] sni_param SNI hostname as Java string
 * @param[in] md5_fingerprint_param Expected server certificate fingerprint as
 * Java string
 *
 * @return jlong Native pointer to the created WrapperHttpsClient instance
 *
 * @warning The returned handle must be destroyed with nativeDestroy()
 *          to avoid memory leaks
 */
extern "C" JNIEXPORT jlong JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativeCreate(
    JNIEnv* env,
    jobject thiz,
    jstring host_param,
    jint port_param,
    jstring sni_param,
    jstring md5_fingerprint_param) {
  jobject global_object_ref = env->NewWeakGlobalRef(thiz);

  auto host = fptn::wrapper::ConvertToCString(env, host_param);
  int port = port_param;
  auto sni = fptn::wrapper::ConvertToCString(env, sni_param);
  auto md5_fingerprint =
      fptn::wrapper::ConvertToCString(env, md5_fingerprint_param);

  auto* https_client = new WrapperHttpsClient(env, global_object_ref,
      std::move(host), port, std::move(sni), std::move(md5_fingerprint));
  return reinterpret_cast<jlong>(https_client);
}

// Destroy
extern "C" JNIEXPORT void JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong native_handle) {
  (void)env;
  (void)thiz;

  auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
  if (https_client) {
    delete https_client;
  }
}

/**
 * @brief Executes an HTTP GET request and returns response data
 *
 * @param[in] env JNI environment pointer
 * @param[in] thiz Java object reference (unused)
 * @param[in] native_handle Pointer to native WrapperHttpsClient instance
 * @param[in] http_handle_param Request URL as Java string
 * @param[in] timeout_param Request timeout in milliseconds
 *
 * @return jobjectArray Java Object array containing:
 *         - [0]: String response body (JSON format)
 *         - [1]: Integer HTTP status code
 *         - [2]: String error message (empty if successful)
 *
 *
 * @note Default return values when client is invalid:
 *       - Body: "{}" (empty JSON)
 *       - Code: 400 (Bad Request)
 *       - Error: "Object is empty"
 */
extern "C" JNIEXPORT jobject JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativeGet(
    JNIEnv* env,
    jobject thiz,
    jlong native_handle,
    jstring http_handle_param,
    jint timeout_param) {
  (void)thiz;

  const auto http_handle =
      fptn::wrapper::ConvertToCString(env, http_handle_param);
  int timeout = timeout_param;

  auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
  if (https_client) {
    const auto resp = https_client->Get(http_handle, timeout);
    return create_response(env, resp.body, resp.code, resp.errmsg);
  }
  return create_response(env, "{}", 400, "Object is empty");
}

/**
 * @brief Makes an HTTP POST request and returns the response as a Java Object
 * array.
 *
 * @param[in] env The JNI environment pointer
 * @param[in] thiz The Java object this native method is associated with
 * @param[in] native_handle Pointer to the native WrapperHttpsClient instance
 * @param[in] http_handle_param URL endpoint for the POST request (as Java
 * String)
 * @param[in] http_request_param HTTP request body (as Java String)
 * @param[in] timeout_param Request timeout in milliseconds
 *
 * @return jobjectArray Java Object array containing:
 *         - [0]: String response body (JSON format)
 *         - [1]: Integer HTTP status code
 *         - [2]: String error message (empty if successful)
 *
 *
 * @note Default return values when client is invalid:
 *       - Body: "{}" (empty JSON)
 *       - Code: 400 (Bad Request)
 *       - Error: "Object is empty"
 */
extern "C" JNIEXPORT jobject JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativePost(
    JNIEnv* env,
    jobject thiz,
    jlong native_handle,
    jstring http_handle_param,
    jstring http_request_param,
    jint timeout_param) {
  (void)thiz;

  const auto http_handle =
      fptn::wrapper::ConvertToCString(env, http_handle_param);
  const auto http_request =
      fptn::wrapper::ConvertToCString(env, http_request_param);
  int timeout = timeout_param;

  auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
  if (https_client) {
    const auto resp = https_client->Post(http_handle, http_request, timeout);
    return create_response(env, resp.body, resp.code, resp.errmsg);
  }
  return create_response(env, "{}", 400, "Object is empty");
}
