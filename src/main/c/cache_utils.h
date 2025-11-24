#ifndef BEETHOVEN_CACHE_UTILS_H
#define BEETHOVEN_CACHE_UTILS_H

#include <iostream>
#include <cstdint>

// Control whether manual cache flush/invalidate is needed
// Set to 1 for AUP-ZU3 platform, 0 for Kria or platforms with hardware cache coherency
#ifndef ENABLE_MANUAL_CACHE_FLUSH
#define ENABLE_MANUAL_CACHE_FLUSH 0
#endif

namespace beethoven {

// ARM64 cache line size (64 bytes is standard for ARM Cortex-A processors)
constexpr size_t CACHE_LINE_SIZE = 64;

/**
 * Flush CPU cache to RAM for a memory region
 * Call this BEFORE the FPGA reads data written by the CPU
 *
 * @param addr Pointer to the start of the memory region
 * @param size Size of the memory region in bytes
 * @param verbose Print debug messages if true
 */
inline void cache_flush_to_ram(void* addr, size_t size, bool verbose = false) {
#if ENABLE_MANUAL_CACHE_FLUSH
    if (verbose) {
        std::cout << "[CACHE] Flushing CPU cache to RAM..." << std::endl;
        std::cout << "  Address: " << addr << std::endl;
        std::cout << "  Size: " << size << " bytes" << std::endl;
    }

    __sync_synchronize();  // Memory barrier

    // Flush every cache line in the region
    char* ptr = (char*)addr;
    for (size_t i = 0; i < size; i += CACHE_LINE_SIZE) {
        asm volatile("dc civac, %0" : : "r"(ptr + i) : "memory");
    }

    __sync_synchronize();  // Memory barrier

    if (verbose) {
        std::cout << "[CACHE] Cache flushed successfully" << std::endl;
    }
#else
    (void)addr;    // Suppress unused parameter warnings
    (void)size;
    (void)verbose;

    if (verbose) {
        std::cout << "[CACHE] Manual cache flush disabled (ENABLE_MANUAL_CACHE_FLUSH=0)" << std::endl;
    }
#endif
}

/**
 * Invalidate CPU cache for a memory region
 * Call this AFTER the FPGA writes data to force CPU to re-read from RAM
 *
 * @param addr Pointer to the start of the memory region
 * @param size Size of the memory region in bytes
 * @param verbose Print debug messages if true
 */
inline void cache_invalidate_from_ram(void* addr, size_t size, bool verbose = false) {
#if ENABLE_MANUAL_CACHE_FLUSH
    if (verbose) {
        std::cout << "[CACHE] Invalidating CPU cache for memory region..." << std::endl;
        std::cout << "  Address: " << addr << std::endl;
        std::cout << "  Size: " << size << " bytes" << std::endl;
    }

    __sync_synchronize();  // Memory barrier

    // Invalidate every cache line in the region
    char* ptr = (char*)addr;
    for (size_t i = 0; i < size; i += CACHE_LINE_SIZE) {
        asm volatile("dc civac, %0" : : "r"(ptr + i) : "memory");
    }

    __sync_synchronize();  // Memory barrier

    if (verbose) {
        std::cout << "[CACHE] Cache invalidated successfully" << std::endl;
    }
#else
    (void)addr;    // Suppress unused parameter warnings
    (void)size;
    (void)verbose;

    if (verbose) {
        std::cout << "[CACHE] Manual cache invalidate disabled (ENABLE_MANUAL_CACHE_FLUSH=0)" << std::endl;
    }
#endif
}

/**
 * Convenience function to flush cache for multiple buffers at once
 *
 * Example usage:
 *   cache_flush_buffers({
 *       {vec_a.getHostAddr(), vec_a_size},
 *       {vec_b.getHostAddr(), vec_b_size}
 *   });
 */
inline void cache_flush_buffers(std::initializer_list<std::pair<void*, size_t>> buffers, bool verbose = false) {
    for (const auto& [addr, size] : buffers) {
        cache_flush_to_ram(addr, size, verbose);
    }
}

/**
 * Convenience function to invalidate cache for multiple buffers at once
 */
inline void cache_invalidate_buffers(std::initializer_list<std::pair<void*, size_t>> buffers, bool verbose = false) {
    for (const auto& [addr, size] : buffers) {
        cache_invalidate_from_ram(addr, size, verbose);
    }
}

} // namespace beethoven

#endif // BEETHOVEN_CACHE_UTILS_H
