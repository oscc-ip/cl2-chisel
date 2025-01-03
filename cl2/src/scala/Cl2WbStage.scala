// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

import Control._
import os.read

class Cl2WbStage extends Module {

  val io = IO(new Bundle {
    val idEx2Wb = Flipped(Decoupled(new IdEx2WbSignals()))
    val dummy   = Input(Bool())

    // Write back signals, don't care CSR now
    val wen     = Output(Bool())
    val wbValue = Output(UInt(32.W))
    val rd      = Output(UInt(5.W))

    val ok = Output(Bool())

  })

  val busy = RegEnable(io.idEx2Wb.valid, false.B, io.idEx2Wb.ready)

  /* LSU interface */

  val isLoad  = io.idEx2Wb.bits.ldType =/= LD_XXX
  val isStore = io.idEx2Wb.bits.stType =/= ST_XXX

  val lsu = Module(new Cl2Lsu)

  lsu.io.ldType := io.idEx2Wb.bits.ldType
  lsu.io.stType := io.idEx2Wb.bits.stType

  lsu.io.memReq.bits.addr  := io.idEx2Wb.bits.aluRes
  lsu.io.memReq.bits.mask  := 0.U     // Let Lsu do this
  lsu.io.memReq.bits.wdata := io.idEx2Wb.bits.rs2Value
  lsu.io.memReq.bits.wen   := false.B // Let Lsu do this

  // What happens if memReq channel does not handshake ?
  // This simple interface assumes that the valid signal is deasserted only when
  // a resp valid signal is recieved, so wo don't need ready signal actually
  lsu.io.memReq.valid := busy && (isLoad || isStore) && !io.dummy

  lsu.io.memResp.ready := true.B

  /* Pipeline control */

  // val ready_go = io.dummy || (isLoad && lsu.io.memResp.valid) || (isStore && lsu.io.memResp.valid) || (!isLoad && !isStore)
  val ready_go = (isLoad && lsu.io.memResp.valid) || (isStore && lsu.io.memResp.valid) || (!isLoad && !isStore)

  io.idEx2Wb.ready := !busy || ready_go || io.dummy

  /* Write back */

  // io.wen := io.idEx2Wb.bits.wbWen && busy && !io.dummy
  io.wen := io.idEx2Wb.bits.wbWen && ready_go && !io.dummy && busy
  // io.ok := io.idEx2Wb.fire && !io.dummy
  io.ok  := ready_go && !io.dummy && busy

  val aluRes = io.idEx2Wb.bits.aluRes
  // How to deal with error here ?
  val memRes = lsu.io.memResp.bits.rdata

  val pc = Wire(UInt(32.W))
  pc         := io.idEx2Wb.bits.pc
  io.wbValue := MuxLookup(io.idEx2Wb.bits.wbType, aluRes)(
    Seq(
      WB_ALU -> aluRes,
      WB_CSR -> aluRes, // change in the future
      WB_MEM -> memRes,
      WB_PC4 -> (pc + 4.U)
    )
  )

  io.rd := io.idEx2Wb.bits.rdAddr

}
