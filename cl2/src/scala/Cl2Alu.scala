package cl2

import chisel3._
import chisel3.util._
import Control._

//We should change this
class Cl2UglyAlu extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(32.W))
    val b   = Input(UInt(32.W))
    val op  = Input(UInt(4.W))
    val res = Output(UInt(32.W))
    val eq  = Output(Bool())
  })

  io.res := 0.U
  io.eq  := io.a === io.b

  switch(io.op) {
    is(ALU_ADD) { io.res := io.a + io.b }
    is(ALU_SUB) { io.res := io.a - io.b }
    is(ALU_AND) { io.res := io.a & io.b }
    is(ALU_OR) { io.res := io.a | io.b }
    is(ALU_XOR) { io.res := io.a ^ io.b }
    is(ALU_SLT) { io.res := (io.a.asSInt < io.b.asSInt).asUInt }
    is(ALU_SLL) { io.res := io.a << io.b(4, 0) }
    is(ALU_SLTU) { io.res := (io.a < io.b).asUInt }
    is(ALU_SRL) { io.res := io.a >> io.b(4, 0) }
    is(ALU_SRA) { io.res := (io.a.asSInt >> io.b(4, 0)).asUInt }
  }
}
