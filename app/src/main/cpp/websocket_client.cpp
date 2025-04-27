#include "websocket_client.h"
#include <jni.h>
#include <string>

/*void WebsocketClient::logToAndroid(const char *message) const {
*//*    jclass cls = env->FindClass("org/fptn/vpn/services/nativewrapper/NativeWebsocketWrapper");
    jmethodID methodid = env->GetStaticMethodID(cls, "logMessageFromNativeStatic",
                                                "(Ljava/lang/String;)V");

    jstring jstr = env->NewStringUTF(message);
    env->CallStaticVoidMethod(cls, methodid, jstr);*//*

}*/

bool WebsocketClient::IsStarted() {
    return running_;
}

void WebsocketClient::Run() {
    running_ = true;
    //logToAndroid("Running");
}

bool WebsocketClient::Send() {
    //logToAndroid("Sending");
    return true;
}

bool WebsocketClient::Stop() {
    running_ = false;
    //logToAndroid("Stop");
    return running_;
}

WebsocketClient::WebsocketClient(JNIEnv *env, jobject wrapper, const std::string host,
                                 const int serverPort, const std::string sni)
        : server_port_(serverPort), host_(host), sni_(sni), env(env), wrapper_(wrapper) {}

std::string convertToCString(JNIEnv *pEnv, jstring pJstring);

std::string convertToCString(JNIEnv *pEnv, jstring pJstring) {
    const char *chars = pEnv->GetStringUTFChars(pJstring, nullptr);
    return {chars};
}

extern "C"
JNIEXPORT jlong JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeCreate(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jstring host, jint port,
                                                                            jstring sni) {
    jobject globalObjectRef = env->NewWeakGlobalRef(thiz);

    auto *pWebsocketClient = new WebsocketClient(env, globalObjectRef, convertToCString(env, host),
                                                 port,
                                                 convertToCString(env, sni));
    return reinterpret_cast<jlong>(pWebsocketClient);
}
extern "C"
JNIEXPORT void JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeDestroy(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jlong native_handle) {
    auto *pWebsocketClient = reinterpret_cast<WebsocketClient *>(native_handle);
    delete pWebsocketClient;
}
extern "C"
JNIEXPORT void JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeRun(JNIEnv *env, jobject thiz,
                                                                         jlong native_handle) {
    auto *pWebsocketClient = reinterpret_cast<WebsocketClient *>(native_handle);
    pWebsocketClient->Run();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeStop(JNIEnv *env, jobject thiz,
                                                                          jlong native_handle) {
    auto *pWebsocketClient = reinterpret_cast<WebsocketClient *>(native_handle);
    return static_cast<jboolean>(pWebsocketClient->Stop());
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeSend(JNIEnv *env, jobject thiz,
                                                                          jlong native_handle,
                                                                          jbyteArray data) {
    // TODO: implement nativeSend()
    auto *pWebsocketClient = reinterpret_cast<WebsocketClient *>(native_handle);
    pWebsocketClient->Send();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fptn_vpn_services_websocket_NativeWebSocketClientImpl_nativeIsStarted(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jlong native_handle) {
    auto *pWebsocketClient = reinterpret_cast<WebsocketClient *>(native_handle);
    return static_cast<jboolean>(pWebsocketClient->IsStarted());
}