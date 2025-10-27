#!/usr/bin/env python3
import sys

try:
    import beethoven_python
    print("✔️ beethoven_python module imported successfully")
except ImportError as e:
    print(f"❌ Failed to import beethoven_python: {e}")
    print("Make sure the .so file is in your current directory or " +
            "PYTHONPATH OR that /usr/local/lib is in LD_LIBRARY_PATH")
    exit(1)

def test_basic_functionality():
    print("\n=== Testing Basic Functionality ===")
    
    # Create the wrapper
    fpga = beethoven_python.BeethovenWrapper()
    print("✔️ BeethovenWrapper created")
    
    # Test memory allocation
    mem_id = fpga.malloc(20)  # Allocate space for 5 integers (4 bytes each)
    print(f"✔️ Memory allocated with ID: {mem_id}")
    
    # Test memory count
    count = fpga.get_memory_count()
    print(f"✔️ Active memory count: {count}")
    
    # Test writing and reading data
    test_data = [1, 2, 3, 4, 5]
    fpga.write_int_array(mem_id, test_data)
    print(f"✔️ Written data: {test_data}")
    
    read_data = fpga.read_int_array(mem_id, len(test_data))
    print(f"✔️ Read data: {read_data}")
    
    if test_data == read_data:
        print("✔️ Data integrity test PASSED")
    else:
        print("❌ Data integrity test FAILED")
    
    # Clean up
    fpga.free_memory(mem_id)
    final_count = fpga.get_memory_count()
    print(f"✔️ Memory freed, final count: {final_count}")

def test_simple_vector_addition():
    print("\n=== Testing Simple Vector Addition (4 elements) ===")
    
    fpga = beethoven_python.BeethovenWrapper()
    
    # Simple test case
    n_eles = 4
    size_of_int = 4
    
    try:
        # Allocate memory
        vec_a_id = fpga.malloc(size_of_int * n_eles)
        vec_b_id = fpga.malloc(size_of_int * n_eles)
        vec_out_id = fpga.malloc(size_of_int * n_eles)
        
        # Simple test data
        vec_a = [1, 2, 3, 4]
        vec_b = [10, 20, 30, 40]
        expected = [11, 22, 33, 44]
        
        print(f"Vector A: {vec_a}")
        print(f"Vector B: {vec_b}")
        print(f"Expected: {expected}")
        
        # Write data to memory
        fpga.write_int_array(vec_a_id, vec_a)
        fpga.write_int_array(vec_b_id, vec_b)
        print("✔️ Data written to host memory")
        
        # Copy to FPGA
        fpga.copy_to_fpga(vec_a_id)
        fpga.copy_to_fpga(vec_b_id)
        print("✔️ Data copied to FPGA")
        
        # Perform vector addition
        success = fpga.vector_add(vec_a_id, vec_b_id, vec_out_id, n_eles)
        
        if success:
            print("✔️ Vector addition completed successfully")
            
            # Copy result back from FPGA
            fpga.copy_from_fpga(vec_out_id)
            results = fpga.read_int_array(vec_out_id, n_eles)
            print(f"Results:  {results}")
            
            if results == expected:
                print("✔️ Simple test PASSED")
            else:
                print("❌ Simple test FAILED")
        else:
            print("❌ Simple test computation failed")
        
        # Clean up
        fpga.free_memory(vec_a_id)
        fpga.free_memory(vec_b_id)
        fpga.free_memory(vec_out_id)
        
    except Exception as e:
        print(f"❌ Simple test failed: {e}")

def test_vector_addition_full():
    print("\n=== Testing Vector Addition (32 elements - matching main.cc) ===")
    
    fpga = beethoven_python.BeethovenWrapper()
    
    # Test parameters (matching your main.cc exactly)
    n_eles = 32
    size_of_int = 4
    
    print(f"Number of elements: {n_eles}")
    
    try:
        # Allocate memory
        vec_a_id = fpga.malloc(size_of_int * n_eles)
        vec_b_id = fpga.malloc(size_of_int * n_eles)
        vec_out_id = fpga.malloc(size_of_int * n_eles)
        
        print("✔️ Memory allocated for vector addition")
        
        # Prepare test data (exactly matching your main.cc)
        vec_a = []
        vec_b = []
        for i in range(n_eles):
            vec_a.append(i + 1)    # vec_a_host[i] = i + 1
            vec_b.append(i * 2)    # vec_b_host[i] = i * 2
        
        print(f"Vector A (first 5): {vec_a[:5]}...")
        print(f"Vector B (first 5): {vec_b[:5]}...")
        
        # Calculate expected results
        expected = []
        for i in range(n_eles):
            expected.append(vec_a[i] + vec_b[i])
        
        print(f"Expected (first 5): {expected[:5]}...")
        
        # Write data to memory
        fpga.write_int_array(vec_a_id, vec_a)
        fpga.write_int_array(vec_b_id, vec_b)
        print("✔️ Data written to host memory")
        
        # Copy to FPGA
        fpga.copy_to_fpga(vec_a_id)
        fpga.copy_to_fpga(vec_b_id)
        print("✔️ Data copied to FPGA")
        
        # Perform vector addition
        success = fpga.vector_add(vec_a_id, vec_b_id, vec_out_id, n_eles)
        
        if success:
            print("✔️ Vector addition completed successfully")
            
            # Copy result back from FPGA
            fpga.copy_from_fpga(vec_out_id)
            results = fpga.read_int_array(vec_out_id, n_eles)
            
            print(f"Results (first 10): {results[:10]}...")
            
            # Verify results
            if results == expected:
                print("✔️ Vector addition verification PASSED")
            else:
                print("❌ Vector addition verification FAILED")
                # Show first few mismatches
                mismatch_count = 0
                for i in range(n_eles):
                    if results[i] != expected[i]:
                        print(f"  Mismatch at index {i}: got {results[i]}, expected {expected[i]}")
                        mismatch_count += 1
                        if mismatch_count >= 5:  # Only show first 5 mismatches
                            break
                
        else:
            print("❌ Vector addition failed")
        
        # Clean up
        fpga.free_memory(vec_a_id)
        fpga.free_memory(vec_b_id)
        fpga.free_memory(vec_out_id)
        print("✔️ Memory cleaned up")
        
    except Exception as e:
        print(f"❌ Vector addition test failed with error: {e}")

def main(test:int):
    print("=== Beethoven Python Binding Test (Integer Vector Addition) ===")
    print("This script tests the Python bindings for Beethoven's FPGA vector addition.")

    if test == 1:
        print("Running basic functionality tests...")
        test_basic_functionality()
    elif test == 2:
        print("Running simple vector addition tests...")
        test_simple_vector_addition()
    elif test == 3:
        print("Running full vector addition tests...")
        test_vector_addition_full()
    
    
    print("\n=== Test Complete ===")

if __name__ == "__main__":
    test = sys.argv[1] if len(sys.argv) > 1 else "1"
    main(int(test))
