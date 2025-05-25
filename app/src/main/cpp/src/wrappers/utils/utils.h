/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#pragma once

#include <jni.h>
#include <string>

namespace fptn::wrapper {
inline std::string ConvertToCString(JNIEnv* p_env, jstring jstr) {
  const char* chars = p_env->GetStringUTFChars(jstr, nullptr);
  return {chars};
}


//inline JNIEnv* GetJniEnv() {
//    JNIEnv* env = nullptr;
//    int status = jvm_->GetEnv((void**)&env, JNI_VERSION_1_6);
//    if (status == JNI_EDETACHED) {
//        status = jvm_->AttachCurrentThread(&env, nullptr);
//        if (status < 0) {
//            // Обработка ошибки
//            return nullptr;
//        }
//    }
//    return env;
//}
}  // namespace fptn::wrapper
