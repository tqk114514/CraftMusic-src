cmake_minimum_required(VERSION 3.15)

project(craftmusic_audio LANGUAGES C)

set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# 查找 JDK/JNI 头文件
find_package(JNI REQUIRED)

add_library(craftmusic_audio SHARED
    src/craftmusic_audio.c
)

target_include_directories(craftmusic_audio
    PRIVATE
        ${JNI_INCLUDE_DIRS}
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

if(MSVC)
    target_compile_definitions(craftmusic_audio PRIVATE _CRT_SECURE_NO_WARNINGS NOMINMAX)
    # 源文件按 UTF-8 解析，避免中文注释触发 C4819
    target_compile_options(craftmusic_audio PRIVATE /utf-8)
    # 可选：/MD vs /MT，默认使用 /MD
else()
    # 对于 MinGW/Clang 可按需添加链接库
    # target_link_libraries(craftmusic_audio PRIVATE winmm)
endif()

# 输出目录（可选）
set_target_properties(craftmusic_audio PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
)

message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
