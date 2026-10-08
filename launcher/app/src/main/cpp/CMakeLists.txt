cmake_minimum_required(VERSION 3.22.1)
project(sklauncher LANGUAGES C CXX)

if(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(ARM32_JRE "${CMAKE_CURRENT_BINARY_DIR}/arm32-jre")
    file(MAKE_DIRECTORY "${ARM32_JRE}")
    execute_process(
        COMMAND "${CMAKE_COMMAND}" -E tar xf
            "${CMAKE_CURRENT_SOURCE_DIR}/../assets/jre25-arm32/bin-arm.tar.xz"
            ./lib/libawt_headless.so
        WORKING_DIRECTORY "${ARM32_JRE}"
        COMMAND_ERROR_IS_FATAL ANY
    )
    add_library(awt_xawt SHARED xawt_fake.c)
    target_link_options(awt_xawt PRIVATE "-Wl,--no-as-needed")
    target_link_libraries(awt_xawt "${ARM32_JRE}/lib/libawt_headless.so")
endif()

add_library(sklauncher SHARED
    sklauncher.c
)

option(SK_ARM32_INTERPRETER_TEST "Disable JIT for ARM32 startup diagnosis" OFF)
if(SK_ARM32_INTERPRETER_TEST AND ANDROID_ABI STREQUAL "armeabi-v7a")
    target_compile_definitions(sklauncher PRIVATE SK_ARM32_INTERPRETER_TEST)
endif()

find_library(log-lib log)
find_library(android-lib android)
find_library(egl-lib EGL)
find_library(glesv2-lib GLESv2)

target_link_libraries(sklauncher
    ${log-lib}
    ${android-lib}
    ${egl-lib}
    ${glesv2-lib}
)

add_executable(xdgopen xdgopen.c)

set_target_properties(xdgopen PROPERTIES
    PREFIX ""
    SUFFIX ""
    OUTPUT_NAME "libxdgopen.so"
)
