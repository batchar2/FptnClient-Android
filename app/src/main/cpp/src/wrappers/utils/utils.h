#pragma once

#include <string>

#include <jni.h>

namespace fptn::wrapper {
    std::string ConvertToCString(JNIEnv *p_env, jstring jstr) {
        const char *chars = p_env->GetStringUTFChars(jstr, nullptr);
        return {chars};
    }
}