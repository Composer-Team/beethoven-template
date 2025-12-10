
module SystolicArrayCore #(parameter SYSTOLIC_ARRAY_DIM, DATA_WIDTH_BITS, INT_BITS, FRAC_BITS)(
  input clock,
  input areset,

  input          cmd_matmul_valid,
  output         cmd_matmul_ready,
  input  [19:0]  cmd_matmul_inner_dimension,
  input  [63:0]  cmd_matmul_out_addr,
  input  [63:0]  cmd_matmul_act_addr,
  input  [63:0]  cmd_matmul_wgt_addr,
  output         resp_matmul_valid,
  input          resp_matmul_ready,
  output         weights_req_valid,
  input          weights_req_ready,
  output [33:0]  weights_req_len,
  output [63:0]  weights_req_addr_address,
  output         activations_req_valid,
  input          activations_req_ready,
  output [33:0]  activations_req_len,
  output [63:0]  activations_req_addr_address,
  input          weights_inProgress,
  input          activations_inProgress,
  input                                                 weights_data_valid,
  output                                                weights_data_ready,
  input  [(SYSTOLIC_ARRAY_DIM * DATA_WIDTH_BITS - 1):0] weights_data,
  input                                                 activations_data_valid,
  output                                                activations_data_ready,
  input  [(SYSTOLIC_ARRAY_DIM * DATA_WIDTH_BITS - 1):0] activations_data,
  output         vec_out_req_valid,
  input          vec_out_req_ready,
  output [33:0]  vec_out_req_len,
  output [63:0]  vec_out_req_addr_address,
  input          vec_out_isFlushed,
  output         vec_out_data_valid,
  input          vec_out_data_ready,
  output [(SYSTOLIC_ARRAY_DIM * DATA_WIDTH_BITS - 1):0] vec_out_data
);

wire cmd_fire = cmd_matmul_valid && cmd_matmul_ready;

assign vec_out_req_valid = ???;
// length of output transaction in bytes
assign vec_out_req_len = ???;
// address of the output matrix
assign vec_out_req_addr_address = ???;

assign weights_req_valid = ???;
assign weights_req_len = ???;
assign weights_req_addr_address = ???;

assign activations_req_valid = ???;
assign activations_req_len = ???;
assign activations_req_addr_address = ???;

// state machine
// IDLE
//    The systolic array is idle and waiting for a command from the host
//
//    Once cmd_fire is high, we launch memory transactions, launch the systolic array,
//    and move to "GO" state
//
// GO
//    The systolic array should now be running on its own, streaming from the memory
//    interfaces when appropriate, and then, once its complete, writing everything out
//    to memory.
//
//    Once the systolic array notifies us that it is once again idle and the write streams
//    have cohered everything to external memory, then we can notify the host that we are done
//    by transition to RESPONSE and driving the response interface
//
// RESPONSE
//    We drive response valid high and transition back to IDLE once this handshake is complete

`define IDLE 0
`define GO 1
`define RESPONSE 2
reg [1:0] state;

assign cmd_matmul_ready = ???;
assign resp_matmul_valid = ???;

wire sa_idle;

SystolicArray #(.DATA_WIDTH_BITS(DATA_WIDTH_BITS), .FRAC_BITS(FRAC_BITS), .INT_BITS(INT_BITS), .SYSTOLIC_ARRAY_DIM(SYSTOLIC_ARRAY_DIM)) sa(
  .clk(clock),
  .rst(areset),

  .act_in(),
  .act_valid(),
  .act_ready(),

  .wgt_in(),
  .wgt_valid(),
  .wgt_ready(),

  .accumulator_out(),
  .accumulator_out_valid(),
  .accumulator_out_ready(),

  .ctrl_start_matmul(),
  .ctrl_start_ready(sa_idle),
  .ctrl_inner_dimension()
);

always @(posedge clock) begin
  if (areset) begin
    state <= `IDLE;
  end else begin
    if (state == `IDLE) begin
    end else if (state == `GO) begin
    end else if (state == `RESPONSE) begin
    end
  end
end

endmodule
