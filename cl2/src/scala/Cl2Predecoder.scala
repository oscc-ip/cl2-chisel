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

  val ImmNone     = "b10000".U
  val ImmLWSP     = "b00000".U
  val ImmLDSP     = "b00001".U
  val ImmSWSP     = "b00010".U
  val ImmSDSP     = "b00011".U
  val ImmSW       = "b00100".U
  val ImmSD       = "b00101".U
  val ImmLW       = "b00110".U
  val ImmLD       = "b00111".U
  val ImmJ        = "b01000".U
  val ImmB        = "b01001".U
  val ImmLI       = "b01010".U
  val ImmLUI      = "b01011".U
  val ImmADDI     = "b01100".U
  val ImmADDI16SP = "b01101".U
  val ImmADD4SPN  = "b01110".U
  val ImmCBREAK   = "b01111".U

  val DtCare  = "b0000".U
  val REGrs   = "b0011".U
  val REGrt   = "b0001".U
  val REGrd   = "b0010".U
  val REGrs1  = "b0100".U
  val REGrs2  = "b0101".U
  val REGrs1p = "b0110".U
  val REGrs2p = "b0111".U
  val REGx1   = "b1000".U
  val REGx2   = "b1001".U

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

  def chiselType:                      UInt   = UInt(5.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.immType match {
      case "None    " => BitPat(ImmNone)
      case "LWSP    " => BitPat(ImmLWSP)
      case "LDSP    " => BitPat(ImmLDSP)
      case "SWSP    " => BitPat(ImmSWSP)
      case "SDSP    " => BitPat(ImmSDSP)
      case "SW      " => BitPat(ImmSW)
      case "SD      " => BitPat(ImmSD)
      case "LW      " => BitPat(ImmLW)
      case "LD      " => BitPat(ImmLD)
      case "J       " => BitPat(ImmJ)
      case "B       " => BitPat(ImmB)
      case "LI      " => BitPat(ImmLI)
      case "LUI     " => BitPat(ImmLUI)
      case "ADDI    " => BitPat(ImmADDI)
      case "ADDI16SP" => BitPat(ImmADDI16SP)
      case "ADD4SPN " => BitPat(ImmADD4SPN)
      case "CBREAK  " => BitPat(ImmCBREAK)
      case _          => BitPat(ImmNone)
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
  def name:                            String = "SRC1"
  def chiselType:                      UInt   = UInt(4.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.regType match {
      case "C_LWSP     " => BitPat(REGx2)
      case "C_LDSP     " => BitPat(REGx2)
      case "C_SWSP     " => BitPat(REGx2)
      case "C_SDSP     " => BitPat(REGx2)
      case "C_NOP      " => BitPat(DtCare)
      case "C_EBREAK   " => BitPat(DtCare)
      case "C_LW       " => BitPat(REGrs1p)
      case "C_LD       " => BitPat(REGrs1p)
      case "C_SW       " => BitPat(REGrs1p)
      case "C_SD       " => BitPat(REGrs1p)
      case "C_J        " => BitPat(DtCare)
      case "C_JR       " => BitPat(REGrs1)
      case "C_BEQZ     " => BitPat(REGrs1p)
      case "C_BNEZ     " => BitPat(REGrs1p)
      case "C_LI       " => BitPat(DtCare)
      case "C_LUI      " => BitPat(DtCare)
      case "C_ADDI     " => BitPat(REGrd)
      case "C_ADDI4SPN " => BitPat(REGx2)
      case "C_SLLI     " => BitPat(REGrd)
      case "C_SRLI     " => BitPat(REGrs1p)
      case "C_SRAI     " => BitPat(REGrs1p)
      case "C_ANDI     " => BitPat(REGrs1p)
      case "C_MV       " => BitPat(REGrs2)
      case "C_ADD      " => BitPat(REGrd)
      case "C_SUB      " => BitPat(REGrs1p)
      case "C_XOR      " => BitPat(REGrs1p)
      case "C_OR       " => BitPat(REGrs1p)
      case "C_AND      " => BitPat(REGrs1p)
      case "C_SUBW     " => BitPat(REGrs1p)
      case "C_ADDW     " => BitPat(REGrs1p)
    }
  }
}

object RV32CSRC2Field extends DecodeField[CompressedPattern, UInt] {
  def name:                            String = "SRC2"
  def chiselType:                      UInt   = UInt(4.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.regType match {
      case "C_LWSP     " => BitPat(DtCare)
      case "C_LDSP     " => BitPat(DtCare)
      case "C_SWSP     " => BitPat(REGrs2)
      case "C_SDSP     " => BitPat(REGrs2)
      case "C_NOP      " => BitPat(DtCare)
      case "C_EBREAK   " => BitPat(DtCare)
      case "C_LW       " => BitPat(DtCare)
      case "C_LD       " => BitPat(DtCare)
      case "C_SW       " => BitPat(REGrs2p)
      case "C_SD       " => BitPat(REGrs2p)
      case "C_J        " => BitPat(DtCare)
      case "C_JR       " => BitPat(DtCare)
      case "C_BEQZ     " => BitPat(DtCare)
      case "C_BNEZ     " => BitPat(DtCare)
      case "C_LI       " => BitPat(DtCare)
      case "C_LUI      " => BitPat(DtCare)
      case "C_ADDI     " => BitPat(DtCare)
      case "C_ADDI4SPN " => BitPat(DtCare)
      case "C_SLLI     " => BitPat(DtCare)
      case "C_SRLI     " => BitPat(DtCare)
      case "C_SRAI     " => BitPat(DtCare)
      case "C_ANDI     " => BitPat(DtCare)
      case "C_MV       " => BitPat(DtCare)
      case "C_ADD      " => BitPat(REGrs2)
      case "C_SUB      " => BitPat(REGrs2p)
      case "C_XOR      " => BitPat(REGrs2p)
      case "C_OR       " => BitPat(REGrs2p)
      case "C_AND      " => BitPat(REGrs2p)
      case "C_SUBW     " => BitPat(REGrs2p)
      case "C_ADDW     " => BitPat(REGrs2p)
    }
  }
}

object RV32CDestField extends DecodeField[CompressedPattern, UInt] {
  def name:                            String = "Dest"
  def chiselType:                      UInt   = UInt(4.W)
  def genTable(op: CompressedPattern): BitPat = {
    op.regType match {
      case "C_LWSP     " => BitPat(REGrd)
      case "C_LDSP     " => BitPat(REGrd)
      case "C_SWSP     " => BitPat(DtCare)
      case "C_SDSP     " => BitPat(DtCare)
      case "C_NOP      " => BitPat(DtCare)
      case "C_EBREAK   " => BitPat(DtCare)
      case "C_LW       " => BitPat(REGrs2p)
      case "C_LD       " => BitPat(REGrs2p)
      case "C_SW       " => BitPat(DtCare)
      case "C_SD       " => BitPat(DtCare)
      case "C_J        " => BitPat(DtCare)
      case "C_JR       " => BitPat(DtCare)
      case "C_BEQZ     " => BitPat(DtCare)
      case "C_BNEZ     " => BitPat(DtCare)
      case "C_LI       " => BitPat(REGrd)
      case "C_LUI      " => BitPat(REGrd)
      case "C_ADDI     " => BitPat(REGrd)
      case "C_ADDI4SPN " => BitPat(REGrs2p)
      case "C_SLLI     " => BitPat(REGrd)
      case "C_SRLI     " => BitPat(REGrs1p)
      case "C_SRAI     " => BitPat(REGrs1p)
      case "C_ANDI     " => BitPat(REGrs1p)
      case "C_MV       " => BitPat(REGrd)
      case "C_ADD      " => BitPat(REGrd)
      case "C_SUB      " => BitPat(REGrs1p)
      case "C_XOR      " => BitPat(REGrs1p)
      case "C_OR       " => BitPat(REGrs1p)
      case "C_AND      " => BitPat(REGrs1p)
      case "C_SUBW     " => BitPat(REGrs1p)
      case "C_ADDW     " => BitPat(REGrs1p)
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

  val immType = Output(UInt(5.W))
  val aluOp   = Output(UInt(4.W))
  val SRC1    = Output(UInt(4.W))
  val SRC2    = Output(UInt(4.W))
  val Dest    = Output(UInt(4.W))
  val jType   = Output(UInt(4.W))
  val stType  = Output(UInt(3.W))
  val ldType  = Output(UInt(3.W))
}

//class Cl2Predecoder extends Module {
//
//  val io = IO(new Bundle {
//    val instr = Input(UInt(32.W))
//    val out   = Output(new RV32CPreDecodeOutput())
//  })
//
//  val decodeTable  = new DecodeTable(Cl2PreDecodeInfo.possiblePatterns, Cl2PreDecodeInfo.allFields)
//  val decodeResult = decodeTable.decode(io.instr)
//
//  io.out.aluOp    := decodeResult(RV32CAluOpField )
//  io.out.immType  := decodeResult(RV32CImmField   )
//  io.out.jType    := decodeResult(RV32CJumpField  )
//  io.out.ldType   := decodeResult(RV32CLoadField  )
//  io.out.stType   := decodeResult(RV32CStoreField )
//  io.out.Dest     := decodeResult(RV32CDestField  )
//  io.out.SRC1     := decodeResult(RV32CSRC1Field  )
//  io.out.SRC2     := decodeResult(RV32CSRC2Field  )
//}

object Opcodes {
  val OPCODE_LOAD     = "h03".U(7.W)
  val OPCODE_MISC_MEM = "h0f".U(7.W)
  val OPCODE_OP_IMM   = "h13".U(7.W)
  val OPCODE_AUIPC    = "h17".U(7.W)
  val OPCODE_STORE    = "h23".U(7.W)
  val OPCODE_OP       = "h33".U(7.W)
  val OPCODE_LUI      = "h37".U(7.W)
  val OPCODE_BRANCH   = "h63".U(7.W)
  val OPCODE_JALR     = "h67".U(7.W)
  val OPCODE_JAL      = "h6f".U(7.W)
  val OPCODE_SYSTEM   = "h73".U(7.W)
}

import Opcodes._
class Cl2Predecoder extends Module {
  val io = IO(new Bundle {
    val instr   = Input(UInt(16.W))
    val out     = Output(UInt(32.W))
    val illegal = Output(Bool())
  })

  // Default assignments
  io.out     := Fill(2, io.instr)
  io.illegal := true.B

  // Decode compressed instructions
  switch(io.instr(1, 0)) {
    // C0
    is("b00".U) {
      switch(io.instr(15, 13)) {
        is("b000".U) {
          // c.addi4spn -> addi rd', x2, imm
          io.out := Cat(
            0.U(2.W),
            io.instr(10, 7),
            io.instr(12, 11),
            io.instr(5),
            io.instr(6),
            0.U(2.W),
            2.U(5.W),
            "b000".U(3.W),
            "b01".U(2.W),
            io.instr(4, 2),
            OPCODE_OP_IMM
          )
          when(io.instr(12, 5) === 0.U) {
            io.illegal := true.B
          }
        }

        is("b010".U) {
          // c.lw -> lw rd', imm(rs1')
          io.out := Cat(
            0.U(5.W),
            io.instr(5),
            io.instr(12, 10),
            io.instr(6),
            0.U(2.W),
            "b01".U(2.W),
            io.instr(9, 7),
            "b010".U(3.W),
            "b01".U(2.W),
            io.instr(4, 2),
            OPCODE_LOAD
          )
        }

        is("b110".U) {
          // c.sw -> sw rs2', imm(rs1')
          io.out := Cat(
            0.U(5.W),
            io.instr(5),
            io.instr(12),
            "b01".U(2.W),
            io.instr(4, 2),
            "b01".U(2.W),
            io.instr(9, 7),
            "b010".U(3.W),
            io.instr(11, 10),
            io.instr(6),
            0.U(2.W),
            OPCODE_STORE
          )
        }

        is("b001".U, "b011".U, "b100".U, "b101".U, "b111".U) {
          io.illegal := true.B
        }

      }
    }

    // C1
    is("b01".U) {
      switch(io.instr(15, 13)) {
        is("b000".U) {
          // c.addi -> addi rd, rd, nzimm (c.nop)
          io.out := Cat(
            Fill(6, io.instr(12)),
            io.instr(12),
            io.instr(6, 2),
            io.instr(11, 7),
            "b000".U(3.W),
            io.instr(11, 7),
            OPCODE_OP_IMM
          )
        }

        is("b001".U, "b101".U) {
          // c.jal -> jal x1, imm
          // c.j   -> jal x0, imm
          io.out := Cat(
            io.instr(12),
            io.instr(8),
            io.instr(10, 9),
            io.instr(6),
            io.instr(7),
            io.instr(2),
            io.instr(11),
            io.instr(5, 3),
            Fill(9, io.instr(12)),
            0.U(4.W),
            ~io.instr(15),
            OPCODE_JAL
          )
        }

        is("b010".U) {
          // c.li -> addi rd, x0, nzimm
          io.out := Cat(
            Fill(6, io.instr(12)),
            io.instr(12),
            io.instr(6, 2),
            0.U(5.W),
            "b000".U(3.W),
            io.instr(11, 7),
            OPCODE_OP_IMM
          )
        }

        is("b011".U) {
          // c.lui -> lui rd, imm
          io.out := Cat(
            Fill(15, io.instr(12)),
            io.instr(6, 2),
            io.instr(11, 7),
            OPCODE_LUI
          )

          when(io.instr(11, 7) === "b00010".U) {
            // c.addi16sp -> addi x2, x2, nzimm
            io.out := Cat(
              Fill(3, io.instr(12)),
              io.instr(4, 3),
              io.instr(5),
              io.instr(2),
              io.instr(6),
              0.U(4.W),
              2.U(5.W),
              "b000".U(3.W),
              2.U(5.W),
              OPCODE_OP_IMM
            )
          }

          when(Cat(io.instr(12), io.instr(6, 2)) === 0.U) {
            io.illegal := true.B
          }
        }
        is("b110".U, "b111".U) {
          // c.beqz -> beq rs1', x0, imm
          // c.bnez -> bne rs1', x0, imm
          io.out := Cat(
            Fill(4, io.instr(12)),
            io.instr(6, 5),
            io.instr(2),
            0.U(5.W),
            "b01".U(2.W),
            io.instr(9, 7),
            "b000".U(2.W),
            io.instr(13),
            io.instr(11, 10),
            io.instr(4, 3),
            io.instr(12),
            OPCODE_BRANCH
          )
        }
        is("b100".U) {
          switch(io.instr(11, 10)) {

            is("b00".U, "b01".U) {
              // c.srli -> srli rd, rd, shamt
              // c.srai -> srai rd, rd, shamt
              io.out := Cat(
                0.U(1.W),
                io.instr(10),
                0.U(5.W),
                io.instr(6, 2),
                "b01".U(2.W),
                io.instr(9, 7),
                "b101".U(3.W),
                "b01".U(2.W),
                io.instr(9, 7),
                OPCODE_OP_IMM
              )
              when(io.instr(12) === 1.U) {
                io.illegal := true.B
              }
            }
            is("b10".U) {
              // c.andi -> andi rd, rd, imm
              io.out := Cat(
                Fill(6, io.instr(12)),
                io.instr(12),
                io.instr(6, 2),
                "b01".U(2.W),
                io.instr(9, 7),
                "b111".U(3.W),
                "b01".U(2.W),
                io.instr(9, 7),
                OPCODE_OP_IMM
              )
            }
            is("b11".U) {
              switch(Cat(io.instr(12), io.instr(6, 5))) {
                is("b000".U) {
                  // c.sub -> sub rd', rd', rs2'
                  io.out := Cat(
                    "b01".U(2.W),
                    0.U(5.W),
                    "b01".U(2.W),
                    io.instr(4, 2),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    "b000".U(3.W),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    OPCODE_OP
                  )
                }
                is("b001".U) {
                  // c.xor -> xor rd', rd', rs2'
                  io.out := Cat(
                    0.U(7.W),
                    "b01".U(2.W),
                    io.instr(4, 2),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    "b100".U(3.W),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    OPCODE_OP
                  )
                }
                is("b010".U) {
                  // c.or -> or rd', rd', rs2'
                  io.out := Cat(
                    0.U(7.W),
                    "b01".U(2.W),
                    io.instr(4, 2),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    "b110".U(3.W),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    OPCODE_OP
                  )
                }
                is("b011".U) {
                  // c.and -> and rd', rd', rs2'
                  io.out := Cat(
                    0.U(7.W),
                    "b01".U(2.W),
                    io.instr(4, 2),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    "b111".U(3.W),
                    "b01".U(2.W),
                    io.instr(9, 7),
                    OPCODE_OP
                  )
                }
              }
            }
          }
        }

      }
    }

    // C2
    is("b10".U) {
      switch(io.instr(15, 13)) {
        is("b000".U) {
          // c.slli -> slli rd, rd, shamt
          io.out := Cat(
            0.U(7.W),
            io.instr(6, 2),
            io.instr(11, 7),
            "b001".U(3.W),
            io.instr(11, 7),
            OPCODE_OP_IMM
          )
          when(io.instr(12) === 1.U) {
            io.illegal := true.B // Reserved for custom extensions
          }
        }

        is("b010".U) {
          // c.lwsp -> lw rd, imm(x2)
          io.out := Cat(
            0.U(4.W),
            io.instr(3, 2),
            io.instr(12),
            io.instr(6, 4),
            0.U(2.W),
            2.U(5.W),
            "b010".U(3.W),
            io.instr(11, 7),
            OPCODE_LOAD
          )
          when(io.instr(11, 7) === 0.U) {
            io.illegal := true.B
          }
        }

        is("b100".U) {
          when(io.instr(12) === 0.U) {
            when(io.instr(6, 2) =/= 0.U) {
              // c.mv -> add rd/rs1, x0, rs2
              io.out := Cat(
                0.U(7.W),
                io.instr(6, 2),
                0.U(5.W),
                "b000".U(3.W),
                io.instr(11, 7),
                OPCODE_OP
              )
            }.otherwise {
              // c.jr -> jalr x0, rd/rs1, 0
              io.out := Cat(
                0.U(12.W),
                io.instr(11, 7),
                "b000".U(3.W),
                0.U(5.W),
                OPCODE_JALR
              )
              when(io.instr(11, 7) === 0.U) {
                io.illegal := true.B
              }
            }
          }.otherwise {
            when(io.instr(6, 2) =/= 0.U) {
              // c.add -> add rd, rd, rs2
              io.out := Cat(
                0.U(7.W),
                io.instr(6, 2),
                io.instr(11, 7),
                "b000".U(3.W),
                io.instr(11, 7),
                OPCODE_OP
              )
            }.otherwise {
              when(io.instr(11, 7) === 0.U) {
                // c.ebreak -> ebreak
                io.out := "h00100073".U
              }.otherwise {
                // c.jalr -> jalr x1, rs1, 0
                io.out := Cat(
                  0.U(12.W),
                  io.instr(11, 7),
                  "b000".U(3.W),
                  1.U(5.W),
                  OPCODE_JALR
                )
              }
            }
          }
        }

        is("b110".U) {
          // c.swsp -> sw rs2, imm(x2)
          io.out := Cat(
            0.U(4.W),
            io.instr(8, 7),
            io.instr(12),
            io.instr(6, 2),
            2.U(5.W),
            "b010".U(3.W),
            io.instr(11, 9),
            0.U(2.W),
            OPCODE_STORE
          )
        }

        is("b001".U, "b011".U, "b101".U, "b111".U) {
          io.illegal := true.B
        }

      }
    }

    // Incoming instruction is not compressed
    is("b11".U) {
      // Default case: no changes
    }

  }
}
