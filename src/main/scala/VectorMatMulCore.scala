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


  val s_idle :: s_prefill :: s_ready :: s_compute ::  s_done :: Nil = Enum(5)
  val state = RegInit(s_idle)


  val vec_length_bytes = my_io.req.bits.vector_length * 4.U
  val row_index = RegInit(0.U(32.W))
  val col_index = RegInit(0.U(32.W))


  //init scratchpad 
  vec_sp.requestChannel.init.valid := false.B
  vec_sp.requestChannel.init.bits.memAddr := my_io.req.bits.vec_in_addr
  vec_sp.requestChannel.init.bits.scAddr := 0.U
  vec_sp.requestChannel.init.bits.len := vec_length_bytes


  // DUT
  val dut = Module(new VectorDot())
  dut.io.vec_out.ready := false.B


  //Default
  my_io.req.ready := false.B
  my_io.resp.valid := false.B

  vec_sp.dataChannels(0).req.valid := false.B
  vec_sp.dataChannels(0).req.bits.addr := row_index //read from fixed sp addr
  vec_sp.dataChannels(0).req.bits.data := DontCare
  vec_sp.dataChannels(0).req.bits.write_enable := false.B

  mat_reader.requestChannel.valid := false.B
  mat_reader.requestChannel.bits.addr := my_io.req.bits.mat_in_addr + (col_index * vec_length_bytes) + row_index //matrix in column major
  mat_reader.requestChannel.bits.len := vec_length_bytes

  out_writer.requestChannel.valid := false.B
  out_writer.requestChannel.bits.addr := my_io.req.bits.vec_out_addr + col_index * 8.U
  out_writer.requestChannel.bits.len := my_io.req.bits.cols * 8.U

  dut.io.vec_a <> mat_reader.dataChannel.data  //mat column from mem
   
  dut.io.vec_b.valid := false.B
  dut.io.vec_b.bits := vec_sp.dataChannels(0).res.bits  //vector row from sp

  out_writer.dataChannel.data.bits := dut.io.vec_out.bits
  out_writer.dataChannel.data.valid := dut.io.vec_out.valid

  // state machine
  switch(state) {
    is(s_idle) {
      my_io.req.ready := mat_reader.requestChannel.ready && vec_sp.dataChannels(0).req.ready && out_writer.requestChannel.ready
      // .fire is a Chisel-ism for "ready && valid"
      when(my_io.req.fire) {
        out_writer.requestChannel.valid := true.B
        vec_sp.requestChannel.init.valid := true.B
        state := s_prefill
      }
    }

    is(s_prefill) {
      when(vec_sp.requestChannel.init.ready) {
        state := s_ready
      }
    }

    is(s_ready) {
      vec_sp.dataChannels(0).req.valid := true.B
      state := s_compute
    }

    is(s_compute) {
      vec_sp.dataChannels(0).req.valid := true.B
      mat_reader.requestChannel.valid := true.B
      when(mat_reader.dataChannel.data.valid){
        dut.io.vec_b.valid := true.B
        when (row_index === my_io.req.bits.vector_length - 1.U) {
          dut.io.vec_out.ready := true.B 
          row_index := 0.U

          when(col_index === my_io.req.bits.cols - 1.U) {
            state := s_done
            col_index := 0.U
          }.otherwise {
            state := s_ready
            col_index := col_index + 1.U
          }
        }.otherwise{
          state := s_ready
          row_index := row_index + 1.U
        }
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