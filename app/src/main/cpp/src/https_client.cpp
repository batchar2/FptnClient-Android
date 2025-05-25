#include <jni.h>

#include "wrappers/utils/utils.h"
#include "wrappers/wrapper_https_client/wrapper_https_client.h"

using fptn::wrapper::WrapperHttpsClient;

// create an object
extern "C"
JNIEXPORT jlong JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativeCreate(
    JNIEnv *env,
    jobject thiz,
    jstring host_param,
    jint port_param,
    jstring sni_param,
    jstring md5_fingerprint_param)
{
    jobject global_object_ref = env->NewWeakGlobalRef(thiz);

    auto host = fptn::wrapper::ConvertToCString(env, host_param);
    int port = port_param;
    auto sni = fptn::wrapper::ConvertToCString(env, sni_param);
    auto md5_fingerprint = fptn::wrapper::ConvertToCString(env, md5_fingerprint_param);

    auto* https_client = new WrapperHttpsClient(
        env,
        global_object_ref,
        std::move(host),
        port,
        std::move(sni),
        std::move(md5_fingerprint)
    );
    return reinterpret_cast<jlong>(https_client);
}

// Destroy
extern "C"
JNIEXPORT void JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativeDestroy(
        JNIEnv *env,
        jobject thiz,
        jlong native_handle)
{
    (void)env;
    (void)thiz;

    auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
    if (https_client) {
        delete https_client;
    }
}

// Get request
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativeGet(
        JNIEnv *env,
        jobject thiz,
        jlong native_handle,
        jstring http_handle_param,
        jint timeout_param
)
{
    (void)thiz;

    const auto http_handle = fptn::wrapper::ConvertToCString(env, http_handle_param);
    int timeout = timeout_param;

    auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
    if (https_client) {
        auto resp = https_client->Get(http_handle, timeout);
        (void)resp;
    }
    return static_cast<jboolean>(true);
}

// Post request
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeHttpsClientImpl_nativePost(
        JNIEnv *env,
        jobject thiz,
        jlong native_handle,
        jstring http_handle_param,
        jstring http_request_param,
        jint timeout_param
)
{
    (void)thiz;

    const auto http_handle = fptn::wrapper::ConvertToCString(env, http_handle_param);
    const auto http_request = fptn::wrapper::ConvertToCString(env, http_request_param);
    int timeout = timeout_param;

    auto* https_client = reinterpret_cast<WrapperHttpsClient*>(native_handle);
    if (https_client) {
        auto resp = https_client->Post(http_handle, http_request, timeout);
        (void)resp;
    }
    return static_cast<jboolean>(true);
}
