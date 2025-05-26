/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include <jni.h>

#include "wrappers/utils/utils.h"
#include "wrappers/wrapper_websocket_client/wrapper_websocket_client.h"

using fptn::wrapper::WrapperWebsocketClient;

// create an object
extern "C" JNIEXPORT jlong JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeCreate(
    JNIEnv* env,
    jobject thiz,
    jstring server_ip_param,
    jint server_port_param,
    jstring tun_ipv4_param,
    jstring sni_param,
    jstring access_token_param,
    jstring expected_md5_fingerprint_param) {
  jobject global_object_ref = env->NewWeakGlobalRef(thiz);
  auto server_ip = fptn::wrapper::ConvertToCString(env, server_ip_param);
  int server_port = server_port_param;
  auto tun_ipv4 = fptn::wrapper::ConvertToCString(env, tun_ipv4_param);
  auto sni = fptn::wrapper::ConvertToCString(env, sni_param);
  auto access_token = fptn::wrapper::ConvertToCString(env, access_token_param);
  auto expected_md5_fingerprint =
      fptn::wrapper::ConvertToCString(env, expected_md5_fingerprint_param);

  //        public void onOpenImpl()
  //        public void onCloseImpl()
  //        public void onMessageImpl(byte[] msg)

  jclass cls_foo = env->GetObjectClass(global_object_ref);
  //
  //    jmethodID mid_callback = env->GetMethodID(cls_foo,
  //    "logMessageFromNative", "(Ljava/lang/String;)V"); jstring jstr =
  //    env->NewStringUTF("NativeRun"); env->CallVoidMethod(wrapper_,
  //    mid_callback, jstr);

  //    JavaVM jvm = nullptr;
  //    GetJavaVM(env, &jvm);

  jmethodID on_open_impl = env->GetMethodID(cls_foo, "onOpenImpl", "()V");
  jmethodID on_close_impl = env->GetMethodID(cls_foo, "onCloseImpl", "()V");
  jmethodID on_message_impl =
      env->GetMethodID(cls_foo, "onMessageImpl", "([B)V");

  auto* websocket_client = new WrapperWebsocketClient(global_object_ref,
      std::move(server_ip), server_port, std::move(tun_ipv4), std::move(sni),
      std::move(access_token), std::move(expected_md5_fingerprint),
      on_open_impl, on_close_impl, on_message_impl);
  return reinterpret_cast<jlong>(websocket_client);
}

// Destroy
extern "C" JNIEXPORT void JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeDestroy(
    JNIEnv* env, jobject thiz, jlong native_handle) {
  (void)env;
  (void)thiz;

  auto* websocket_client =
      reinterpret_cast<WrapperWebsocketClient*>(native_handle);
  if (websocket_client) {
    websocket_client->Stop();
    delete websocket_client;
  }
}

// Run
extern "C" JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeRun(
    JNIEnv* env, jobject thiz, jlong native_handle) {
  (void)env;
  (void)thiz;

  bool status = false;
  auto* websocket_client =
      reinterpret_cast<WrapperWebsocketClient*>(native_handle);
  if (websocket_client) {
    status = websocket_client->Start();
  }
  return static_cast<jboolean>(status);
}

// Stop
extern "C" JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeStop(
    JNIEnv* env, jobject thiz, jlong native_handle) {
  (void)env;
  (void)thiz;

  bool status = false;
  auto* websocket_client =
      reinterpret_cast<WrapperWebsocketClient*>(native_handle);
  if (websocket_client) {
    status = websocket_client->Stop();
  }
  return static_cast<jboolean>(status);
}

// Send
extern "C" JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeSend(
    JNIEnv* env, jobject thiz, jlong native_handle, jbyteArray data) {
  (void)thiz;

  bool status = false;
  auto* websocket_client =
      reinterpret_cast<WrapperWebsocketClient*>(native_handle);
  if (websocket_client && env) {
    // Java bytes to std::string
    jbyte* buffer = env->GetByteArrayElements(data, nullptr);
    const jsize length = env->GetArrayLength(data);
    if (buffer != nullptr && length != 0) {
      std::string packet(reinterpret_cast<const char*>(buffer), length);
      status = websocket_client->Send(std::move(packet));
    }
    if (buffer) {
      // Release the buffer
      env->ReleaseByteArrayElements(data, buffer, JNI_ABORT);
    }
  }
  return static_cast<jboolean>(status);
}

// IsStarted
extern "C" JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeIsStarted(
    JNIEnv* env, jobject thiz, jlong native_handle) {
  (void)env;
  (void)thiz;

  bool status = false;
  auto* websocket_client =
      reinterpret_cast<WrapperWebsocketClient*>(native_handle);
  if (websocket_client) {
    status = websocket_client->IsStarted();
  }
  return static_cast<jboolean>(status);
}
