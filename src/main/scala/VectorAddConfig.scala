import beethoven.Platforms.FPGA.Xilinx.F2.AWSF2Platform
import beethoven._

class VectorAddConfig extends AcceleratorConfig(
  AcceleratorSystemConfig(
    nCores = 1,
    name = "myVectorAdd",
    moduleConstructor = ModuleBuilder(p => new VectorAddCore()(p)),
    memoryChannelConfig = List(
      ReadChannelConfig("vec_a", dataBytes = 4),
      ReadChannelConfig("vec_b", dataBytes = 4),
      WriteChannelConfig("vec_out", dataBytes = 4)
    )
  )
)

object VectorAddConfig extends BeethovenBuild(new VectorAddConfig,
  buildMode = BuildMode.Simulation,
  platform = new AWSF2Platform)