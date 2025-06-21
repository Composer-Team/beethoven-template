import beethoven._
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform

class VectorMatMulConfig(N: Int, dataWidth: Int) extends AcceleratorConfig(
  AcceleratorSystemConfig(
    nCores = 1,
    name = "myVectorMatMul",
    moduleConstructor = ModuleBuilder(p => new VectorMatMulCore(N, dataWidth)(p)),
    memoryChannelConfig = List(
      ScratchpadConfig("vec_in", dataWidthBits = dataWidth,
                            nDatas = N,
                            nPorts = 1,
                            latency = 1,
                            features = ScratchpadFeatures()),
      ReadChannelConfig("mat_in", dataBytes = dataWidth / 8),
      WriteChannelConfig("vec_out", dataBytes = 2 * dataWidth / 8)
    )
  )
)

//N_max = 128
object VectorMatMulConfig extends BeethovenBuild(new VectorMatMulConfig(128, 16), 
  buildMode = BuildMode.Simulation,
  platform = new AWSF2Platform)