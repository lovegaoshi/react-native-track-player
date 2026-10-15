cmake_minimum_required(VERSION 3.13)

project(trackplayer)

# React Native headers
set(REACT_NATIVE_DIR ${CMAKE_SOURCE_DIR}/../../../../node_modules/react-native/ReactCommon)

# Specify path to React Native includes
include_directories(
        ${REACT_NATIVE_DIR}
        ${REACT_NATIVE_DIR}/jsi
        ${REACT_NATIVE_DIR}/callinvoker
        ${REACT_NATIVE_DIR}/react/nativemodule/core
)

add_library(trackplayer SHARED
        ${CMAKE_SOURCE_DIR}/JsiBindings.cpp
        ${REACT_NATIVE_DIR}/jsi/jsi/jsi.cpp
)

find_library(log-lib log)

target_link_libraries(
        trackplayer
        ${log-lib}
        android
        atomic
        m
)