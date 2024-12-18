// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import scala.collection.immutable.Stream.Cons

class If2IdExSignals extends Bundle {
  val pc    = Output(UInt(32.W))
  val instr = Output(UInt(32.W))
}

class IfMemReq extends Bundle {
  val raddr = Output(UInt(32.W))
}

//note that we will implement compressed ext in the future
class IfMemResp extends Bundle {
  val data = Input(UInt(32.W))
}

class Cl2IfetchStage extends Module {
  val io = IO(new Bundle {
    val nextPC    = Input(UInt(32.W))
    val instrReq  = Valid(new IfMemReq())
    val instrResp = Flipped(Valid(new IfMemResp()))
    val idEx      = Decoupled(new If2IdExSignals())
    val flush     = Input(Bool())
    val dummy     = Output(Bool())
  })

  val pc = RegEnable(io.nextPC, Cl2Config.BOOT_ADDR.U, io.idEx.fire)

  val ready_go = io.instrResp.valid

  io.idEx.valid := ready_go

  io.instrReq.bits.raddr := pc
  io.instrReq.valid      := true.B

  val plPC    = RegEnable(pc, 0.U(32.W), io.idEx.fire & !io.flush)
  val plInstr = RegEnable(io.instrResp.bits.data, 0.U(32.W), io.idEx.fire & !io.flush)
  val plDummy = RegEnable(Mux(io.flush, false.B, true.B), false.B, io.idEx.fire)

  io.idEx.bits.instr := plInstr
  io.idEx.bits.pc    := plPC
  io.dummy           := plDummy
}
