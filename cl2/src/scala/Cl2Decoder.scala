// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

object Control {
  val IMM_X = 0.U(3.W)
  val IMM_S = 1.U(3.W)
  val IMM_J = 2.U(3.W)
  val IMM_I = 3.U(3.W)
  val IMM_B = 4.U(3.W)
  val IMM_U = 5.U(3.W)

  val ASEL_REG = 0.U(2.W)
  val ASEL_PC  = 1.U(2.W)
  val ASEL_Z   = 2.U(2.W)

  val BSEL_IMM = 0.U(2.W)
  val BSEL_REG = 1.U(2.W)

  // We can optimize this, for example, we can use add to implement sub
  val ALU_ADD  = 0.U(4.W)
  val ALU_SUB  = 1.U(4.W)
  val ALU_AND  = 2.U(4.W)
  val ALU_OR   = 3.U(4.W)
  val ALU_XOR  = 4.U(4.W)
  val ALU_SLT  = 5.U(4.W)
  val ALU_SLL  = 6.U(4.W)
  val ALU_SLTU = 7.U(4.W)
  val ALU_SRL  = 8.U(4.W)
  val ALU_SRA  = 9.U(4.W)

  // We can do this in a better way
  val J_XXX  = 0.U(4.W)
  val J_JAL  = 1.U(4.W)
  val J_EQ   = 2.U(4.W)
  val J_NE   = 3.U(4.W)
  val J_GE   = 4.U(4.W)
  val J_GEU  = 5.U(4.W)
  val J_LT   = 6.U(4.W)
  val J_LTU  = 7.U(4.W)
  val J_JALR = 8.U(4.W)

  val ST_XXX = 0.U(2.W)
  val ST_SB  = 1.U(2.W)
  val ST_SH  = 2.U(2.W)
  val ST_SW  = 3.U(2.W)

  val LD_XXX = 0.U(3.W)
  val LD_LB  = 1.U(3.W)
  val LD_LBU = 2.U(3.W)
  val LD_LH  = 3.U(3.W)
  val LD_LHU = 4.U(3.W)
  val LD_LW  = 5.U(3.W)

  /* the data source of write back */
  val WB_ALU = 0.U(2.W)
  val WB_CSR = 1.U(2.W)
  val WB_MEM = 2.U(2.W)
  val WB_PC4 = 3.U(2.W)

  val MD_XX     = 0.U(4.W)
  val MD_MUL    = "b0001".U(4.W)
  val MD_MULH   = "b0100".U(4.W)
  val MD_MULHU  = "b0101".U(4.W)
  val MD_MULHSU = "b0110".U(4.W)
  val MD_DIVU   = "b1000".U(4.W)
  val MD_REMU   = "b1001".U(4.W)
  val MD_DIV    = "b1100".U(4.W)
  val MD_REM    = "b1101".U(4.W)
}

/* More tests, more fun */

// Potential optimization opportunity for this decoder
case class InstructionPattern(
  val instType: String,
  val ldType:   String = "LD_XXX",
  val stType:   String = "ST_XXX",
  val isCSR:    Boolean = false,
  val func7:    BitPat = BitPat.dontCare(7),
  val func3:    BitPat = BitPat.dontCare(3),
  val opcode:   BitPat = BitPat.dontCare(7))
    extends DecodePattern {
  def bitPat: BitPat = pattern

  val pattern = func7 ## BitPat.dontCare(10) ## func3 ## BitPat.dontCare(5) ## opcode

}

import Control._

object ImmSelField extends DecodeField[InstructionPattern, UInt] {
  def name: String = "immediate select"

  def chiselType:                       UInt   = UInt(3.W)
  def genTable(op: InstructionPattern): BitPat = {
    op.instType match {
      case "U" => BitPat(IMM_U)
      case "I" => BitPat(IMM_I)
      case "S" => BitPat(IMM_S)
      case "J" => BitPat(IMM_J)
      case "B" => BitPat(IMM_B)
      case _   => BitPat(IMM_X)
    }
  }
}

object EbreakField extends BoolDecodeField[InstructionPattern] {
  def name: String = "is ebreak instruction"

  def genTable(op: InstructionPattern): BitPat = {
    if (op.instType == "R" && op.func3.rawString == "000" && op.opcode.rawString == "1110011")
      BitPat(true.B)
    else
      BitPat(false.B)
  }
}

object AluOpField extends DecodeField[InstructionPattern, UInt] {
  def name: String = "ALU operation"

  def chiselType: UInt = UInt(4.W)

  def genTable(op: InstructionPattern): BitPat = {
    if (op.instType == "U")
      BitPat(ALU_ADD)
    else if (op.instType == "J")
      BitPat(ALU_ADD)
    else if (op.instType == "S")
      BitPat(ALU_ADD)
    else if (op.ldType != "LD_XXX") // load instructions are also I type
      BitPat(ALU_ADD)
    else if (op.instType == "I") {
      op.func3.rawString match {
        case "000" => BitPat(ALU_ADD) // jalr and addi
        case "010" => BitPat(ALU_SLT)
        case "011" => BitPat(ALU_SLTU)
        case "100" => BitPat(ALU_XOR)
        case "110" => BitPat(ALU_OR)
        case "111" => BitPat(ALU_AND)
        case "001" => BitPat(ALU_SLL)
        // So ugly here !!!
        case "101" =>
          if (op.func7.rawString == "0000000")
            BitPat(ALU_SRL)
          else
            BitPat(ALU_SRA)
        case _: String => BitPat(ALU_ADD)

      }
    } else if (op.instType == "R") {
      op.func3.rawString match {
        case "000" =>
          if (op.func7.rawString == "0000000")
            BitPat(ALU_ADD)
          else
            BitPat(ALU_SUB)
        case "001" => BitPat(ALU_SLL)
        case "010" => BitPat(ALU_SLT)
        case "011" => BitPat(ALU_SLTU)
        case "100" => BitPat(ALU_XOR)
        // So ugly here !!!
        case "101" =>
          if (op.func7.rawString == "0000000")
            BitPat(ALU_SRL)
          else
            BitPat(ALU_SRA)
        case "110" => BitPat(ALU_OR)
        case "111" => BitPat(ALU_AND)
        case _: String => BitPat(ALU_ADD)
      }
    } else if (op.instType == "B")
      BitPat(ALU_SUB)
    else
      BitPat(ALU_ADD)
  }
}

object AselField extends DecodeField[InstructionPattern, UInt] {
  def name:                             String = "ALU select A"
  def chiselType:                       UInt   = UInt(2.W)
  def genTable(op: InstructionPattern): BitPat = {
    if (op.instType == "I")
      BitPat(ASEL_REG)
    else if (op.opcode.rawString == "0110111")
      BitPat(ASEL_Z)
    else if (op.opcode.rawString == "0010111")
      BitPat(ASEL_PC)
    else if (op.instType == "R")
      BitPat(ASEL_REG)
    else if (op.instType == "J")
      BitPat(ASEL_PC)
    else if (op.instType == "B")
      BitPat(ASEL_REG)
    else
      BitPat(ASEL_Z)
  }
}

object BselField extends DecodeField[InstructionPattern, UInt] {
  def name:                             String = "ALU select B"
  def chiselType:                       UInt   = UInt(2.W)
  def genTable(op: InstructionPattern): BitPat = {
    if (op.instType == "I")
      BitPat(BSEL_IMM)
    else if (op.instType == "U")
      BitPat(BSEL_IMM)
    else if (op.instType == "R")
      BitPat(BSEL_REG)
    else if (op.instType == "J")
      BitPat(BSEL_IMM)
    else if (op.instType == "B")
      BitPat(BSEL_REG)
    else
      BitPat(BSEL_REG)
  }
}

object JumpField extends DecodeField[InstructionPattern, UInt] {
  def name:       String = "Jump type"
  def chiselType: UInt   = UInt(4.W)

  def genTable(op: InstructionPattern): BitPat = {
    // We can do this in a better way
    if (op.opcode.rawString == "1100111")
      BitPat(J_JALR)
    else if (op.opcode.rawString == "1101111")
      BitPat(J_JAL)
    else if (op.instType == "I")
      BitPat(J_XXX)
    else if (op.instType == "U")
      BitPat(J_XXX)
    else if (op.instType == "R")
      BitPat(J_XXX)
    else if (op.instType == "B") {
      op.func3.rawString match {
        case "000" => BitPat(J_EQ)
        case "001" => BitPat(J_NE)
        case "100" => BitPat(J_LT)
        case "101" => BitPat(J_GE)
        case "110" => BitPat(J_LTU)
        case "111" => BitPat(J_GEU)
        case _: String => BitPat(J_XXX)
      }
    } else
      BitPat(J_XXX)
  }
}

object StoreField extends DecodeField[InstructionPattern, UInt] {
  def name:       String = "Store type"
  def chiselType: UInt   = UInt(2.W)

  def genTable(op: InstructionPattern): BitPat = {
    if (op.stType == "ST_XXX")
      BitPat(ST_XXX)
    else
      op.stType match {
        case "ST_SB" => BitPat(ST_SB)
        case "ST_SH" => BitPat(ST_SH)
        case "ST_SW" => BitPat(ST_SW)
        case _       => BitPat(ST_XXX)
      }
  }
}

object LoadField extends DecodeField[InstructionPattern, UInt] {
  def name:       String = "Load type"
  def chiselType: UInt   = UInt(3.W)

  def genTable(op: InstructionPattern): BitPat = {
    if (op.ldType == "LD_XXX")
      BitPat(LD_XXX)
    else
      op.ldType match {
        case "LD_LB"  => BitPat(LD_LB)
        case "LD_LBU" => BitPat(LD_LBU)
        case "LD_LH"  => BitPat(LD_LH)
        case "LD_LHU" => BitPat(LD_LHU)
        case "LD_LW"  => BitPat(LD_LW)
        case _        => BitPat(LD_XXX)
      }
  }
}

object WBackField extends DecodeField[InstructionPattern, UInt] {
  def name:       String = "Write back type"
  def chiselType: UInt   = UInt(2.W)

  def genTable(op: InstructionPattern): BitPat = {
    if (op.ldType != "LD_XXX")
      BitPat(WB_MEM)
    else if (op.opcode.rawString == "1101111")
      BitPat(WB_PC4)
    else if (op.instType == "I" && op.func3.rawString == "000" && op.opcode.rawString == "1100111")
      BitPat(WB_PC4)
    else
      BitPat(WB_ALU)
  }
}

//Do we really need this signal ?
object WenField extends BoolDecodeField[InstructionPattern] {
  def name:                             String = "write back enable"
  def genTable(op: InstructionPattern): BitPat = {
    if (op.instType == "B" || op.instType == "S") {
      BitPat(false.B)
    } else if (op.func3.rawString == "000" && op.opcode.rawString == "0001111")
      BitPat(false.B) // fence
    else if (op.func3.rawString == "000" && op.opcode.rawString == "1110011")
      BitPat(false.B) // ecall and ebreak
    else
      BitPat(true.B)
  }
}

//TODO:
object IsCSRField extends BoolDecodeField[InstructionPattern] {
  def name:                             String = "csr operations"
  def genTable(op: InstructionPattern): BitPat = {
    BitPat(false.B)
  }
}

//TODO:
object IllegalField extends BoolDecodeField[InstructionPattern] {
  def name: String = "illegal instruction"

  def genTable(op: InstructionPattern): BitPat = {
    BitPat(false.B)
  }
}

object MultDivField extends DecodeField[InstructionPattern, UInt] {
  def name:                             String = "Mult and div op"
  def chiselType:                       UInt   = UInt(4.W)
  def genTable(op: InstructionPattern): BitPat = {
    // We can do this in a beter way, rvdecoderdb
    if (op.func7.rawString == "0000001" && op.opcode.rawString == "0110011") {
      op.func3.rawString match {
        case "000" => BitPat(MD_MUL)
        case "001" => BitPat(MD_MULH)
        case "010" => BitPat(MD_MULHSU)
        case "011" => BitPat(MD_MULHU)
        case "100" => BitPat(MD_DIV)
        case "101" => BitPat(MD_DIVU)
        case "110" => BitPat(MD_REM)
        case "111" => BitPat(MD_REMU)
        case _: String => BitPat(MD_XX)
      }
    } else
      BitPat(MD_XX)

  }

}

class DecodeOutput extends Bundle {

  val immType  = Output(UInt(3.W))
  val aluOp    = Output(UInt(4.W))
  val aSel     = Output(UInt(2.W))
  val bSel     = Output(UInt(2.W))
  val jType    = Output(UInt(4.W))
  val stType   = Output(UInt(2.W))
  val ldType   = Output(UInt(3.W))
  val wbType   = Output(UInt(2.W))
  val wbWen    = Output(Bool())
  val isEbreak = Output(Bool())
  val md       = Output(UInt(4.W))
  // val isCsr     = Output(Bool())
  // val isIllegal = Output(Bool())
}

class Cl2Decoder extends Module {

  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))
    val out   = Output(new DecodeOutput())
  })

  val decodeTable  = new DecodeTable(Cl2DecodeInfo.possiblePatterns, Cl2DecodeInfo.allFields)
  val decodeResult = decodeTable.decode(io.instr)

  // Can we use an elegant way to do this ?
  io.out.aSel     := decodeResult(AselField)
  io.out.bSel     := decodeResult(BselField)
  io.out.aluOp    := decodeResult(AluOpField)
  io.out.immType  := decodeResult(ImmSelField)
  io.out.jType    := decodeResult(JumpField)
  io.out.ldType   := decodeResult(LoadField)
  io.out.stType   := decodeResult(StoreField)
  io.out.wbType   := decodeResult(WBackField)
  io.out.wbWen    := decodeResult(WenField)
  io.out.isEbreak := decodeResult(EbreakField)
  io.out.md       := decodeResult(MultDivField)
}
