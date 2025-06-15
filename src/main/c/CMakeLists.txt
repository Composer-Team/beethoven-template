cmake_minimum_required(VERSION 3.15)
project(vector_dot)

find_package(beethoven REQUIRED)
set(CMAKE_CXX_STANDARD 17)

beethoven_build(vector_dot
    SOURCES main.cc)