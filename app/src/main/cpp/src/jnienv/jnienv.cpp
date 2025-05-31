#include "jnienv.h"


#include <cstdlib>

static JavaVM *s_java_vm = nullptr;

JNIEnv *getJniEnv() {
    JNIEnv *env = nullptr;

    switch (s_java_vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6)) {
        case JNI_OK:
            return env;
        case JNI_EDETACHED: {
            JavaVMAttachArgs args;
            args.name = nullptr;
            args.group = nullptr;
            args.version = JNI_VERSION_1_6;

            if (!s_java_vm->AttachCurrentThreadAsDaemon(&env, &args)) {
                return env;
            }
            break;
        }
    }
    return nullptr;
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *aJavaVM, void *aReserved) {
    (void) aReserved;
    s_java_vm = aJavaVM;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *aJavaVM, void *aReserved) {
    (void) aJavaVM;
    (void) aReserved;
    s_java_vm = nullptr;
}
