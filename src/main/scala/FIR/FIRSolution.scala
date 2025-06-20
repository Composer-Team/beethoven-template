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

class FirFilterSolution(window: Int)(implicit p: Parameters) extends AcceleratorCore {
    CppGeneration.addPreprocessorDefinition("ACCEL_WINDOW_SIZE", window)
    val start_cmd = BeethovenIO(new AccelCommand("do_filter") {
        val input_addr = Address()
        val output_addr = Address()
        val n_elems = UInt(32.W)
    }, new EmptyAccelResponse)

    val tap_set = BeethovenIO(new AccelCommand("set_taps") {
        val tap_idx = UInt(log2Up(window).W)
        val tap_value = UInt(32.W)
    })

    val reader_in = getReaderModule("input_stream")
    val writer_out = getWriterModule("output_stream")
    reader_in.requestChannel.valid := false.B
    writer_out.requestChannel.valid := false.B
    reader_in.requestChannel.bits := DontCare
    writer_out.requestChannel.bits := DontCare

    val ele_ctr, eles_expected = Reg(UInt(32.W))
    val s_IDLE :: s_COMPUTING :: s_FINISH :: Nil = Enum(3)
    val state = RegInit(s_IDLE)
    start_cmd.req.ready := false.B
    when (state === s_IDLE) {
        start_cmd.req.ready := reader_in.requestChannel.ready && writer_out.requestChannel.ready
        when (start_cmd.req.fire) {
            reader_in.requestChannel.bits.addr := start_cmd.req.bits.input_addr
            writer_out.requestChannel.bits.addr := start_cmd.req.bits.output_addr
            reader_in.requestChannel.bits.len := start_cmd.req.bits.n_elems * 4.U
            writer_out.requestChannel.bits.len := start_cmd.req.bits.n_elems * 4.U
            reader_in.requestChannel.valid := true.B
            writer_out.requestChannel.valid := true.B
            ele_ctr := 0.U
            eles_expected := start_cmd.req.bits.n_elems
            state := s_COMPUTING
        }
    }.elsewhen(state === s_COMPUTING) {
        when (ele_ctr === eles_expected) {
            state := s_FINISH
        }

    }.elsewhen(state === s_FINISH) {
        start_cmd.resp.valid := true.B
        when (start_cmd.resp.fire) {
            state := s_IDLE
        }
    }
    val taps = Reg(Vec(window, UInt(32.W)))
    tap_set.req.ready := true.B
    when (tap_set.req.fire) {
        taps(tap_set.req.bits.tap_idx) := tap_set.req.bits.tap_value
    }
    val window_reg = Reg(Vec(window-1, UInt(32.W)))
    val full_window = Seq(reader_in.dataChannel.data.bits) ++ window_reg
    when (start_cmd.req.fire) {
        window_reg.foreach(_ := 0.U)
    }
    when (reader_in.dataChannel.data.fire) {
        window_reg(0) := reader_in.dataChannel.data.bits
        for (i <- 1 until window-1) {
            window_reg(i) := window_reg(i-1)
        }
    }

    // Perform the multiplication
    val mults = full_window.zip(taps).map(a => a._1 * a._2)
    
    // Perform the sum
    val sum = VecInit(mults).reduceTree(_ + _)
    writer_out.dataChannel.data.bits := sum
    reader_in.dataChannel.data.ready := writer_out.dataChannel.data.ready
    writer_out.dataChannel.data.valid := reader_in.dataChannel.data.valid
    when (reader_in.dataChannel.data.fire) {
        ele_ctr := ele_ctr + 1.U
    }
}

class FirFilterSolutionConfig(windowSize: Int) extends AcceleratorConfig(
    List(AcceleratorSystemConfig(
        nCores = 3,
        name = "FIR",
        moduleConstructor = ModuleBuilder(p => new FirFilterSOLUTION(windowSize)(p)),
        memoryChannelConfig = List(
            ReadChannelConfig("input_stream", dataBytes = 4),
            WriteChannelConfig("output_stream", dataBytes = 4)
        )), new DMAHelperConfig()
        ))

object FirFilterSolutionBuild extends BeethovenBuild(
    new FirFilterSolutionConfig(16),
    platform = new AWSF2Platform(),
    buildMode =  BuildMode.Simulation
)