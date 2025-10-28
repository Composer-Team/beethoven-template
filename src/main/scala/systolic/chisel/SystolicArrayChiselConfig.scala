package systolic.chisel
import beethoven._
import chisel3._
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform
import systolic.Constants._
import beethoven.Generation.CppGeneration

// Usually we would use `Address` inside the core definition, but since
// we are also providing a Verilog-core implementation then we need to
// provide fixed-widths 
class SystolicArrayCmd extends AccelCommand("matmul") {
  val wgt_addr = UInt(64.W)
  val act_addr = UInt(64.W)
  val out_addr = UInt(64.W)
  val inner_dimension = UInt(20.W)
}

class SystolicArrayChiselConfig(nCores: Int)
    extends AcceleratorConfig(
      AcceleratorSystemConfig(
        nCores = nCores,
        name = "SystolicArrayCore",
        moduleConstructor = new ModuleBuilder(p => new SystolicArrayCore(systolic_array_dim)(p)),
        memoryChannelConfig = List( /* TODO */ )
      )
    )

object SystolicArrayConfig
    extends BeethovenBuild(
      new SystolicArrayChiselConfig(1),
      platform = new AWSF2Platform,
      buildMode = BuildMode.Simulation
    )
