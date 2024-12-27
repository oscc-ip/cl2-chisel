// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class If2IdExSignals extends Bundle {
  val pc    = Output(UInt(32.W))
  val instr = Output(UInt(32.W))
}

class Cl2IfetchStage extends Module {
  val io = IO(new Bundle {
    val nextPC = Input(UInt(32.W))

    val if2IdEx = Decoupled(new If2IdExSignals())
    val dummy   = Output(Bool())

    val pc    = Output(UInt(32.W))
    val flush = Input(Bool())
  })

  val pc = RegEnable(io.nextPC, Cl2Config.BOOT_ADDR.U, io.if2IdEx.fire)
  io.pc := pc

  val icacheHelper = Module(new cache_helper()) // DPI-C

  icacheHelper.io.reset         := reset
  icacheHelper.io.clock         := clock
  icacheHelper.io.in.valid      := true.B
  icacheHelper.io.in.bits.addr  := pc
  icacheHelper.io.in.bits.mask  := 15.U
  icacheHelper.io.in.bits.wdata := 0.U
  icacheHelper.io.in.bits.wen   := false.B
  icacheHelper.io.out.ready     := io.if2IdEx.ready

  /* flush */
  val plDummy = RegEnable(Mux(io.flush, true.B, false.B), true.B, io.if2IdEx.fire)

  /* Pipeline control */
  val ready_go = icacheHelper.io.out.valid

  io.if2IdEx.valid := ready_go

  /* pipeline output */
  val plPC    = RegEnable(pc, 0.U(32.W), io.if2IdEx.fire & !io.flush)
  // How to handle a memory access error? exception
  val plInstr = RegEnable(icacheHelper.io.out.bits.rdata, 0.U(32.W), io.if2IdEx.fire & !io.flush)

  io.if2IdEx.bits.instr := plInstr
  io.if2IdEx.bits.pc    := plPC
  io.dummy              := plDummy
}
