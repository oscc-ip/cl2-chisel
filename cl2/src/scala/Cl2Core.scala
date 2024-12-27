// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class Cl2Core extends Module {
  val io = IO(new Bundle {
    val hart_id = Input(UInt(32.W))
    //interrupt
  })

  val if_stage   = Module(new Cl2IfetchStage)
  val idex_stage = Module(new Cl2IdExStage)

  val jAddr = idex_stage.io.jAddr
  val jump  = idex_stage.io.jump
  val flush = jump && jAddr =/= if_stage.io.pc

  if_stage.io.nextPC := Mux(flush, jAddr, if_stage.io.pc + 4.U)
  if_stage.io.flush  := flush

  idex_stage.io.if2IdEx <> if_stage.io.if2IdEx

  idex_stage.io.dummy_in := if_stage.io.dummy

  val wb_stage = Module(new Cl2WbStage)

  val instr       = if_stage.io.if2IdEx.bits.instr
  val rs1         = instr(19, 15)
  val rs2         = instr(24, 20)
  val rd          = wb_stage.io.rd
  val bypassValue = wb_stage.io.wbValue

  val gpr = Module(new Cl2RegFile)

  gpr.io.wen       := wb_stage.io.wen
  gpr.io.writeData := wb_stage.io.wbValue
  gpr.io.writeAddr := wb_stage.io.rd
  gpr.io.readAddrA := rs1
  gpr.io.readAddrB := rs2

  idex_stage.io.rs1Value := Mux(rs1 === rd, bypassValue, gpr.io.readDataA)
  idex_stage.io.rs2Value := Mux(rs2 === rd, bypassValue, gpr.io.readDataB)

  wb_stage.io.idEx2Wb <> idex_stage.io.idEx2Wb
  wb_stage.io.dummy := idex_stage.io.dummy_out

}
