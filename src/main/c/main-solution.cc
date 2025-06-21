#include <iostream>
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>

using namespace beethoven;

using input_type = uint16_t;
using output_type = uint32_t;

int main() {
    fpga_handle_t handle;
    int size_of_int = 4;
    
    // Define problem dimensions
    int vector_length = 128;  // Must be <= N (128 from your config)
    int num_cols = 64;        // Number of columns in matrix

    printf("Vector length: %d, Number of columns: %d\n", vector_length, num_cols);
    
    // Allocate memory
    auto vec_in = handle.malloc(sizeof(input_type) * vector_length);
    auto mat_in = handle.malloc(sizeof(input_type) * vector_length * num_cols);  // Column-major matrix
    auto vec_out = handle.malloc(sizeof(output_type) * num_cols);  // Output vector

    printf("Memory allocated.\n");
    
    // Get host pointers
    auto vec_in_host = (input_type*)vec_in.getHostAddr();
    auto mat_in_host = (input_type*)mat_in.getHostAddr();
    auto vec_out_host = (output_type*)vec_out.getHostAddr();

    printf("Host pointers obtained.\n");
    printf("%08x <- vec input\n", vec_in.getFpgaAddr());
    
    // Initialize input vector
    for (int i = 0; i < vector_length; ++i) {
        vec_in_host[i] = i + 1;  // Simple test pattern
    }
    
    // Initialize matrix (column-major order)
    for (int col = 0; col < num_cols; ++col) {
        for (int row = 0; row < vector_length; ++row) {
            mat_in_host[col * vector_length + row] = (col + 1) * (row + 1);
        }
    }
    
    // Copy data to FPGA
    handle.copy_to_fpga(vec_in);
    handle.copy_to_fpga(mat_in);

    printf("Input vector and matrix copied to FPGA.\n");
    
    // Call the accelerator
    auto resp_handle = myVectorMatMul::vector_mat_mul(0,
                                                      num_cols,
                                                      mat_in,
                                                      vec_in,
                                                      vec_out,
                                                      vector_length);

    printf("Accelerator called, waiting for response...\n");
    
    // Wait for completion
    auto response = resp_handle.get();

    printf("Response received: %s\n", response ? "Success" : "Failure");
    
    // Copy result back from FPGA
    handle.copy_from_fpga(vec_out);
    
    // Print results (optional)
    std::cout << "Results:" << std::endl;
    for (int i = 0; i < num_cols; ++i) {
        std::cout << "vec_out[" << i << "] = " << vec_out_host[i] << std::endl;
    }
    
    return 0;
}
