/*=============================================================================
Copyright (c) 2024-2025 Stas Skokov
Copyright (c) 2024-2025 brightsunshine54

Distributed under the MIT License (https://opensource.org/licenses/MIT)
=============================================================================*/

#include "utils.h"

#include <mutex>

#include "common/logger/logger.h"

bool fptn::wrapper::init_logger() {
  static std::once_flag flag;
  static bool initialized = false;

  std::call_once(
      flag, []() { initialized = fptn::logger::init("fptn-android-client"); });

  return initialized;
}
