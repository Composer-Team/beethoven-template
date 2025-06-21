package VectorAdd


import beethoven._
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform
import beethoven.Platforms.FPGA.Xilinx.AWS.DMAHelperConfig
import beethoven.Platforms.FPGA.Xilinx.AWS.MemsetHelperConfig

class VectorAddConfig extends AcceleratorConfig(
  List(AcceleratorSystemConfig(
    nCores = 3,
    name = "myVectorAdd",
    moduleConstructor = ModuleBuilder(p => new VectorAddCore()(p)),
    memoryChannelConfig = List(
      ReadChannelConfig("vec_a", dataBytes = 4),
      ReadChannelConfig("vec_b", dataBytes = 4),
      WriteChannelConfig("vec_out", dataBytes = 4)
    )
  ),

  //////////////////////////////
  // DO NOT REMOVE OR CHANGE THESE
  // During the transition from AWS F1 -> F2 instances, some of the AWS infra
  // is lagging behind, requiring these work-arounds. These are not needed for
  // simulation, ASIC, or Kria FPGA targets
  new DMAHelperConfig, new MemsetHelperConfig(4)
  //////////////////////////////
  ))

object VectorAddConfig extends BeethovenBuild(new VectorAddConfig,
  buildMode = BuildMode.Simulation,
  platform = new AWSF2Platform("beethoven-user0"))