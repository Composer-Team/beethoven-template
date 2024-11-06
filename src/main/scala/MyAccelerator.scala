import chisel3._
import beethoven._
import beethoven.common._
import chipsalliance.rocketchip.config._

class MyAccelerator(implicit p: Parameters) extends AcceleratorCore {
  val io = BeethovenIO(new AccelCommand("my_accel") {
    val a: UInt = UInt(32.W)
    val b: UInt = UInt(32.W)
  }, new AccelResponse("my_accel_resp") {
    val result: UInt = UInt(32.W)
  })

  io.req.ready := io.resp.ready
  io.resp.valid := io.req.valid
  io.resp.bits.result := io.req.bits.a + io.req.bits.b

  val q = getReaderModule("a")
  q.requestChannel.valid := false.B
  q.requestChannel.bits := DontCare

  q.dataChannel.data.ready := false.B
}