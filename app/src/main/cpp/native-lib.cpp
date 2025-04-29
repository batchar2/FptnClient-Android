#include <jni.h>
#include <string>

#include <jni.h>
#include <string>
#include "zlib.h"

//extern "C"
//JNIEXPORT jstring JNICALL
//Java_org_fptn_vpn_utils_NativeLib_stringFromJNI(JNIEnv *env, jobject thiz) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}
extern "C"
JNIEXPORT jstring JNICALL
Java_org_fptn_vpn_utils_NativeLib_multipleStr(JNIEnv *env, jobject thiz, jstring input_string,
                                              jint times, jboolean with_comma) {
    const char *chars = env->GetStringUTFChars(input_string, nullptr);
    std::string cppString(chars);
    std::string result;
    for (int i = 0; i < times; ++i) {
        result += cppString;
        if (with_comma) {
            if (i == times - 1) {
                result += '.';
            } else {
                result += std::string(", ");
            }
        }
    }
    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_org_fptn_vpn_utils_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++, zlib version: ";
    hello.append(zlibVersion());
    return env->NewStringUTF(hello.c_str());
}

