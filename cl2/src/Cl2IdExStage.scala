// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import org.yaml.snakeyaml.events.Event.ID

class IdEx2WbSignals extends Bundle {
  // val wbType  = Output(UInt(2.W))
  // val stType  = Output(UInt(2.W))
  // val jType   = Output(UInt(3.W))
  // val imm     = Output(UInt(32.W))
  // val aluRes  = Output(UInt(32.W))
  // val wbWen   = Output(Bool())
  val pc = Output(UInt(32.W))

}

class Cl2IdExStage extends Module {
  val io = IO(new Bundle {
    val if2IdEx = Flipped(Decoupled(new If2IdExSignals()))
    val idEx2Wb = Decoupled(new IdEx2WbSignals())
    val dummy   = Output(Bool())
  })

  /* Decode */
  val decode = Module(new Cl2Decoder())

  decode.io.instr := io.if2IdEx.bits.instr

  /* GPR and CSR */
  val gpr = Module(new Cl2RegFile())
  gpr.io.readAddrA :=

  /* Alu and immediate */
  val alu = Module(new Cl2Alu(32))

  /* Pipeline control */
  val ready_go = Wire(Bool())
  val busy     = RegInit(false.B)

  ready_go := true.B // Always true right now

  io.if2IdEx.ready := !busy || ready_go && io.idEx2Wb.ready

  when(io.if2IdEx.ready) {
    busy := io.if2IdEx.valid
  }

  /* Pipeline Registers */
  val plRegs = Reg(new IdEx2WbSignals)
  val dummy  = RegEnable(io.dummy, true.B, io.if2IdEx.fire)

  when(io.if2IdEx.fire & !io.dummy) {
    plRegs.pc := io.if2IdEx.bits.pc
  }
  io.idEx2Wb.bits := plRegs

  io.dummy         := dummy
  io.idEx2Wb.valid := true.B

}
