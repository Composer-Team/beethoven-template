cmake_minimum_required(VERSION 3.15)
project(fir)

find_package(beethoven REQUIRED)
set(CMAKE_CXX_STANDARD 17)

beethoven_build(fir_tb SOURCES fir_tb_SOLUTION.cc)
