/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include <jni.h>

#include "wrappers/utils/utils.h"
#include "wrappers/wrapper_https_client/wrapper_https_client.h"

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
extern "C" JNIEXPORT jobjectArray JNICALL
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

  jclass object_class = env->FindClass("java/lang/Object");
  jobjectArray result = env->NewObjectArray(3, object_class, nullptr);

  auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
  if (https_client) {
    const auto resp = https_client->Get(http_handle, timeout);
    env->SetObjectArrayElement(result, 0, env->NewStringUTF(resp.body.c_str()));
    env->SetObjectArrayElement(result, 1,
        env->NewObject(env->FindClass("java/lang/Integer"),
            env->GetMethodID(
                env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            resp.code));
    env->SetObjectArrayElement(
        result, 2, env->NewStringUTF(resp.errmsg.c_str()));
  } else {
    // default value
    env->SetObjectArrayElement(result, 0, env->NewStringUTF("{}"));
    env->SetObjectArrayElement(result, 1,
        env->NewObject(env->FindClass("java/lang/Integer"),
            env->GetMethodID(
                env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            400));
    env->SetObjectArrayElement(result, 2, env->NewStringUTF("Object is empty"));
  }
  return result;
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
extern "C" JNIEXPORT jobjectArray JNICALL
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

  jclass object_class = env->FindClass("java/lang/Object");
  jobjectArray result = env->NewObjectArray(3, object_class, nullptr);

  auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
  if (https_client) {
    const auto resp = https_client->Post(http_handle, http_request, timeout);
    env->SetObjectArrayElement(result, 0, env->NewStringUTF(resp.body.c_str()));
    env->SetObjectArrayElement(result, 1,
        env->NewObject(env->FindClass("java/lang/Integer"),
            env->GetMethodID(
                env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            resp.code));
    env->SetObjectArrayElement(
        result, 2, env->NewStringUTF(resp.errmsg.c_str()));
  } else {
    // default value
    env->SetObjectArrayElement(result, 0, env->NewStringUTF("{}"));
    env->SetObjectArrayElement(result, 1,
        env->NewObject(env->FindClass("java/lang/Integer"),
            env->GetMethodID(
                env->FindClass("java/lang/Integer"), "<init>", "(I)V"),
            400));
    env->SetObjectArrayElement(result, 2, env->NewStringUTF("Object is empty"));
  }
  return result;
}
