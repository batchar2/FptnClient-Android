cmake_minimum_required(VERSION 3.22.1)

project("fptn_native_lib")

find_package(ZLIB CONFIG)
find_package(absl REQUIRED)
find_package(Protobuf REQUIRED)
find_package(OpenSSL REQUIRED)
find_package(argparse REQUIRED)
find_package(spdlog REQUIRED)
find_package(fmt REQUIRED)
find_package(PcapPlusPlus REQUIRED)
find_package(Boost REQUIRED COMPONENTS random filesystem)
find_package(ZLIB REQUIRED)
find_package(nlohmann_json REQUIRED)

message(STATUS "OpenSSL_FOUND: ${OpenSSL_FOUND}")
message(STATUS "OpenSSL_INCLUDE_DIR: ${OPENSSL_INCLUDE_DIR}")
message(STATUS "OpenSSL_LIBRARIES: ${OPENSSL_LIBRARIES}")
message(STATUS "OpenSSL_VERSION: ${OPENSSL_VERSION}")

#exit
#find_package(fptn CONFIG)
include_directories(src/)
include_directories(${OpenSSL_INCLUDE_DIR})

include_directories(libs/fptn/src/)
include_directories(libs/fptn/src/fptn-protocol-lib)
set(FPTN_BUILD_ONLY_FPTN_LIB ON CACHE INTERNAL "Build only FPTN lib")

add_subdirectory(libs/fptn)

if(Protobuf_INCLUDE_DIRS)
    include_directories(${Protobuf_INCLUDE_DIRS})
endif()
add_library(${CMAKE_PROJECT_NAME}
        SHARED

        src/jnienv/jnienv.h
        src/jnienv/jnienv.cpp

        src/wrappers/wrapper_websocket_client/wrapper_websocket_client.h
        src/wrappers/wrapper_websocket_client/wrapper_websocket_client.cpp
        src/wrappers/wrapper_https_client/wrapper_https_client.h
        src/wrappers/wrapper_https_client/wrapper_https_client.cpp

        src/https_client.cpp
        src/websocket_client.cpp
)

target_link_libraries(${CMAKE_PROJECT_NAME}
    android
    log
    ZLIB::ZLIB
    fptn-protocol-lib_static
)
