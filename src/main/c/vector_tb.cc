#include <iostream>
#include <iomanip>
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>

using namespace beethoven;

int main() {
  std::cout << "========================================" << std::endl;
  std::cout << "Vector Add Testbench - Debug Mode" << std::endl;
  std::cout << "========================================" << std::endl;

  fpga_handle_t handle;
  std::cout << "[INIT] FPGA handle created successfully" << std::endl;

  int size_of_int = 4;
  int n_eles = 32;
  std::cout << "[CONFIG] Element size: " << size_of_int << " bytes" << std::endl;
  std::cout << "[CONFIG] Number of elements: " << n_eles << std::endl;
  std::cout << "[CONFIG] Total buffer size: " << (size_of_int * n_eles) << " bytes" << std::endl;

  std::cout << "\n[MALLOC] Allocating buffers..." << std::endl;
  auto vec_a = handle.malloc(size_of_int * n_eles);
  std::cout << "  vec_a allocated at FPGA addr: 0x" << std::hex << vec_a.getFpgaAddr() << std::dec << std::endl;
  std::cout << "  vec_a host addr: " << vec_a.getHostAddr() << std::endl;

  auto vec_b = handle.malloc(size_of_int * n_eles);
  std::cout << "  vec_b allocated at FPGA addr: 0x" << std::hex << vec_b.getFpgaAddr() << std::dec << std::endl;
  std::cout << "  vec_b host addr: " << vec_b.getHostAddr() << std::endl;

  auto vec_out = handle.malloc(size_of_int * n_eles);
  std::cout << "  vec_out allocated at FPGA addr: 0x" << std::hex << vec_out.getFpgaAddr() << std::dec << std::endl;
  std::cout << "  vec_out host addr: " << vec_out.getHostAddr() << std::endl;

  std::cout << "\n[INIT] Initializing input vectors..." << std::endl;
  auto vec_a_host = (int*)vec_a.getHostAddr();
  auto vec_b_host = (int*)vec_b.getHostAddr();

  for (int i = 0; i < n_eles; ++i) {
      vec_a_host[i] = i + 1;
      vec_b_host[i] = i * 2;
  }

  std::cout << "[DATA] Sample input values (first 8 elements):" << std::endl;
  std::cout << "  Index | vec_a | vec_b | expected_sum" << std::endl;
  std::cout << "  ------|-------|-------|-------------" << std::endl;
  for (int i = 0; i < std::min(8, n_eles); ++i) {
      std::cout << "  " << std::setw(5) << i << " | "
                << std::setw(5) << vec_a_host[i] << " | "
                << std::setw(5) << vec_b_host[i] << " | "
                << std::setw(12) << (vec_a_host[i] + vec_b_host[i]) << std::endl;
  }
  if (n_eles > 8) {
      std::cout << "  ... (" << (n_eles - 8) << " more elements)" << std::endl;
  }

  std::cout << "\n[COPY] Copying vec_a to FPGA..." << std::endl;
  handle.copy_to_fpga(vec_a);
  std::cout << "[COPY] vec_a copied successfully" << std::endl;

  std::cout << "[COPY] Copying vec_b to FPGA..." << std::endl;
  handle.copy_to_fpga(vec_b);
  std::cout << "[COPY] vec_b copied successfully" << std::endl;

  std::cout << "\n[ACCEL] Launching vector_add accelerator..." << std::endl;
  std::cout << "  Core ID: 0" << std::endl;
  std::cout << "  vec_a FPGA addr: 0x" << std::hex << vec_a.getFpgaAddr() << std::dec << std::endl;
  std::cout << "  vec_b FPGA addr: 0x" << std::hex << vec_b.getFpgaAddr() << std::dec << std::endl;
  std::cout << "  vec_out FPGA addr: 0x" << std::hex << vec_out.getFpgaAddr() << std::dec << std::endl;
  std::cout << "  n_elements: " << n_eles << std::endl;

  myVectorAdd::vector_add(0,
                          vec_a,
                          vec_b,
                          vec_out,
                          n_eles).get();
  std::cout << "[ACCEL] Accelerator completed successfully" << std::endl;

  std::cout << "\n[COPY] Copying vec_out from FPGA..." << std::endl;
  handle.copy_from_fpga(vec_out);
  std::cout << "[COPY] vec_out copied successfully" << std::endl;

  std::cout << "\n[VERIFY] Checking results..." << std::endl;
  auto output = (int*)vec_out.getHostAddr();
  int error_count = 0;

  for (int i = 0; i < n_eles; ++i) {
    int expected = (i+1) + (i*2);
    if (output[i] != expected) {
      std::cout << "[ERROR] Index " << i << ": output[" << i << "]=" << output[i]
                << ", expected=" << expected
                << " (vec_a[" << i << "]=" << vec_a_host[i]
                << " + vec_b[" << i << "]=" << vec_b_host[i] << ")" << std::endl;
      error_count++;
    }
  }

  std::cout << "\n[RESULTS] All output values:" << std::endl;
  std::cout << "  Index | vec_a | vec_b | output | expected | status" << std::endl;
  std::cout << "  ------|-------|-------|--------|----------|-------" << std::endl;
  for (int i = 0; i < n_eles; ++i) {
      int expected = vec_a_host[i] + vec_b_host[i];
      std::string status = (output[i] == expected) ? "PASS" : "FAIL";
      std::cout << "  " << std::setw(5) << i << " | "
                << std::setw(5) << vec_a_host[i] << " | "
                << std::setw(5) << vec_b_host[i] << " | "
                << std::setw(6) << output[i] << " | "
                << std::setw(8) << expected << " | "
                << status << std::endl;
  }

  std::cout << "\n========================================" << std::endl;
  if (error_count == 0) {
    std::cout << "[PASS] All " << n_eles << " elements verified successfully!" << std::endl;
  } else {
    std::cout << "[FAIL] " << error_count << " errors out of " << n_eles << " elements" << std::endl;
  }
  std::cout << "========================================" << std::endl;

  return error_count > 0 ? 1 : 0;
}
