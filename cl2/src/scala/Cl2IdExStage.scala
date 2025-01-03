// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

import Control._
import os.read

object SignExt {
  def apply(sig: UInt, len: Int): UInt = {
    val signBit = sig(sig.getWidth - 1)
    if (sig.getWidth >= len) sig(len - 1, 0) else signBit.asUInt ## Fill(len - sig.getWidth, signBit) ## sig
  }
}

object ZeroExt {
  def apply(sig: UInt, len: Int): UInt = {
    if (sig.getWidth >= len) sig(len - 1, 0) else 0.U((len - sig.getWidth).W) ## sig
  }
}
class IdEx2WbSignals extends Bundle {
  val wbType   = Output(UInt(2.W))
  val ldType   = Output(UInt(3.W))
  val stType   = Output(UInt(2.W))
  val aluRes   = Output(UInt(32.W))
  val rs2Value = Output(UInt(32.W)) // Do we really need this signal ?
  val rdAddr   = Output(UInt(5.W))
  val wbWen    = Output(Bool())
  val pc       = Output(UInt(32.W))
  val isEbreak = Output(Bool())

}

class Cl2IdExStage extends Module {
  val io = IO(new Bundle {
    val if2IdEx = Flipped(Decoupled(new If2IdExSignals()))
    val idEx2Wb = Decoupled(new IdEx2WbSignals())

    val rs1Value = Input(UInt(32.W))
    val rs2Value = Input(UInt(32.W))

    val jump  = Output(Bool())
    val jAddr = Output(UInt(32.W))

    val dummy_in  = Input(Bool())
    val dummy_out = Output(Bool())

  })

  val instr = io.if2IdEx.bits.instr
  val pc    = io.if2IdEx.bits.pc

  val busy = RegEnable(io.if2IdEx.valid, false.B, io.if2IdEx.ready)

  /* Decode */
  val decode = Module(new Cl2Decoder())

  decode.io.instr := instr

  val ctrl = decode.io.out

  /* Immediate */
  // We can use another way to do this
  val Uimm = Cat(io.if2IdEx.bits.instr(31, 12), 0.U(12.W))
  val Iimm = SignExt(instr(31, 20), 32)
  val Bimm = SignExt(Cat(instr(31), instr(7), instr(30, 25), instr(11, 8), 0.U(1.W)), 32)
  val Simm = SignExt(Cat(instr(31, 25), instr(11, 7)), 32)
  val Jimm = SignExt(Cat(instr(31), instr(19, 12), instr(20), instr(30, 25), instr(24, 21), 0.U(1.W)), 32)

  val imm = Wire(UInt(32.W))
  imm := MuxLookup(ctrl.immType, 0.U)(
    Seq(
      IMM_U -> Uimm,
      IMM_B -> Bimm,
      IMM_I -> Iimm,
      IMM_J -> Jimm,
      IMM_S -> Simm,
      IMM_X -> Iimm
    )
  )

  /* ALU */
  val alu = Module(new Cl2UglyAlu)
  val a   = MuxLookup(ctrl.aSel, 0.U)(
    Seq(
      ASEL_PC  -> pc,
      ASEL_REG -> io.rs1Value,
      ASEL_Z   -> 0.U
    )
  )

  val b = MuxLookup(ctrl.bSel, 0.U)(
    Seq(
      BSEL_REG -> io.rs2Value,
      BSEL_IMM -> imm
    )
  )

  alu.io.op := decode.io.out.aluOp

  alu.io.a := a
  alu.io.b := b
  val res = alu.io.res
  val lt  = Mux(a(31) === b(31), res(31), a(31))
  val ltu = Mux(a(31) === b(31), res(31), b(31))
  // We can do this better

  /* Mult and Div */

  // fanout ?
  val div = Module(new RestoringDivider)
  dontTouch(div.io)
  div.io.in.bits(0) := a
  div.io.in.bits(1) := b
  div.io.sign       := ctrl.md(2)
  div.io.in.valid   := busy && !io.dummy_in && ctrl.md(3)
  div.io.out.ready  := io.idEx2Wb.ready

  val mult = Module(new BoothMultiplier)
  dontTouch(mult.io)
  mult.io.in.bits(0) := a.asSInt
  mult.io.in.bits(1) := b.asSInt
  mult.io.in.valid   := busy && !io.dummy_in && !ctrl.md(3) && ctrl.md =/= MD_XX
  val mult_ready = RegInit(false.B)
  when(mult.io.in.fire) {
    mult_ready := true.B
  }.elsewhen(mult.io.out.fire) {
    mult_ready := false.B
  }
  mult.io.out.ready := io.idEx2Wb.ready && mult_ready

  /* Pipeline control */
  val ready_go = Wire(Bool())
  // Pay attention to ready_go signal
  ready_go := ctrl.md === MD_XX || ctrl.md(3) && div.io.out.valid || mult_ready && mult.io.out.valid

  io.if2IdEx.ready := !busy || ready_go && io.idEx2Wb.ready || io.dummy_in

  val jump = MuxLookup(ctrl.jType, false.B)(
    Seq(
      J_XXX  -> false.B,
      J_JAL  -> true.B,
      J_JALR -> true.B,
      J_EQ   -> alu.io.eq,
      J_NE   -> !alu.io.eq,
      J_LT   -> lt,
      J_LTU  -> ltu,
      J_GE   -> ~lt,
      J_GEU  -> ~ltu
    )
  )
  io.jump := jump && !io.dummy_in && ready_go

  val br_addr = (imm + pc)
  // We can do this better
  io.jAddr := MuxLookup(ctrl.jType, 0.U)(
    Seq(
      J_XXX  -> 0.U,
      J_EQ   -> br_addr,
      J_NE   -> br_addr,
      J_LT   -> br_addr,
      J_LTU  -> br_addr,
      J_GE   -> br_addr,
      J_GEU  -> br_addr,
      J_JAL  -> res,
      J_JALR -> (res & "hfffffffe".U)
    )
  )
  /* Pipeline Registers */
  val exRes = MuxLookup(ctrl.md, alu.io.res)(
    Seq(
      MD_XX   -> alu.io.res,
      MD_DIV  -> div.io.out.bits(31, 0),
      MD_DIVU -> div.io.out.bits(31, 0),
      MD_REM  -> div.io.out.bits(63, 32),
      MD_REMU -> div.io.out.bits(63, 32),
      MD_MUL  -> mult.io.out.bits(31, 0),
      MD_MULH -> mult.io.out.bits(63, 32)
    )
  )

  val plRegs = RegInit(0.U.asTypeOf(new IdEx2WbSignals))
  // How to do this in a more elegant way ?
  when(io.idEx2Wb.fire & !io.dummy_in) {
    plRegs.pc       := io.if2IdEx.bits.pc
    plRegs.aluRes   := exRes
    plRegs.ldType   := ctrl.ldType
    plRegs.stType   := ctrl.stType
    plRegs.wbType   := ctrl.wbType
    plRegs.wbWen    := ctrl.wbWen
    plRegs.rs2Value := io.rs2Value
    plRegs.rdAddr   := instr(11, 7)
    plRegs.isEbreak := ctrl.isEbreak
  }

  val dummy_r = RegEnable(io.dummy_in, true.B, io.idEx2Wb.fire)

  io.idEx2Wb.bits := plRegs

  io.dummy_out     := dummy_r
  io.idEx2Wb.valid := ready_go && busy || io.dummy_in

}
