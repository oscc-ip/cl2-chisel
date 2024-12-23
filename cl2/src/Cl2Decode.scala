// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

//TODO: fix interface for C-Extension
case class InstructionPattern(
  val instType: String,
  val ldType:   String = "LD_XXX",
  val stType:   String = "ST_XXX",
  val isCSR:    Boolean = false,
  val func7:    BitPat = BitPat.dontCare(7),
  val func3:    BitPat = BitPat.dontCare(3),
  // ToDo: add C-Extension Pattern
  val opcode:   BitPat)
    extends DecodePattern {
  def bitPat: BitPat = pattern

  val pattern = func7 ## BitPat.dontCare(10) ## func3 ## BitPat.dontCare(5) ## opcode
}

object TestField extends BoolDecodeField[InstructionPattern] {
  def name: String = "test decoder"

  def genTable(op: InstructionPattern): BitPat = {
    op.func3.rawString match {
      case "000" => BitPat(true.B)
      case _     => BitPat(false.B)
    }
  }
}

class DecodeOutput extends Bundle {

  val immType   = Output(UInt(3.W))
  val aluOp     = Output(UInt(4.W))
  val aSel      = Output(UInt(2.W))
  val bSel      = Output(UInt(2.W))
  val jType     = Output(UInt(2.W))
  val stType    = Output(UInt(2.W))
  val ldType    = Output(UInt(2.W))
  val wbType    = Output(UInt(2.W))
  val wbWen     = Output(Bool())
  val isCsr     = Output(Bool())
  val isIllegal = Output(Bool())
}

class Cl2Decoder extends Module {

  val io = IO(new Bundle {
    val instr   = Input(UInt(32.W))
    val testOut = Output(Bool())
    val out     = Output(new DecodeOutput())
  })

  //TODO: RVC-immediate generation & register selection
  //TODO: 
  val decodeTable  = new DecodeTable(Cl2DecodeInfo.possiblePatterns ++ Cl2PreDecodeInfo.possiblePatterns, Cl2DecodeInfo.allFields ++ Cl2PreDecodeInfo.allFields)
  val decodeResult = decodeTable.decode(io.instr)

  io.testOut := decodeResult(TestField)

}
