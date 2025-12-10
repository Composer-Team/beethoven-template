package systolic.chisel
import beethoven._
import beethoven.common._
import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import systolic.Constants.data_width_bytes
import beethoven.Generation.CppGeneration
import systolic.Constants._

class SystolicArrayCore(dim: Int)(implicit p: Parameters) extends AcceleratorCore {
  val io = BeethovenIO(new SystolicArrayCmd(), EmptyAccelResponse())
  // These will fail unless weights, activations, and vec_out are declared in the configuration
  val ReaderModuleChannel(weights_req, weights) = getReaderModule("weights")
  val ReaderModuleChannel(activations_req, activations) = getReaderModule("activations")
  val WriterModuleChannel(output_req, output) = getWriterModule("vec_out")
  // In addition to the BeethovenIO generating a C++ stub, we can export various hardware
  // constants to the C++ environments here.
  // .addPreprocessorDefinition produces #define statements, and
  // .addUserCppDefinition produces const <data_type> <name> = ??? statements
  CppGeneration.addPreprocessorDefinition(
    Seq(
      ("DATA_WIDTH_BYTES", data_width_bytes),
      ("FRAC_BITS", frac_bits),
      ("INT_BITS", int_bits),
      ("SYSTOLIC_ARRAY_DIM", systolic_array_dim)
    )
  )

  val cmd_fire = io.req.fire

  output_req.bits.len /* := ??? */
  weights_req.bits.len /* := ??? */
  activations_req.bits.len /* := ??? */

  weights_req.bits.addr := Address(io.req.bits.wgt_addr)
  activations_req.bits.addr /* := ??? */
  output_req.bits.addr /* := ??? */

  val s_idle :: s_go :: s_flush :: s_response :: Nil = Enum(4)
  val state = RegInit(s_idle)

  // request should be idle when state is idle and memory streams are ready for a request
  io.req.ready /* := ??? */
  io.resp.valid /* := ??? */

  val sa = Module(new SystolicArray())
  val sa_idle = sa.io.ctrl_start_ready
  
  sa.io.act_in /* := ??? */
  activations.data.ready /* := ??? */
  sa.io.act_valid /* := ??? */

  sa.io.wgt_in /* := ??? */
  weights.data.ready /* := ??? */
  sa.io.wgt_valid /* := ??? */

  output.data.valid /* := ??? */
  sa.io.accumulator_out_ready /* := ??? */
  output.data.bits /* := ??? */

  sa.io.ctrl_start_matmul /* := ??? */
  sa.io.ctrl_inner_dimension /* := ??? */

  when(state === s_idle) {
    // TODO
  }.elsewhen(state === s_go) {
    // TODO
  }.elsewhen(state === s_flush) {
    // TODO
  }.elsewhen(state === s_response) {
    // TODO
  }
}
