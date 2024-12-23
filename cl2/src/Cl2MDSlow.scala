import chisel3._
import chisel3.util._

object MdOp extends ChiselEnum {
  val MD_OP_MULL, MD_OP_MULH, MD_OP_DIV, MD_OP_REM = Value
}

object MdState extends ChiselEnum {
  val MD_IDLE, MD_ABS_A, MD_ABS_B, MD_COMP, MD_LAST, MD_CHANGE_SIGN, MD_FINISH = Value
}

class OperandExtension(signedMode: UInt) extends Module {
  val io = IO(new Bundle {
    val opA = Input(UInt(32.W))
    val opB = Input(UInt(32.W))
    val opAExt = Output(UInt(33.W))
    val opBExt = Output(UInt(33.W))
  })

  val signA = io.opA(31) & signedMode(0)
  val signB = io.opB(31) & signedMode(1)

  io.opAExt := Cat(signA, io.opA)
  io.opBExt := Cat(signB, io.opB)
}

class Multiplier extends Module {
  val io = IO(new Bundle {
    val opAShift = Input(UInt(33.W))
    val opBShift = Input(UInt(33.W))
    val opA_bw_pp = Output(UInt(33.W))
    val opA_bw_last_pp = Output(UInt(33.W))
  })

    val b0 = Cat(Seq.fill(32)(io.opBShift(0)))
  io.opA_bw_pp := Cat(~(io.opAShift(32) & io.opBShift(0)), io.opAShift(31, 0) & b0)
  io.opA_bw_last_pp := Cat(io.opAShift(32) & io.opBShift(0), ~(io.opAShift(31, 0) & b0))
}

class Divider extends Module {
  val io = IO(new Bundle {
    val accumWindow = Input(UInt(33.W))
    val opBShift = Input(UInt(33.W))
    val divResult = Output(UInt(32.W))
    val remainder = Output(UInt(32.W))
    val isGreaterEqual = Output(Bool())
  })

    io.isGreaterEqual := (io.accumWindow(31) === io.opBShift(31)) && !io.accumWindow(32)

  io.remainder := Mux(io.isGreaterEqual, io.accumWindow(31, 0), io.accumWindow(31, 0))
  io.divResult := Mux(io.isGreaterEqual, io.accumWindow(32, 0), io.accumWindow(32, 0))
}

class MultDivSlow extends Module {
  val io = IO(new Bundle {
    val mult_en_i = Input(Bool())
    val div_en_i = Input(Bool())
    val mult_sel_i = Input(Bool())
    val div_sel_i = Input(Bool())
    val operator_i = Input(MdOp())
    val signed_mode_i = Input(UInt(2.W))
    val op_a_i = Input(UInt(32.W))
    val op_b_i = Input(UInt(32.W))
    val alu_adder_ext_i = Input(UInt(34.W))
    val alu_adder_i = Input(UInt(32.W))
    val equal_to_zero_i = Input(Bool())
    val data_ind_timing_i = Input(Bool())

    val alu_operand_a_o = Output(UInt(33.W))
    val alu_operand_b_o = Output(UInt(33.W))

    val imd_val_d_o = Output(Vec(2, UInt(34.W)))
    val imd_val_we_o = Output(Vec(2, Bool()))

    val multdiv_result_o = Output(UInt(32.W))
    val valid_o = Output(Bool())
  })

  val operandExt = Module(new OperandExtension(io.signed_mode_i))
  val multiplier = Module(new Multiplier)
  val divider = Module(new Divider)

	operandExt.io.opA := io.op_a_i
  operandExt.io.opB := io.op_b_i

  multiplier.io.opAShift := operandExt.io.opAExt
  multiplier.io.opBShift := operandExt.io.opBExt

  divider.io.accumWindow := io.alu_adder_ext_i
  divider.io.opBShift := operandExt.io.opBExt

  val mdState = RegInit(MdState.MD_IDLE)
  val accumWindow = RegInit(0.U(33.W))
  val opBShift = RegInit(0.U(33.W))
  val opAShift = RegInit(0.U(33.W))

	switch(mdState) {
		is(MdState.MD_IDLE) {
		  when(io.mult_en_i || io.div_en_i) {
		    mdState := MdState.MD_ABS_A
		  }
		}
		is(MdState.MD_ABS_A) {
		  accumWindow := Mux(io.operator_i === MdOp.MD_OP_DIV, operandExt.io.opAExt, 0.U)
		  opAShift := operandExt.io.opAExt
		  mdState := MdState.MD_ABS_B
		}
		is(MdState.MD_ABS_B) {
		  accumWindow := Mux(io.operator_i === MdOp.MD_OP_DIV, operandExt.io.opBExt, 0.U)
		  opBShift := operandExt.io.opBExt
		  mdState := MdState.MD_COMP
		}
		is(MdState.MD_COMP) {
		    when(io.operator_i === MdOp.MD_OP_MULL) {
		    	accumWindow := multiplier.io.opA_bw_pp
		    	mdState := MdState.MD_LAST
		  	}.elsewhen(io.operator_i === MdOp.MD_OP_DIV) {
		    	divider.io.isGreaterEqual := divider.io.isGreaterEqual
		    	mdState := MdState.MD_LAST
		  }
		}
		is(MdState.MD_LAST) {
		  mdState := MdState.MD_FINISH
		}
		is(MdState.MD_FINISH) {
		  io.valid_o := true.B
		  io.multdiv_result_o := accumWindow(31, 0)
		  mdState := MdState.MD_IDLE
		}
	}
	
  io.alu_operand_a_o := accumWindow
  io.alu_operand_b_o := opBShift
  io.imd_val_d_o(0) := accumWindow
  io.imd_val_we_o(0) := true.B
  io.imd_val_d_o(1) := accumWindow
  io.imd_val_we_o(1) := true.B
}
