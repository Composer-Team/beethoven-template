#include <iostream>
#include <iomanip>
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>
#include <sys/mman.h>
#include <fcntl.h>
#include <unistd.h>
#include <cstring>

using namespace beethoven;

// Direct MMIO access functions
volatile uint32_t* mmio_base = nullptr;

void setup_cacheprot_mmio() {
  int fd = open("/dev/mem", O_SYNC | O_RDWR);
  if (fd < 0) {
    std::cerr << "Error opening /dev/mem: " << strerror(errno) << std::endl;
    std::cerr << "Make sure you run with sudo or have appropriate permissions" << std::endl;
    exit(1);
  }

  void* mapped = mmap(nullptr, 0x1000, PROT_READ | PROT_WRITE,
                      MAP_SHARED, fd, BeethovenMMIOOffset);
  if (mapped == MAP_FAILED) {
    std::cerr << "Failed to mmap MMIO region: " << strerror(errno) << std::endl;
    close(fd);
    exit(1);
  }

  mmio_base = (volatile uint32_t*)mapped;
  close(fd);  // Can close fd after mmap
}

void write_cacheprot(uint32_t value) {
  if (mmio_base == nullptr) {
    std::cerr << "MMIO not initialized!" << std::endl;
    return;
  }
  mmio_base[CACHEPROT / 4] = value;  // Divide by 4 for uint32_t indexing
}

uint32_t read_cacheprot() {
  if (mmio_base == nullptr) {
    std::cerr << "MMIO not initialized!" << std::endl;
    return 0;
  }
  return mmio_base[CACHEPROT / 4];
}

int main() {
  std::cout << "========================================" << std::endl;
  std::cout << "Vector Add Testbench - Debug Mode" << std::endl;
  std::cout << "========================================" << std::endl;

  // Setup MMIO access for CACHEPROT register
  std::cout << "[INIT] Setting up MMIO access..." << std::endl;
  setup_cacheprot_mmio();
  std::cout << "[INIT] MMIO access configured" << std::endl;

  fpga_handle_t handle;
  std::cout << "[INIT] FPGA handle created successfully" << std::endl;

  // Configure AXI CACHE and PROT bits for cache coherency
  // Format: bits [6:3] = CACHE, bits [2:0] = PROT
  // Try different values to fix coherency issues:
  //   0x02 = Non-cacheable (CACHE=0000, PROT=010) - safest, slowest
  //   0x0A = Bufferable but non-cacheable (CACHE=0001, PROT=010)
  //   0x7A = Default - fully cacheable (CACHE=1111, PROT=010) - may have coherency issues
  // NOTE: This requires hardware rebuild with hasDebugAXICACHEPROT=true
  uint32_t cache_prot_value = 0x02;  // Start with non-cacheable
  bool skip_copies = true;  // Set to true to test cache coherency without copy operations
  std::cout << "[CONFIG] Skip DMA copies: " << (skip_copies ? "YES (testing coherency)" : "NO") << std::endl;
  std::cout << "[CONFIG] Setting CACHEPROT register to 0x" << std::hex << cache_prot_value << std::dec << std::endl;
  write_cacheprot(cache_prot_value);

  // Verify the write
  uint32_t readback = read_cacheprot();
  std::cout << "[CONFIG] CACHEPROT readback: 0x" << std::hex << readback << std::dec << std::endl;

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

  // Manual cache flush - force data to RAM
  std::cout << "[CACHE] Flushing CPU cache to RAM..." << std::endl;
  __sync_synchronize();  // Memory barrier
  asm volatile("dc civac, %0" : : "r"(vec_a_host) : "memory");
  asm volatile("dc civac, %0" : : "r"(vec_b_host) : "memory");
  for (int i = 0; i < n_eles * 4; i += 64) {  // Flush every cache line (64 bytes)
    asm volatile("dc civac, %0" : : "r"((char*)vec_a_host + i) : "memory");
    asm volatile("dc civac, %0" : : "r"((char*)vec_b_host + i) : "memory");
  }
  std::cout << "[CACHE] Cache flushed successfully" << std::endl;

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

  if (skip_copies) {
    std::cout << "\n[SKIP] Skipping copy_to_fpga (testing cache coherency)..." << std::endl;
  } else {
    std::cout << "\n[COPY] Copying vec_a to FPGA..." << std::endl;
    handle.copy_to_fpga(vec_a);
    std::cout << "[COPY] vec_a copied successfully" << std::endl;

    std::cout << "[COPY] Copying vec_b to FPGA..." << std::endl;
    handle.copy_to_fpga(vec_b);
    std::cout << "[COPY] vec_b copied successfully" << std::endl;
  }

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

  // Manual cache invalidate - force re-read from RAM
  std::cout << "\n[CACHE] Invalidating CPU cache for vec_out..." << std::endl;
  __sync_synchronize();
  for (int i = 0; i < n_eles * 4; i += 64) {  // Invalidate every cache line (64 bytes)
    asm volatile("dc civac, %0" : : "r"((char*)vec_out.getHostAddr() + i) : "memory");
  }
  std::cout << "[CACHE] Cache invalidated successfully" << std::endl;

  // Debug: Check vec_out before copy
  auto output_before = (int*)vec_out.getHostAddr();
  std::cout << "\n[DEBUG] vec_out[0] before copy: " << output_before[0] << std::endl;
  std::cout << "[DEBUG] vec_out[16] before copy: " << output_before[16] << std::endl;

  if (skip_copies) {
    std::cout << "\n[SKIP] Skipping copy_from_fpga (testing cache coherency)..." << std::endl;
  } else {
    std::cout << "\n[COPY] Copying vec_out from FPGA..." << std::endl;
    handle.copy_from_fpga(vec_out);
    std::cout << "[COPY] vec_out copied successfully" << std::endl;
  }

  // Debug: Check vec_out after copy
  std::cout << "[DEBUG] vec_out[0] after copy/skip: " << output_before[0] << std::endl;
  std::cout << "[DEBUG] vec_out[16] after copy/skip: " << output_before[16] << std::endl;

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
