cmake_minimum_required(VERSION 3.18.1)
project("iperf3wrapper")

# Set C standard
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Add iperf3 source files
set(IPERF3_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/iperf3)

# Check if source files exist
if(NOT EXISTS "${IPERF3_SRC_DIR}/src/iperf_api.c")
    message(FATAL_ERROR "iperf3 source files not found at ${IPERF3_SRC_DIR}/src/")
endif()

# Define iperf3 library sources (only core files needed for client)
set(IPERF3_SOURCES
    ${IPERF3_SRC_DIR}/src/cjson.c
    ${IPERF3_SRC_DIR}/src/iperf_api.c
    ${IPERF3_SRC_DIR}/src/iperf_auth.c
    ${IPERF3_SRC_DIR}/src/iperf_client_api.c
    ${IPERF3_SRC_DIR}/src/iperf_server_api.c
    ${IPERF3_SRC_DIR}/src/iperf_error.c
    ${IPERF3_SRC_DIR}/src/iperf_locale.c
    ${IPERF3_SRC_DIR}/src/iperf_tcp.c
    ${IPERF3_SRC_DIR}/src/iperf_time.c
    ${IPERF3_SRC_DIR}/src/iperf_udp.c
    ${IPERF3_SRC_DIR}/src/iperf_util.c
    ${IPERF3_SRC_DIR}/src/net.c
    ${IPERF3_SRC_DIR}/src/tcp_info.c
    ${IPERF3_SRC_DIR}/src/timer.c
    ${IPERF3_SRC_DIR}/src/units.c
    ${IPERF3_SRC_DIR}/src/dscp.c
)

# Create static library from iperf3 sources
add_library(iperf3_static STATIC ${IPERF3_SOURCES})

# Configure iperf3 compilation flags
target_compile_definitions(iperf3_static PRIVATE
    HAVE_CONFIG_H
    PACKAGE_VERSION="3.16"
    PACKAGE_BUGREPORT=""
    PACKAGE_NAME="iperf3"
    PACKAGE_STRING="iperf3 3.16"
)

# Include directories
target_include_directories(iperf3_static PRIVATE
    ${IPERF3_SRC_DIR}/src
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# JNI wrapper library
add_library(iperf3wrapper SHARED
    iperf3_jni.c
)

# Link libraries
target_link_libraries(iperf3wrapper
    iperf3_static
    log
    android
)

# Include directories for JNI wrapper
target_include_directories(iperf3wrapper PRIVATE
    ${IPERF3_SRC_DIR}/src
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# Configure JNI wrapper compilation flags
target_compile_definitions(iperf3wrapper PRIVATE
    HAVE_CONFIG_H
)