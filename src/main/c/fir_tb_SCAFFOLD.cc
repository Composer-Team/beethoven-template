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
    
    ///// Use the FIR set_tap command you made to send over the tap value /////
    // TODO
    auto fpga_in = handle.malloc(sizeof(int) * data_vector_length);
    auto fpga_in_host = (int*)fpga_in.getHostAddr();
    std::vector<int> input;
    for (int i = 0; i < data_vector_length; ++i) {
        int data = i * 2 + 1;
        fpga_in_host[i] = data;
        input.push_back(data);
    }
    // in simulation, this is fine
    handle.copy_to_fpga(fpga_in);

    auto fpga_out = handle.malloc(sizeof(int) * data_vector_length);
    FIR::do_filter(0, fpga_in, data_vector_length, fpga_out).get();

    auto golden_out = golden_fir(input, taps);
    auto fpga_out_host = (int*)fpga_out.getHostAddr();
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