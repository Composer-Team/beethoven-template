#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <beethoven/fpga_handle.h>
#include <beethoven_hardware.h>
#include <unordered_map>
#include <memory>

using namespace beethoven;

class BeethovenWrapper {
private:
    fpga_handle_t handle;
    std::unordered_map<size_t, decltype(handle.malloc(0))> memory_map;
    size_t next_id = 0;
    
public:
    BeethovenWrapper() = default;
    
    // Allocate memory and return a handle ID
    size_t malloc(size_t size) {
        auto mem = handle.malloc(size);
        memory_map[next_id] = mem;
        return next_id++;
    }
    
    // Get host pointer for a memory ID (returns memory address as integer)
    uintptr_t get_host_ptr(size_t mem_id) {
        if (memory_map.find(mem_id) == memory_map.end()) {
            throw std::runtime_error("Invalid memory ID");
        }
        return reinterpret_cast<uintptr_t>(memory_map[mem_id].getHostAddr());
    }
    
    // Write integer array to memory
    void write_int_array(size_t mem_id, const std::vector<int>& data) {
        if (memory_map.find(mem_id) == memory_map.end()) {
            throw std::runtime_error("Invalid memory ID");
        }
        
        auto host_ptr = static_cast<int*>(memory_map[mem_id].getHostAddr());
        std::memcpy(host_ptr, data.data(), data.size() * sizeof(int));
    }
    
    // Read integer array from memory
    std::vector<int> read_int_array(size_t mem_id, size_t num_elements) {
        if (memory_map.find(mem_id) == memory_map.end()) {
            throw std::runtime_error("Invalid memory ID");
        }
        
        auto host_ptr = static_cast<int*>(memory_map[mem_id].getHostAddr());
        std::vector<int> result(num_elements);
        std::memcpy(result.data(), host_ptr, num_elements * sizeof(int));
        return result;
    }
    
    // Copy data to FPGA
    void copy_to_fpga(size_t mem_id) {
        if (memory_map.find(mem_id) == memory_map.end()) {
            throw std::runtime_error("Invalid memory ID");
        }
        handle.copy_to_fpga(memory_map[mem_id]);
    }
    
    // Copy data from FPGA
    void copy_from_fpga(size_t mem_id) {
        if (memory_map.find(mem_id) == memory_map.end()) {
            throw std::runtime_error("Invalid memory ID");
        }
        handle.copy_from_fpga(memory_map[mem_id]);
    }
    
    // Vector addition
    bool vector_add(size_t vec_a_mem_id, size_t vec_b_mem_id, size_t vec_out_mem_id, int n_eles) {
        if (memory_map.find(vec_a_mem_id) == memory_map.end() ||
            memory_map.find(vec_b_mem_id) == memory_map.end() ||
            memory_map.find(vec_out_mem_id) == memory_map.end()) {
            throw std::runtime_error("Invalid memory ID");
        }
        
        auto resp_handle = myVectorAdd::vector_add(
            0,
            memory_map[vec_a_mem_id],
            memory_map[vec_b_mem_id],
            memory_map[vec_out_mem_id],
            n_eles
        );
        
        auto response = resp_handle.get();
        return response;
    }
    
    // Free memory
    void free_memory(size_t mem_id) {
        memory_map.erase(mem_id);
    }
    
    // Get number of allocated memories (for debugging)
    size_t get_memory_count() const {
        return memory_map.size();
    }
};

PYBIND11_MODULE(beethoven_python, m) {
    m.doc() = "Beethoven FPGA Python bindings - integers only";
    
    pybind11::class_<BeethovenWrapper>(m, "BeethovenWrapper")
        .def(pybind11::init<>())
        .def("malloc", &BeethovenWrapper::malloc, 
             "Allocate memory on FPGA and return memory ID")
        .def("get_host_ptr", &BeethovenWrapper::get_host_ptr,
             "Get host pointer address for memory ID")
        .def("write_int_array", &BeethovenWrapper::write_int_array,
             "Write integer array to memory")
        .def("read_int_array", &BeethovenWrapper::read_int_array,
             "Read integer array from memory")
        .def("copy_to_fpga", &BeethovenWrapper::copy_to_fpga,
             "Copy data from host to FPGA")
        .def("copy_from_fpga", &BeethovenWrapper::copy_from_fpga,
             "Copy data from FPGA to host")
        .def("vector_add", &BeethovenWrapper::vector_add,
             "Perform vector addition on FPGA")
        .def("free_memory", &BeethovenWrapper::free_memory,
             "Free allocated memory")
        .def("get_memory_count", &BeethovenWrapper::get_memory_count,
             "Get number of allocated memories");
}
