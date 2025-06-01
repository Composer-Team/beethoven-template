#include <iostream>
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>

using namespace beethoven;
int main() {
  fpga_handle_t handle;
  int size_of_int = 4;
  int n_eles = 32;
  auto vec_a = handle.malloc(size_of_int * n_eles);
  auto vec_b = handle.malloc(size_of_int * n_eles);
  auto vec_out = handle.malloc(size_of_int * n_eles);

  auto vec_a_host = (int*)vec_a.getHostAddr();
  auto vec_b_host = (int*)vec_b.getHostAddr();
  for (int i = 0; i < n_eles; ++i) {
      vec_a_host[i] = i + 1;
      vec_b_host[i] = i * 2;
  }
  handle.copy_to_fpga(vec_a);
  handle.copy_to_fpga(vec_b);
  auto resp_handle = myVectorAdd::vector_add(0,
                                             vec_a,
                                             vec_b,
                                             vec_out,
                                             n_eles);
  auto response = resp_handle.get();
  handle.copy_from_fpga(vec_out);
}