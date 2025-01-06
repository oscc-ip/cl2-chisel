// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import os.stat

class If2IdExSignals extends Bundle {
  val pc           = Output(UInt(32.W))
  val instr        = Output(UInt(32.W))
  val isCompressed = Output(Bool())
}

class Cl2IfetchStage extends Module {
  val io = IO(new Bundle {
    val nextPC = Input(UInt(32.W))

    val if2IdEx = Decoupled(new If2IdExSignals())
    val dummy   = Output(Bool())

    val pc    = Output(UInt(32.W))
    val flush = Input(Bool())

    val interrupt = Input(Bool())

    val isCompressed = Output(Bool())

  })

  // val pc = RegEnable(io.nextPC, Cl2Config.BOOT_ADDR.U, io.if2IdEx.fire)
  val pc = RegEnable(io.nextPC, Cl2Config.BOOT_ADDR.U, io.if2IdEx.fire || io.flush)
  // val snpc = Mux(pc(1), pc + 2.U, pc + 4.U)
  io.pc := pc

  val icacheHelper = Module(new cache_helper()) // DPI-C

  // val s_align1 :: s_align2 :: s_unalign :: Nil = Enum(3)
  // val state = RegInit(s_align1)
  // val buffer = RegInit(0.U(16.W))
  val rdata = icacheHelper.io.out.bits.rdata

  val full32 = rdata(1, 0) === "b11".U
  // val l16h32 = rdata(1, 0) =/= "b11".U && rdata(17, 16) === "b11".U
  // val l16h16 = rdata(1, 0) =/= "b11".U && rdata(17, 16) =/= "b11".U
  // val h32 = rdata(17, 16) === "b11".U
  // val h16 = rdata(17, 16) =/= "b11".U
  // when(state === s_align1 && full32) {state := s_align1}
  // when(state === s_align1 && l16h32 && io.if2IdEx.fire) {
  //   state := s_unalign
  //   buffer := rdata(31, 16)
  // }
  // when(state === s_align1 && l16h16 && io.if2IdEx.fire) {
  //   state := s_align2
  //   buffer := rdata(31, 16)
  // }
  // when(state === s_align2 && io.if2IdEx.fire) {state := s_align1}
  // // when(state === s_unalign && h32) {state := s_unalign}
  // when(state === s_unalign && h16 && io.if2IdEx.fire) {state := s_align2}

  // flush
  // when(io.flush) {
  //   state := s_align1
  //   buffer := 0.U
  // }

  icacheHelper.io.reset         := reset
  icacheHelper.io.clock         := clock
  icacheHelper.io.in.valid      := !io.flush // Pay attention here, change it the future
  icacheHelper.io.in.bits.addr  := pc
  icacheHelper.io.in.bits.mask  := 0.U
  icacheHelper.io.in.bits.wdata := 0.U
  icacheHelper.io.in.bits.wen   := false.B
  // icacheHelper.io.out.ready     := MuxLookup(state, false.B)(Seq(
  //   s_align1 -> true.B,
  //   s_align2 -> false.B,
  //   s_unalign -> true.B
  // ))
  icacheHelper.io.out.ready     := true.B

  val predecode = Module(new Cl2Predecoder)
  predecode.io.instr := rdata(15, 0)
  // predecode.io.instr := MuxLookup(state, 0.U)(Seq(
  //   s_align1 -> rdata(15, 0),
  //   s_align2 -> buffer,
  //   s_unalign -> buffer
  // ))

  /* flush */
  // val plDummy = RegEnable(Mux(io.flush, true.B, false.B), true.B, io.if2IdEx.fire)
  val plDummy = RegInit(true.B)
  when(io.flush && io.if2IdEx.ready) {
    plDummy := true.B
  }.elsewhen(io.if2IdEx.fire) {
    plDummy := false.B
  }

  /* Pipeline control */
  // val ready_go = icacheHelper.io.out.valid
  // val ready_go = MuxLookup(state, true.B)(Seq(
  //   s_align1 -> icacheHelper.io.out.valid,
  //   s_align2 -> true.B,
  //   s_unalign -> icacheHelper.io.out.valid
  // ))
  val ready_go = icacheHelper.io.out.valid
  io.if2IdEx.valid := ready_go

  /* pipeline output */
  val plPC = RegEnable(pc, 0.U(32.W), io.if2IdEx.fire & !io.flush)

  // How to handle a memory access error? exception
  // val instr = MuxLookup(state, 0.U(32.W))(Seq(
  //   s_align1 -> Mux(full32, rdata, predecode.io.out),
  //   s_align2 -> predecode.io.out,
  //   s_unalign -> Cat(buffer, rdata(15, 0))

  // ))
  val isCompressed = !full32
  io.isCompressed := isCompressed
  val plComressed = RegEnable(isCompressed, false.B, io.if2IdEx.fire & !io.flush)
  val instr       = Mux(isCompressed, predecode.io.out, rdata)
  val plInstr     = RegEnable(instr, 0.U(32.W), io.if2IdEx.fire & !io.flush)

  io.if2IdEx.bits.instr        := plInstr
  io.if2IdEx.bits.pc           := plPC
  io.if2IdEx.bits.isCompressed := plComressed
  io.dummy                     := plDummy
}
