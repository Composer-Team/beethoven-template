/*
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>
#include <vector>
#include <deque>

using namespace beethoven;
// ########## DO NOT REMOVE ############################
// DO NOT REMOVE THESE!
void dma_workaround_copy_to_fpga(remote_ptr &q) {
    printf("Copying to FPGA...\n");
    int sz = q.getLen() / 4;
    auto * intar = (int*)q.getHostAddr();
    for (int i = 0; i < sz; ++i) {
        printf("\rProgress: %d/%d", i, sz);
        auto ptr = q + i * 4;
        DMAHelper::memcmd(0, q + i * 4, intar[i], 1).get();
    }
    printf("\n");
}
void dma_workaround_copy_from_fpga(remote_ptr &q) {
    printf("Copying from FPGA...\n");
    int sz = q.getLen() / 4;
    auto * intar = (int*)q.getHostAddr();
    for (int i = 0; i < sz; ++i) {
        printf("\rProgress: %d/%d", i, sz);
        auto ptr = q + i * 4;
        auto resp = DMAHelper::memcmd(0, q + i * 4, 0, 0).get();
        intar[i] = resp.payload; 
    }
    printf("\n");
}
// ############# END DO NOT REMOVE ######################

// golden model for the FIR filter
std::vector<int> golden_fir(const std::vector<int> &in, const std::vector<int> &taps) {
    std::vector<int> output;
    std::deque<int> window;
    for (const auto &i: in) {
        int sum = 0;
        printf("%d\n", i);
        // push to window
        window.push_front(i);
        // perform multiplication over window
        for (int i = 0; i < taps.size() && i < window.size(); ++i) {
            sum += taps[i] * window[i];
        }
        // handle the items getting shifted outside of the window
        if (window.size() == taps.size())
            window.pop_back();
        
        // write output
        output.push_back(sum);
    }
    return output;
}

int main() {
    fpga_handle_t handle;
    int data_vector_length = 128;
    std::vector<int> taps;
    for (int i = 0; i < ACCEL_WINDOW_SIZE; ++i) {
        auto tap_value = i + 5;
        taps.push_back(tap_value);
        ///// Use the FIR set_tap command you made to send over the tap value /////
        // TODO
        printf("TAP[%d] = %d\n", i, tap_value);
    }
    
    ///// Allocate memory segments /////
    // TODO
    // remote_ptr fpga_in = ...
    // remote_ptr fpga_out = ...

    // TODO
    // extract the host pointer from the fpga_in remote_ptr
    int * fpga_in_host;

    std::vector<int> input;
    for (int i = 0; i < data_vector_length; ++i) {
        int data = i * 2 + 1;
        input.push_back(data);
        // TODO
        // Use fpga_in_host to write to the FPGA-accessible segment
    }
    // in simulation, we can use the following to copy large segments to memory quickly
    // handle.copy_to_fpga(fpga_in);
    // 
    // For the time being, we have to use a workaround for memory transfer on the F2 instances
    // **** TODO Uncomment Below ****
    // dma_workaround_copy_to_fpga(fpga_in);

    // Call the FIR Filter accelerator
    // TODO
    
    auto golden_out = golden_fir(input, taps);
    // Transfer the FPGA segment back to host memory
    // dma_workaround...

    // Extract the host pointer from fpga_out
    // int* fpga_out_host = ...

    bool success = true;
    for (int i = 0; i < data_vector_length; ++i) {
        if (golden_out[i] != fpga_out_host[i]) {
            printf("[%d]: %d =/= %d\n", i, golden_out[i], fpga_out_host[i]);
            success = false;
        }
    }
    if (success) {
        printf("Success!\n");
    }
}
    */