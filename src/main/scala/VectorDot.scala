import chisel3._
import chisel3.util._

import beethoven._
import beethoven.MemoryStreams._

class VectorDot(val dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val vec_a = Flipped(Decoupled(UInt(dataWidth.W)))
    val vec_b = Flipped(Decoupled(UInt(dataWidth.W)))
    val vec_out = Decoupled(UInt((2 * dataWidth).W))
  })

  val acc = RegInit(0.U((2 * dataWidth).W))
  // val done = RegInit(false.B)

  val can_consume = io.vec_b.valid && io.vec_a.valid //&& io.vec_out.ready

  io.vec_a.ready := can_consume
  io.vec_b.ready := true.B
  io.vec_out.valid := false.B
  io.vec_out.bits := acc

  when (can_consume) {
    acc := acc + (io.vec_a.bits * io.vec_b.bits)
  }

  when (io.vec_out.ready) {
    io.vec_out.valid := true.B
    acc := 0.U
  }
}