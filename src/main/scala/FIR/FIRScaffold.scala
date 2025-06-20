package FIR

import beethoven._
import beethoven.common._
import chisel3._
import chipsalliance.rocketchip.config.Parameters
import chisel3.util._
import dataclass.data
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform
import beethoven.Platforms.FPGA.Xilinx.AWS.DMAHelper
import beethoven.Platforms.FPGA.Xilinx.AWS.DMAHelperConfig
import beethoven.Generation.CppGeneration
/*

// FIR Filter:
// The FIR takes in a stream of data (Input) and applies a convolution
// over the input as it streams through.
// 
// The size of the window for this convolution is given given as an input
// to the module. The weights for each element in the window (each tap)
// is set by the host software as a pre-processing step (see T0, T1, etc).
// 
// As the input stream arrives in the module, the window is shifted through
// a shift register.
// 
//         Input --> REG -> ... -> Reg
//           |      |      ...     |
//        T0 *   T1 *           TN *
//           |      |              |
//           |----- + --- +  .. - + -> output
//
// We will implement the following.
// 1. Beethoven IO that will set each one of the taps. This will not return
//    a response to the host.
// 2. Beethoven IO to start reading in a data stream and processing it. We
//    will return a response to the host when we're done processing.
// 3. The core FIR computational logic. We'll use 32-bit UInt for our datatype.
//    On each cycle where our read stream is valid and our write stream is ready,
//    we'll compute the convolution and write it out.

class FirFilterScaffold(window: Int)(implicit p: Parameters) extends AcceleratorCore {
    // Beethoven can export C++ #defines and `consts` to the generated C++ header, allowing
    // us to inform the C++ testbench how the currently build hardware was configured.
    CppGeneration.addPreprocessorDefinition("ACCEL_WINDOW_SIZE", window)
    
    val start_cmd = BeethovenIO(new AccelCommand(???) {
        /* Beethoven interface for start interfaces should go here.
           Provide a sensible name for your interface above.
            We'll need three things: 
            1. Input Pointer
            2. Output Pointer
            3. The length of the vector we'll be streamig in from memory
            */
    }, new EmptyAccelResponse)

    val tap_set = BeethovenIO(new AccelCommand(???) {
        /* Beethoven interface for tap-set should go here.
           Provide a sensible name for your interface above.
            We'll need two things: 
            1. The index of the tap to set
            2. The value of the tap.
            */
    })

    // Get your reader and writer for your module. Choose a name for each one.
    val reader_in = ???
    val writer_out = ???
    // In Chisel, it's standard to provide initially tie-low our interfaces for our default
    // state. We can override them later inside our state machine. You can see how we drive
    // the reader request channel here.
    reader_in.requestChannel.valid := false.B
    reader_in.requestChannel.bits := DontCare

    val ele_ctr, eles_expected = Reg(UInt(32.W))
    // IMPLEMENT THE STATE MACHINE
    // Note: Ready/Valid (i.e., Decoupled) interfaces in Chisel come with a .fire() method.
    //       .fire() is defined as .ready && .valid (a successful handshake)
    // 
    //          move on successful                   move when we've                 reset once we've
    //              handshake                       processed everything           delivered our response
    //                  |                                   |                                |
    // IDLE ---[start_cmd.req.fire()]--> COMPUTING --[ele_ctr==n_elem]--> FINISH --[start_cmd.resp.fire()]--> IDLE
    //  (1)                                (2)                              (3)
    //
    // (1) Sit in the idle state waiting for the start_cmd request interface to go high. Transition to the
    //     COMPUTING state when that happens. Initialize the reader and writer transactions when the request
    //     exchange occurs. We should ensure that we are only ready for a new command when our reader and
    //     writer are ready for new transactions.
    //
    // (2) Complete the FIR filter operation by streaming in the reader data, performing our operation, and
    //     then writing the impulse to the writer. Transition to the FINISH state when we've completed the
    //     stream.
    //
    // (3) Write an acknowledge back to the CPU via the start_cmd.resp() to signal that we've completed.

    // For your convenience, here is the state machine definition 
    val s_IDLE :: s_COMPUTING :: s_FINISH :: Nil = Enum(3)
    val state = RegInit(s_IDLE)
    // We will use these counters to detect the end of the data stream and reset to idle state
    val ele_ctr, eles_expected = Reg(UInt(32.W))

    // by default, we're not ready to accept 
    start_cmd.req.ready := false.B
    when (state === s_IDLE) {
        /////
        // We're only ready to accept a new command when our streams are ready to start
        // new read and write transactions
        ???
        /////

        // When the request handshake occurs...
        when (start_cmd.req.fire) {
            /////
            // Connect the address and length fields of the read/write streams to the start_cmd interface
            // 
            // Mind that the length field of the streams is in # of bytes. If start_cmd takes
            // in a number of elements in the vector, we'll need to multiple this by the data size.
            ???
            /////

            /////
            // launch the transactions: drive valid high for reader and writer request interfaces 
            ???
            /////

            /////
            // Set eles_expected to the length of the vector provided by your start_cmd definition
            ele_ctr := 0.U
            ???
            /////
            state := s_COMPUTING
        }
    }.elsewhen(state === s_COMPUTING) {
        when (ele_ctr === eles_expected) {
            state := s_FINISH
        }

    }.elsewhen(state === s_FINISH) {
        /////
        // When we're in our finish state, drive the response valid and wait for the handshake to
        // proceed before we go back to idle state
        ???
        when (???) {
            state := s_IDLE
        }
    }    

    /////////////////////////////////////////
    // SET UP TAPS
    //         Input --> REG -> ... -> Reg
    //           |      |      ...     |
    //      T0-  *   T1 *           TN *
    //      ^    |      |              |
    //      |    |----- + --- +  .. - + -> output
    //      |
    //    SET TAPS USING BEETHOVEN YOUR tap_set CMD
    ////////////////////////////////////////

    val taps = Reg(Vec(window, UInt(32.W)))
    tap_set.req.ready := true.B
    // When tap set command handshake occurs, set the selected tap to the specified value.
    when (???) {
        ???
    }
    ////////////////////////////////////////
    // Maintain window (remember to reset registers to 0 when new start cmd)
    //          |
    //          \/
    // Input -> Reg -> Reg -> ... -> Reg
    ////////////////////////////////////////
    val window_reg = Reg(Vec(window-1, UInt(32.W)))
    val full_window = Seq(reader_in.dataChannel.data.bits) ++ window_reg
    
    // When a start_cmd fires, reset the window to 0
    when (start_cmd.req.fire) {
        window_reg.foreach(_ := 0.U)
    }

    //////
    // When the reader data channel fires
    when (???) {
        // 1. Set the first element in the shift register to the value in the reader data stream
        window_reg(0) := ???
        // 2. Shift the register
        for (i <- 1 until window-1) {
            ???
        }
    }

    ///// This is not a Chisel tutorial, so we'll do the computation for you.
    // Perform the multiplication
    val mults = full_window.
                    zip(taps).
                    map(a => a._1 * a._2)
    // Perform the sum using a reduction tree.
    val sum: UInt = VecInit(mults).reduceTree(_ + _)

    // another option is the following:
    // val sum = full_window.reduce(_ + _)
    // which uses Scala's Iterable type .reduce() method
    // The downside is that the elaborated hardware
    // has depth LINEAR in the size of the window.

    /////
    // Write the sum out on the writer data channel
    ???
    /////

    /////
    // Time to drive the ready signal for the reader data stream and valid signal
    // for the writer data stream.
    //  
    // We want the writer and reader handshakes to happen on the same cycle:
    // 1. Writer provides VALID data when reader data is VALID
    // 2. Reader is READY to consume data when Writer is READY to accept data
    
    // Set reader_in data ready
    ???
    // Set writer_out data valid
    ???
    //
    /////

    // Increment the ele_ctr whenever we consume another item on the reader channel.
    when (reader_in.dataChannel.data.fire) {
        ele_ctr := ele_ctr + 1.U
    }
}

class FirFilterScaffoldConfig(windowSize: Int) extends AcceleratorConfig(
    List(AcceleratorSystemConfig(
        nCores = ???,
        name = ???,
        moduleConstructor = ModuleBuilder(p => new FirFilterScaffold(windowSize)(p)),
        memoryChannelConfig = List(
            ReadChannelConfig(name = ???, dataBytes = ???),
            WriteChannelConfig(name = ???, dataBytes = ???)
        )), 
        // DO NOT REMOVE THIS! This is necessary while AWS works on restoring DMA driver functionality :(
        new DMAHelperConfig()
        ))

object FirFilterScaffoldBuild extends BeethovenBuild(
    new FirFilterScaffoldConfig(???),
    platform = new AWSF2Platform(),
    // When it's time to deploy our accelerator, set this to BuildMode.Synthesis
    buildMode =  BuildMode.Simulation
)
*/