/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#pragma once

#include <jni.h>
#include <string>

namespace fptn::wrapper {
    inline std::string ConvertToCString(JNIEnv *p_env, jstring jstr) {
        const char *chars = p_env->GetStringUTFChars(jstr, nullptr);
        return {chars};
    }
}