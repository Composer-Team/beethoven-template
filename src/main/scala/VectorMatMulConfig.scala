import beethoven._
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform

class VectorMatMulConfig(N: Int) extends AcceleratorConfig(
  AcceleratorSystemConfig(
    nCores = 1,
    name = "myVectorMatMul",
    moduleConstructor = ModuleBuilder(p => new VectorMatMulCore(N)(p)),
    memoryChannelConfig = List(
      ScratchpadConfig("vec_in", dataWidthBits = 32,
                            nDatas = N,
                            nPorts = 1,
                            latency = 1,
                            features = ScratchpadFeatures()),
      ReadChannelConfig("mat_in", dataBytes = 4),
      WriteChannelConfig("vec_out", dataBytes = 8)
    )
  )
)

//N_max = 128
object VectorMatMulConfig extends BeethovenBuild(new VectorMatMulConfig(128), 
  buildMode = BuildMode.Simulation,
  platform = new AWSF2Platform)