import beethoven._

class MyAcceleratorConfig extends AcceleratorConfig(
  AcceleratorSystemConfig(
    nCores = 1,
    name = "MyAccelerator",
    moduleConstructor = ModuleBuilder(p => new MyAccelerator()(p)),
    memoryChannelConfig = List(
      ReadChannelConfig("a", 4))
  ))

object MyAcceleratorKria extends BeethovenBuild(new MyAcceleratorConfig,
  buildMode = BuildMode.Simulation,
  platform = KriaPlatform())