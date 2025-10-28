package systolic.chisel
import beethoven._
import chisel3._
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform
import systolic.Constants._
import beethoven.Generation.CppGeneration

class SystolicArrayChiselConfig_SOLUTION(nCores: Int)
    extends AcceleratorConfig(
      AcceleratorSystemConfig(
        nCores = nCores,
        name = "SystolicArrayCore",
        moduleConstructor =
          new ModuleBuilder(p => new SystolicArrayCore_SOLUTION(systolic_array_dim)(p)),
        memoryChannelConfig = List(
          ReadChannelConfig(
            "weights",
            dataBytes = data_width_bytes * systolic_array_dim
          ),
          ReadChannelConfig(
            "activations",
            dataBytes = data_width_bytes * systolic_array_dim
          ),
          WriteChannelConfig(
            "vec_out",
            dataBytes = data_width_bytes * systolic_array_dim
          )
        )
      )
    )

object SystolicArrayConfig_SOLUTION
    extends BeethovenBuild(
      new SystolicArrayChiselConfig_SOLUTION(1),
      platform = new AWSF2Platform,
      buildMode = BuildMode.Simulation
    )
