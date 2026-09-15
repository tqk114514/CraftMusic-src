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

# 产物必须零外部依赖，否则根本加载不起来。
# 曾经用 MSVC Debug 配置编译，DLL 依赖 VCRUNTIME140D.dll / ucrtbased.dll ——
# 这两个是调试运行时，只随 Visual Studio 安装，不在 VC++ Redistributable 里，
# 微软也禁止随应用分发。结果是任何没装 VS 的玩家机器上都加载失败，
# 表现为播放按钮变灰、双击无反应。换编译时务必复查依赖：
#   objdump -p craftmusic_audio.dll | grep "DLL Name"
# 正确结果应当只有 KERNEL32.dll、msvcrt.dll 这类系统库。
if(MSVC)
    target_compile_definitions(craftmusic_audio PRIVATE _CRT_SECURE_NO_WARNINGS NOMINMAX)
    # 源文件按 UTF-8 解析，避免中文注释触发 C4819
    target_compile_options(craftmusic_audio PRIVATE /utf-8)
    # 静态链接 VC++ 运行时（/MT），避免依赖 VCRUNTIME140.dll
    set_property(TARGET craftmusic_audio PROPERTY MSVC_RUNTIME_LIBRARY "MultiThreaded")
else()
    # MinGW/Clang：静态链接 libgcc，避免依赖 libgcc_s_seh-1.dll / libwinpthread-1.dll
    target_link_options(craftmusic_audio PRIVATE -static-libgcc)
endif()

# 输出目录（可选）
set_target_properties(craftmusic_audio PROPERTIES
    ARCHIVE_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
    LIBRARY_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
    RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}
)

message(STATUS "JNI include dirs: ${JNI_INCLUDE_DIRS}")
