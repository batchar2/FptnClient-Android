#include <functional>
#include <iostream>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

#include <queue>
#include <jni.h>

class WebsocketClient {

public:
    explicit WebsocketClient(JNIEnv *env, jobject wrapper, const std::string host,
                             const int serverPort, const std::string sni);

    void Run();

    bool Stop();

    // TODO add byte []
    bool Send();

    bool IsStarted();

    const int server_port_;
    const std::string host_;
    const std::string sni_;
    JNIEnv *env;
    jobject wrapper_;

    mutable std::atomic<bool> running_ = false;

    /*todo: ADD CALLBACK!!! FOR byte []*/
};