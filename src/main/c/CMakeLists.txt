cmake_minimum_required(VERSION 3.15)
project(testbenches)

find_package(beethoven REQUIRED)
set(CMAKE_CXX_STANDARD 17)

beethoven_build(vector_tb SOURCES vector_tb.cc)

beethoven_build(fir_tb SOURCES fir_tb_SOLUTION.cc)

beethoven_build(vector_dot SOURCES main.cc)

# Option to enable/disable Python bindings
option(BUILD_PYTHON_BINDINGS "Build Python bindings" OFF)

if(BUILD_PYTHON_BINDINGS)
    find_package(pybind11 REQUIRED)
    message(STATUS "Building Python bindings")
    # Python binding module
    pybind11_add_module(beethoven_python 
        python_bindings.cpp
    )
    
    link_beethoven_to_target(beethoven_python)
else()
    message(STATUS "Python bindings disabled")
endif()
