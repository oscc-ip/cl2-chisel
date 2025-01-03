package cl2

import chisel3._
import chisel3.util._

class Difftest extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val isbreak = Input(Bool())
    val ok      = Input(Bool())
    val wbValue = Input(UInt(32.W))
    val wen     = Input(Bool())
    val rd      = Input(UInt(5.W))
    val clock   = Input(Clock())
    val pc      = Input(UInt(32.W))
  })
  setInline(
    "difftest.sv",
    """
  module Difftest(
    input logic isbreak,
    input logic ok,
    input logic [31:0] wbValue,
    input logic wen,
    input logic [4:0] rd,
    input logic [31:0] pc,
    input logic clock
  );
    import "DPI-C" function void sim_stop(input int unsigned pc);
    import "DPI-C" function void ctx_update(input int unsigned pc, input int unsigned wen, input int unsigned rd, input int unsigned v);

    always_comb begin
      if(isbreak)
        sim_stop(pc);
    end
    always_ff @(posedge clock) begin
      if(ok)
        ctx_update(pc, wen, rd, wbValue);
    end
  endmodule
  """.stripMargin
  )
}
