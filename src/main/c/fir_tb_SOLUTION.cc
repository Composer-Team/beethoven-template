#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>
#include <vector>
#include <deque>

using namespace beethoven;

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

std::vector<int> golden_fir(const std::vector<int> &in, const std::vector<int> &taps) {
    std::vector<int> output;
    std::deque<int> window;
    for (const auto &i: in) {
        int sum = 0;
        printf("%d\n", i);
        window.push_front(i);
        for (int i = 0; i < taps.size() && i < window.size(); ++i) {
            sum += taps[i] * window[i];
        }
        if (window.size() == taps.size())
            window.pop_back();
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
        FIR::set_taps(0, i, tap_value);
        printf("TAP[%d] = %d\n", i, tap_value);
    }
    
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