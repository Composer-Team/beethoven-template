import chipsalliance.rocketchip.config.Config
import beethoven._
class MyAcceleratorConfig extends Config ((site, _, up) => {
  case AcceleratorSystems => up(AcceleratorSystems, site) ++
    Seq(AcceleratorSystemConfig(
      nCores = 1,
      name = "MyAccelerator",
      moduleConstructor = ModuleBuilder(p => new MyAccelerator()(p))
    ))
})

class MyAcceleratorKria extends Config(new MyAcceleratorConfig ++
  new WithBeethoven(platform=KriaPlatform()))

object MyAcceleratorKria extends BeethovenBuild(new MyAcceleratorKria)