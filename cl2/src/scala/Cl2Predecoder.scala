package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.Disable.Type

object RV32CControl {
  val RV32CRegTable = List(
    "b000".U -> 8.U,
    "b001".U -> 9.U,
    "b010".U -> 10.U,
    "b011".U -> 11.U,
    "b100".U -> 12.U,
    "b101".U -> 13.U,
    "b110".U -> 14.U,
    "b111".U -> 15.U
  )

  val ImmNone    = "b10000".U
  val ImmLWSP    = "b00000".U
  val ImmLDSP    = "b00001".U
  val ImmSWSP    = "b00010".U
  val ImmSDSP    = "b00011".U
  val ImmSW      = "b00100".U
  val ImmSD      = "b00101".U
  val ImmLW      = "b00110".U
  val ImmLD      = "b00111".U
  val ImmJ       = "b01000".U
  val ImmB       = "b01001".U
  val ImmLI      = "b01010".U
  val ImmLUI     = "b01011".U
  val ImmADDI    = "b01100".U
  val ImmADDI16SP = "b01101".U
  val ImmADD4SPN = "b01110".U
  val ImmCBREAK  = "b01111".U

  val DtCare     = "b0000".U
  val REGrs      = "b0011".U
  val REGrt      = "b0001".U
  val REGrd      = "b0010".U
  val REGrs1     = "b0100".U
  val REGrs2     = "b0101".U
  val REGrs1p    = "b0110".U
  val REGrs2p    = "b0111".U
  val REGx1      = "b1000".U
  val REGx2      = "b1001".U

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

  val ST_XXX = 0.U(3.W)
  val ST_SB  = 1.U(3.W)
  val ST_SH  = 2.U(3.W)
  val ST_SW  = 3.U(3.W)
  val ST_SD  = 4.U(3.W)

  val LD_XXX = 0.U(3.W)
  val LD_LB  = 1.U(3.W)
  val LD_LBU = 2.U(3.W)
  val LD_LH  = 3.U(3.W)
  val LD_LHU = 4.U(3.W)
  val LD_LW  = 5.U(3.W)
  val LD_LD  = 6.U(3.W)
}

import RV32CControl._

object RV32CImmField extends DecodeField[CompressedPattern, UInt] {
  def name: String = "immediate select"

  def chiselType:   UInt   = UInt(5.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.immType match {
      case "None    " => BitPat(ImmNone    )
      case "LWSP    " => BitPat(ImmLWSP    )
      case "LDSP    " => BitPat(ImmLDSP    )
      case "SWSP    " => BitPat(ImmSWSP    )
      case "SDSP    " => BitPat(ImmSDSP    )
      case "SW      " => BitPat(ImmSW      )
      case "SD      " => BitPat(ImmSD      )
      case "LW      " => BitPat(ImmLW      )
      case "LD      " => BitPat(ImmLD      )
      case "J       " => BitPat(ImmJ       )
      case "B       " => BitPat(ImmB       )
      case "LI      " => BitPat(ImmLI      )
      case "LUI     " => BitPat(ImmLUI     )
      case "ADDI    " => BitPat(ImmADDI    )
      case "ADDI16SP" => BitPat(ImmADDI16SP)
      case "ADD4SPN " => BitPat(ImmADD4SPN )
      case "CBREAK  " => BitPat(ImmCBREAK  )
      case _          => BitPat(ImmNone    )
    }
  }
}

object RV32CAluOpField extends DecodeField[CompressedPattern, UInt] {
  def name: String = "ALU operation"

  def chiselType: UInt = UInt(4.W)

  def genTable(op: CompressedPattern): BitPat = {
    if (op.aluopType == "ADD")
      BitPat(ALU_ADD)
    else if (op.aluopType == "SLT")
      BitPat(ALU_SLT)
    else if (op.aluopType == "XOR")
      BitPat(ALU_XOR)
    else if (op.aluopType == "OR")
      BitPat(ALU_OR)
    else if (op.aluopType == "SUB")
      BitPat(ALU_SUB)
    else if (op.aluopType == "SRL")
      BitPat(ALU_SRL)
    else if (op.aluopType == "SRA")
      BitPat(ALU_SRA)
    else if (op.aluopType == "AND")
      BitPat(ALU_AND)
    else
      BitPat(ALU_ADD)
  }
}

object RV32CSRC1Field extends DecodeField[CompressedPattern, UInt] {
  def name:                             String = "SRC1"
  def chiselType:                       UInt   = UInt(4.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.regType match {
      case "C_LWSP     " => BitPat(REGx2      )
      case "C_LDSP     " => BitPat(REGx2      )
      case "C_SWSP     " => BitPat(REGx2      )
      case "C_SDSP     " => BitPat(REGx2      ) 
      case "C_NOP      " => BitPat(DtCare     )
      case "C_EBREAK   " => BitPat(DtCare     )
      case "C_LW       " => BitPat(REGrs1p    )
      case "C_LD       " => BitPat(REGrs1p    )
      case "C_SW       " => BitPat(REGrs1p    )
      case "C_SD       " => BitPat(REGrs1p    )
      case "C_J        " => BitPat(DtCare     )
      case "C_JR       " => BitPat(REGrs1     )
      case "C_BEQZ     " => BitPat(REGrs1p    )
      case "C_BNEZ     " => BitPat(REGrs1p    )
      case "C_LI       " => BitPat(DtCare     )
      case "C_LUI      " => BitPat(DtCare     )
      case "C_ADDI     " => BitPat(REGrd      )
      case "C_ADDI4SPN " => BitPat(REGx2      )
      case "C_SLLI     " => BitPat(REGrd      )
      case "C_SRLI     " => BitPat(REGrs1p    )
      case "C_SRAI     " => BitPat(REGrs1p    )
      case "C_ANDI     " => BitPat(REGrs1p    )
      case "C_MV       " => BitPat(REGrs2     )
      case "C_ADD      " => BitPat(REGrd      )
      case "C_SUB      " => BitPat(REGrs1p    )
      case "C_XOR      " => BitPat(REGrs1p    )
      case "C_OR       " => BitPat(REGrs1p    )
      case "C_AND      " => BitPat(REGrs1p    )
      case "C_SUBW     " => BitPat(REGrs1p    )
      case "C_ADDW     " => BitPat(REGrs1p    )
    }
  }
}


object RV32CSRC2Field extends DecodeField[CompressedPattern, UInt] {
  def name:                             String = "SRC2"
  def chiselType:                       UInt   = UInt(4.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.regType match {
      case "C_LWSP     " => BitPat(DtCare     )
      case "C_LDSP     " => BitPat(DtCare     )
      case "C_SWSP     " => BitPat(REGrs2     )
      case "C_SDSP     " => BitPat(REGrs2     ) 
      case "C_NOP      " => BitPat(DtCare     )
      case "C_EBREAK   " => BitPat(DtCare     )
      case "C_LW       " => BitPat(DtCare     )
      case "C_LD       " => BitPat(DtCare     )
      case "C_SW       " => BitPat(REGrs2p    )
      case "C_SD       " => BitPat(REGrs2p    )
      case "C_J        " => BitPat(DtCare     )
      case "C_JR       " => BitPat(DtCare     )
      case "C_BEQZ     " => BitPat(DtCare     )
      case "C_BNEZ     " => BitPat(DtCare     )
      case "C_LI       " => BitPat(DtCare     )
      case "C_LUI      " => BitPat(DtCare     )
      case "C_ADDI     " => BitPat(DtCare     )
      case "C_ADDI4SPN " => BitPat(DtCare     )
      case "C_SLLI     " => BitPat(DtCare     )
      case "C_SRLI     " => BitPat(DtCare     )
      case "C_SRAI     " => BitPat(DtCare     )
      case "C_ANDI     " => BitPat(DtCare     )
      case "C_MV       " => BitPat(DtCare     )
      case "C_ADD      " => BitPat(REGrs2     )
      case "C_SUB      " => BitPat(REGrs2p    )
      case "C_XOR      " => BitPat(REGrs2p    )
      case "C_OR       " => BitPat(REGrs2p    )
      case "C_AND      " => BitPat(REGrs2p    )
      case "C_SUBW     " => BitPat(REGrs2p    )
      case "C_ADDW     " => BitPat(REGrs2p    )
    }
  }
}

object RV32CDestField extends DecodeField[CompressedPattern, UInt] {
  def name:                             String = "Dest"
  def chiselType:                       UInt   = UInt(4.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.regType match {
      case "C_LWSP     " => BitPat(REGrd      )
      case "C_LDSP     " => BitPat(REGrd      )
      case "C_SWSP     " => BitPat(DtCare     )
      case "C_SDSP     " => BitPat(DtCare     ) 
      case "C_NOP      " => BitPat(DtCare     )
      case "C_EBREAK   " => BitPat(DtCare     )
      case "C_LW       " => BitPat(REGrs2p    )
      case "C_LD       " => BitPat(REGrs2p    )
      case "C_SW       " => BitPat(DtCare     )
      case "C_SD       " => BitPat(DtCare     )
      case "C_J        " => BitPat(DtCare     )
      case "C_JR       " => BitPat(DtCare     )
      case "C_BEQZ     " => BitPat(DtCare     )
      case "C_BNEZ     " => BitPat(DtCare     )
      case "C_LI       " => BitPat(REGrd      )
      case "C_LUI      " => BitPat(REGrd      )
      case "C_ADDI     " => BitPat(REGrd      )
      case "C_ADDI4SPN " => BitPat(REGrs2p    )
      case "C_SLLI     " => BitPat(REGrd      )
      case "C_SRLI     " => BitPat(REGrs1p    )
      case "C_SRAI     " => BitPat(REGrs1p    )
      case "C_ANDI     " => BitPat(REGrs1p    )
      case "C_MV       " => BitPat(REGrd      )
      case "C_ADD      " => BitPat(REGrd      )
      case "C_SUB      " => BitPat(REGrs1p    )
      case "C_XOR      " => BitPat(REGrs1p    )
      case "C_OR       " => BitPat(REGrs1p    )
      case "C_AND      " => BitPat(REGrs1p    )
      case "C_SUBW     " => BitPat(REGrs1p    )
      case "C_ADDW     " => BitPat(REGrs1p    )
    }
  }
}

object RV32CJumpField extends DecodeField[CompressedPattern, UInt] {
  def name:       String = "Jump type"
  def chiselType: UInt   = UInt(4.W)

  def genTable(op: CompressedPattern): BitPat = {
    // We can do this in a better way
    if (op.aluopType == "J" && op.immType == "J")
      BitPat(J_JAL)
    else if (op.aluopType == "J" && op.immType == "None")
      BitPat(J_JALR)
    else if (op.aluopType == "J" && op.immType == "CBREAK")
      BitPat(J_XXX)
    else if (op.aluopType == "B" && op.regType == "C_BENQZ")
      BitPat(J_EQ)
    else if (op.aluopType == "B" && op.regType == "C_BNEZ")
      BitPat(J_NE)
    else
      BitPat(J_XXX)
  }
}

object RV32CStoreField extends DecodeField[CompressedPattern, UInt] {
  def name:       String = "Store type"
  def chiselType: UInt   = UInt(3.W)

  def genTable(op: CompressedPattern): BitPat = {
    if (op.aluopType == "S" && op.immType == "SWSP")
      BitPat(ST_SW)
    else if (op.aluopType == "S" && op.immType == "SDSP")
      BitPat(ST_SD)
    else if (op.aluopType == "S" && op.immType == "SW")
      BitPat(ST_SW)
    else if (op.aluopType == "S" && op.immType == "SD")
      BitPat(ST_SD)
    else
      BitPat(ST_XXX)
  }
}

object RV32CLoadField extends DecodeField[CompressedPattern, UInt] {
  def name:       String = "Store type"
  def chiselType: UInt   = UInt(3.W)

  def genTable(op: CompressedPattern): BitPat = {
    if (op.aluopType == "S" && op.immType == "LWSP")
      BitPat(LD_LW)
    else if (op.aluopType == "S" && op.immType == "LDSP")
      BitPat(LD_LD)
    else if (op.aluopType == "S" && op.immType == "LW")
      BitPat(LD_LW)
    else if (op.aluopType == "S" && op.immType == "LD")
      BitPat(LD_LD)
    else
      BitPat(LD_XXX)
  }
}

class RV32CPreDecodeOutput extends Bundle {

  val immType  = Output(UInt(5.W))
  val aluOp    = Output(UInt(4.W))
  val SRC1     = Output(UInt(4.W))
  val SRC2     = Output(UInt(4.W))
  val Dest     = Output(UInt(4.W))
  val jType    = Output(UInt(4.W))
  val stType   = Output(UInt(3.W))
  val ldType   = Output(UInt(3.W))
}

class Cl2Predecoder extends Module {

  val io = IO(new Bundle {
    val instr = Input(UInt(32.W))
    val out   = Output(new RV32CPreDecodeOutput())
  })

  val decodeTable  = new DecodeTable(Cl2PreDecodeInfo.possiblePatterns, Cl2PreDecodeInfo.allFields)
  val decodeResult = decodeTable.decode(io.instr)

  io.out.aluOp    := decodeResult(RV32CAluOpField ) 
  io.out.immType  := decodeResult(RV32CImmField   )
  io.out.jType    := decodeResult(RV32CJumpField  )
  io.out.ldType   := decodeResult(RV32CLoadField  )
  io.out.stType   := decodeResult(RV32CStoreField )
  io.out.Dest     := decodeResult(RV32CDestField  )
  io.out.SRC1     := decodeResult(RV32CSRC1Field  )
  io.out.SRC2     := decodeResult(RV32CSRC2Field  )
}
