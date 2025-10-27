package systolic.verilog
import beethoven._
import chisel3._
import beethoven.Platforms.FPGA.Xilinx.AWS.AWSF2Platform
import systolic._
import systolic.Constants.systolic_array_dim
import systolic.Constants.data_width_bits
import systolic.Constants.int_bits
import systolic.Constants.frac_bits
import beethoven.Generation.CppGeneration

class SystolicArrayConfig_SOLUTION(nCores: Int)
    extends AcceleratorConfig(
      AcceleratorSystemConfig(
        nCores = nCores,
        name = "SystolicArrayCore",
        moduleConstructor = new BlackboxBuilderCustom(
          Seq(
            BeethovenIOInterface(
              new SystolicArrayCmd,
              EmptyAccelResponse()
            )
          ),
          // This is where Beethoven will generate the AcceleratorCore
          // definition if something is not already there with the correct
          // name (it will be <SystemName>.v)
          sourcePath = os.pwd / "src" / "main" / "verilog" / "systolic",
          // if the AcceleratorCore has verilog dependencies, they must all
          // be elaborated here. If you're fancy, os.walk can scrape directories
          externalDependencies = {
            val src_dir = os.pwd / "src" / "main" / "verilog" / "systolic"
            Some(
              Seq(
                src_dir / "ProcessingElement.v",
                src_dir / "ShiftReg.v",
                src_dir / "SystolicArray.v"
              )
            )
          },
          // We can pass parameters to the Verilog module elaboration like this
          verilogMacroParams = Map(
            "SYSTOLIC_ARRAY_DIM" -> systolic_array_dim,
            "DATA_WIDTH_BITS" -> data_width_bits,
            "INT_BITS" -> int_bits,
            "FRAC_BITS" -> frac_bits
          )
        ),
        memoryChannelConfig = List(
          ReadChannelConfig(
            "weights",
            dataBytes = Constants.data_width_bytes * Constants.systolic_array_dim
          ),
          ReadChannelConfig(
            "activations",
            dataBytes = Constants.data_width_bytes * Constants.systolic_array_dim
          ),
          WriteChannelConfig(
            "vec_out",
            dataBytes = Constants.data_width_bytes * Constants.systolic_array_dim
          )
        )
      )
    )

// The main method that will drive the elaboration of our accelerator
object SystolicArrayConfig_SOLUTION
    extends BeethovenBuild(
      {
        // usually you would just provide the config here, but because we want to generate
        // some C++ #define statements with our accelerator configuration details, we put
        // these statements here. This statement can be executed at any time but here is a
        // decent place to put it. Typically, I put these statements inside the Chisel Core
        // definition which is not applicable for Verilog
        CppGeneration.addPreprocessorDefinition(
          Seq(
            ("DATA_WIDTH_BYTES", Constants.data_width_bytes),
            ("FRAC_BITS", frac_bits),
            ("INT_BITS", int_bits),
            ("SYSTOLIC_ARRAY_DIM", systolic_array_dim)
          )
        )

        new SystolicArrayConfig(1)
      },
      platform = new AWSF2Platform,
      buildMode = BuildMode.Simulation
    )
