// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class SimpleMemReq(xlen: Int) extends Bundle {
  val addr  = UInt(xlen.W)
  val wen   = Bool()
  val wdata = UInt(xlen.W)
  val mask  = UInt(4.W)
}

class SimpleMemResp(xlen: Int) extends Bundle {
  val err   = UInt(4.W)
  val rdata = UInt(xlen.W)
}

//use DPI-C for test
class cache_helper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(Input(new SimpleMemReq(32))))
    val out   = Decoupled(Output(new SimpleMemResp(32)))
    val reset = Input(Bool())
    val clock = Input(Clock())
  })
  setInline(
    "cache_helper.sv",
    """
  module cache_helper(
	  input  logic in_valid,
	  output logic in_ready,
	  input  logic [31:0] in_bits_addr,
	  input  logic in_bits_wen,
	  input  logic [31:0] in_bits_wdata,
	  input  logic [3:0]  in_bits_mask,
	  output logic out_valid, 
	  input  logic out_ready,
	  output logic [3:0] out_bits_err,
	  output logic [31:0] out_bits_rdata,
	  input  logic reset,
	  input  logic clock
  );
	import "DPI-C" function int unsigned mem_read(input int unsigned raddr);
	import "DPI-C" function void mem_write(input int unsigned waddr, input int unsigned mask, input int unsigned wdata);

	logic [31:0] rdata_reg;
	assign out_bits_rdata = rdata_reg;

	typedef enum logic [1:0] {
		IDLE = 2'b00,
		READ = 2'b01,
		WRITE = 2'b10
		} state_t;

	state_t cs, ns;

	always_ff @(posedge clock) begin
		if(reset)
			cs <= IDLE;
		else
			cs <= ns;
	end

	always_ff @(posedge clock) begin
		if(reset) begin
  			rdata_reg <= 32'b0;
		end
		else begin
			if(in_ready & in_valid) begin
  				if(!in_bits_wen)
  					rdata_reg <= mem_read(in_bits_addr);
				else
					mem_write(in_bits_addr, in_bits_mask, in_bits_wdata);
			end
		end
	end

	always_comb begin
		ns = cs;
		case (cs)
			IDLE: begin
				if(in_ready & in_valid & in_bits_wen)
					ns = WRITE;
				if(in_ready & in_valid & !in_bits_wen)
					ns = READ;
			end
			READ: begin
				if(out_ready & out_valid) begin
					ns = IDLE;
				end
			end
			WRITE: begin
				if(out_ready & out_valid) begin
					ns = IDLE;
				end
			end
		endcase
	end

	assign out_bits_err = 4'b0;
	always_ff @(posedge clock) begin
		if(reset) begin
			in_ready <= 1'b1;
			out_valid <= 1'b0;
		end
		else begin
			case (ns)
				IDLE: begin
					in_ready  <= 1'b1;
					out_valid <= 1'b0;
				end
				READ: begin
					in_ready  <= 1'b0;
					out_valid <= 1'b1;
				end
			    WRITE: begin
				    in_ready  <= 1'b0;
					out_valid <= 1'b1;
		        end
				default: begin
  					in_ready  <= 1'b0;
					out_valid <= 1'b0;
				end
            endcase
        end
    end

endmodule
  """.stripMargin
  )

}
