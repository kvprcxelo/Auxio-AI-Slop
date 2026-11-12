cmake_minimum_required(VERSION 3.22)

project(auxiotags CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# The taglib source and install tree come from the musikr submodule, so both the Android app and
# this module read tags with the same library version.
if(NOT DEFINED TAGLIB_ROOT)
    set(TAGLIB_ROOT
        "${CMAKE_CURRENT_SOURCE_DIR}/../../../../musikr/src/main/cpp/taglib/pkg/linux-x86_64")
endif()

if(NOT EXISTS "${TAGLIB_ROOT}/lib/libtag.a")
    message(FATAL_ERROR
        "taglib is not built for Linux at ${TAGLIB_ROOT}. "
        "Run linux/packaging/build-taglib.sh first.")
endif()

find_package(JNI REQUIRED)

add_library(auxiotags SHARED tagreader.cpp)

target_include_directories(auxiotags PRIVATE
    "${TAGLIB_ROOT}/include"
    "${TAGLIB_ROOT}/include/utf8cpp"
    ${JNI_INCLUDE_DIRS})

target_link_libraries(auxiotags PRIVATE "${TAGLIB_ROOT}/lib/libtag.a")

set_target_properties(auxiotags PROPERTIES OUTPUT_NAME "auxiotags")
