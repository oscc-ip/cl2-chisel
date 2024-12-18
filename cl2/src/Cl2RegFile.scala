// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class Cl2RegFile extends Module {
  val io = IO(new Bundle {
    val readAddrA = Input(UInt(5.W))
    val readAddrB = Input(UInt(5.W))
    val readDataA = Output(UInt(32.W))
    val readDataB = Output(UInt(32.W))
    val wen       = Input(Bool())
    val writeAddr = Input(UInt(5.W))
    val writeData = Input(UInt(32.W))
  })

  // Can we just use 31 registers here ?
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  io.readDataA := Mux(io.readAddrA.orR, regs(io.readAddrA), 0.U)
  io.readDataB := Mux(io.readAddrB.orR, regs(io.readAddrB), 0.U)

  when(io.wen && io.writeAddr.orR) {
    regs(io.writeAddr) := io.writeData
  }

}
