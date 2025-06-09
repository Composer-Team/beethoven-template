import chisel3._
import chisel3.util._
import beethoven._
import beethoven.common._
import chipsalliance.rocketchip.config.Parameters

//noinspection TypeAnnotation,ScalaWeakerAccess
class VectorMatMulCore(N: Int)(implicit p: Parameters) extends AcceleratorCore {
  val my_io = BeethovenIO(new AccelCommand("vector_mat_mul") {
    val vec_in_addr = Address()
    val mat_in_addr = Address()
    val vec_out_addr = Address()
    val cols = UInt(32.W) // number of cols in matrix
    val vector_length = UInt(32.W) //<= N
  }, EmptyAccelResponse())

  // Reader to stream in matrix
  val mat_reader = getReaderModule("mat_in")
  // Scratchpad for vector
  val vec_sp = getScratchpad("vec_in")
  //Writer to stream out output vector
  val out_writer = getWriterModule("vec_out")


  val s_idle :: s_prefill :: s_compute ::  s_done :: Nil = Enum(4)
  val state = RegInit(s_idle)

  //init scratchpad 
  vec_sp.requestChannel.init.valid := (state === s_prefill)
  vec_sp.requestChannel.init.bits.memAddr := my_io.req.bits.vec_in_addr
  vec_sp.requestChannel.init.bits.scAddr := 0.U
  vec_sp.requestChannel.init.bits.len := my_io.req.bits.vector_length

  val vec_length_bytes = my_io.req.bits.vector_length * 4.U
  val col_index = RegInit(0.U(32.W))


  // DUT
  val dut = Module(new VectorDot())
  dut.io.length := my_io.req.bits.vector_length

  //Default
  my_io.req.ready := false.B
  my_io.resp.valid := false.B

  vec_sp.dataChannels(0).req.valid := (state === s_compute)
  vec_sp.dataChannels(0).req.bits.addr := 0.U //read from fixed sp addr
  vec_sp.dataChannels(0).req.bits.data := DontCare
  vec_sp.dataChannels(0).req.bits.write_enable := false.B

  mat_reader.requestChannel.valid := (state === s_compute)
  mat_reader.requestChannel.bits.addr := my_io.req.bits.mat_in_addr + (col_index * vec_length_bytes) //matrix in column major
  mat_reader.requestChannel.bits.len := vec_length_bytes

  out_writer.requestChannel.valid := (state === s_compute)
  out_writer.requestChannel.bits.addr := my_io.req.bits.vec_out_addr
  out_writer.requestChannel.bits.len := vec_length_bytes

  // mat_reader.dataChannel.data.ready := false.B
  // out_writer.dataChannel.data.valid := false.B
  // out_writer.dataChannel.data.bits := DontCare

  dut.io.vec_a <> mat_reader.dataChannel.data  //mat column from mem
  dut.io.vec_out <> out_writer.dataChannel.data 
 
  dut.io.vec_b.valid := vec_sp.dataChannels(0).res.valid  //no ready signal
  dut.io.vec_b.bits := vec_sp.dataChannels(0).res.bits  //vector row from sp

  // state machine

  switch(state) {
    is(s_idle) {
      my_io.req.ready := mat_reader.requestChannel.ready && vec_sp.dataChannels(0).req.ready && out_writer.dataChannel.data.ready
      // .fire is a Chisel-ism for "ready && valid"
      when(my_io.req.fire) {
        state := s_prefill
      }
    }

    is(s_prefill) {
      when(vec_sp.requestChannel.init.ready) {
        state := s_compute
      }
    }

    is(s_compute) {
      when(col_index === my_io.req.bits.cols - 1.U) {
          state := s_done
        }.otherwise {
          col_index := col_index + 1.U
        }
    }

    is(s_done) {
      my_io.resp.valid := true.B
      when(my_io.resp.fire) {
        state := s_idle
      }
    }
  }

}