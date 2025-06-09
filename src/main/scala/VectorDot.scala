import chisel3._
import chisel3.util._

import beethoven._
import beethoven.MemoryStreams._

class VectorDot(val dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    val vec_a = Flipped(Decoupled(UInt(dataWidth.W)))
    val vec_b = Flipped(Decoupled(UInt(dataWidth.W)))
    val vec_out = Decoupled(UInt((2 * dataWidth).W))
    val length = Input(UInt(32.W)) // number of elements in vectors
  })

  val count = RegInit(0.U(32.W))
  val acc = RegInit(0.U((2 * dataWidth).W))
  val done = RegInit(false.B)

  val can_consume = io.vec_a.valid && io.vec_b.valid && !done

  io.vec_a.ready := can_consume
  io.vec_b.ready := can_consume
  io.vec_out.valid := done
  io.vec_out.bits := acc

  when (can_consume) {
    acc := acc + (io.vec_a.bits * io.vec_b.bits)
    count := count + 1.U
    when (count === io.length - 1.U) {
      done := true.B
    }
  }

  when (io.vec_out.fire) {
    acc := 0.U
    count := 0.U
    done := false.B
  }
}