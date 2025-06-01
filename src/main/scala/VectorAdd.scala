import chisel3._
import chisel3.util._

import beethoven._
import beethoven.MemoryStreams._

//noinspection TypeAnnotation, ScalaWeakerAccess
class VectorAdd extends Module {
  val io = IO(new Bundle {
    val vec_a = Flipped(Decoupled(UInt(32.W)))
    val vec_b = Flipped(Decoupled(UInt(32.W)))
    val vec_out = Decoupled(UInt(32.W))
  })
  // only consume an element when everyone's ready to move
  val can_consume = io.vec_a.valid && io.vec_b.valid && io.vec_out.ready
  io.vec_out.valid := can_consume
  io.vec_a.ready := can_consume
  io.vec_b.ready := can_consume
  io.vec_out.bits := io.vec_a.bits + io.vec_b.bits
}