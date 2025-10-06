cmake_minimum_required(VERSION 3.15)
project(testbenches)

find_package(beethoven REQUIRED)
set(CMAKE_CXX_STANDARD 17)

beethoven_build(vector_add_tb SOURCES vector_tb.cc)
