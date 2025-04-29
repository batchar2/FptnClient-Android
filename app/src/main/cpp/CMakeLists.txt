cmake_minimum_required(VERSION 3.22.1)

project("native-lib")

find_package(ZLIB CONFIG)
find_package(fptn CONFIG)

# add_subdirectory(.conan/recipes/fptn)

add_library(${CMAKE_PROJECT_NAME} SHARED native-lib.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
    android
    log
    ZLIB::ZLIB
    fptn-protocol-lib_static
)
