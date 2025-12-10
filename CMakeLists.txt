cmake_minimum_required(VERSION 3.15.0)
project(beethoven-template)

find_package(beethoven REQUIRED)

set(CMAKE_CXX_STANDARD 20)

beethoven_hardware(systolic_array
  MAIN_CLASS systolic.verilog.SystolicArrayConfig_SOLUTION
)

beethoven_testbench(systolic_array_test
    SOURCES src/test/c/systolic/systolic_array_test.cc
    HARDWARE systolic_array
    SIMULATOR verilator
)
