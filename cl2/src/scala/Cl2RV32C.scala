// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.Disable.Type

case class CompressedPattern(
  val immType:     String,
  val aluopType:   String,
  val regType:     String,
  val func3:       BitPat = BitPat.dontCare(3),
  val imm3:        BitPat = BitPat.dontCare(3),
  val rs1:         BitPat = BitPat.dontCare(3),
  val imm2:        BitPat = BitPat.dontCare(2),
  val rs2:         BitPat = BitPat.dontCare(3),
  val opcode:      BitPat)
  extends DecodePattern  {
  def bitPat: BitPat = RV32Cpattern

  val RV32Cpattern = func3 ## imm3 ## rs1 ## imm2 ## rs2 ## opcode
}


object Cl2PreDecodeInfo {
  val possiblePatterns = Seq(
    CompressedPattern(regType = "C_LWSP     ", aluopType = "L  ", immType = "LWSP     ", func3 = BitPat("b010"), opcode = BitPat("b10")),  // C_LWSP
    CompressedPattern(regType = "C_LDSP     ", aluopType = "L  ", immType = "LDSP     ", func3 = BitPat("b011"), opcode = BitPat("b10")),  // C_LDSP
    CompressedPattern(regType = "C_SWSP     ", aluopType = "S  ", immType = "SWSP     ", func3 = BitPat("b110"), opcode = BitPat("b10")), // C_SWSP
    CompressedPattern(regType = "C_SDSP     ", aluopType = "S  ", immType = "SDSP     ", func3 = BitPat("b111"), opcode = BitPat("b10")), // C_SDSP
    CompressedPattern(regType = "C_NOP      ", aluopType = "ADD", immType = "None     ", func3 = BitPat("b000"), opcode = BitPat("b01")),  // C_NOP
    CompressedPattern(regType = "C_EBREAK   ", aluopType = "J  ", immType = "CBREAK   ", func3 = BitPat("b100"), imm3 = BitPat("1??"), opcode = BitPat("b10")), // C_EBREAK
    CompressedPattern(regType = "C_LW       ", aluopType = "L  ", immType = "LW       ", func3 = BitPat("b010"), opcode = BitPat("b00")), // C_LW
    CompressedPattern(regType = "C_LD       ", aluopType = "L  ", immType = "LD       ", func3 = BitPat("b011"), opcode = BitPat("b00")), // C_LD
    CompressedPattern(regType = "C_SW       ", aluopType = "S  ", immType = "SW       ", func3 = BitPat("b110"), opcode = BitPat("b00")), // C_SW
    CompressedPattern(regType = "C_SD       ", aluopType = "S  ", immType = "SD       ", func3 = BitPat("b111"), opcode = BitPat("b00")), // C_SD
    CompressedPattern(regType = "C_J        ", aluopType = "J  ", immType = "J        ", func3 = BitPat("b101"), opcode = BitPat("b01")),                        // C_J
    CompressedPattern(regType = "C_JR       ", aluopType = "J  ", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b0??"), opcode = BitPat("b10")), // C_JR
    CompressedPattern(regType = "C_BEQZ     ", aluopType = "B  ", immType = "B        ", func3 = BitPat("b110"), opcode = BitPat("b01")),  // C_BEQZ
    CompressedPattern(regType = "C_BNEZ     ", aluopType = "B  ", immType = "B        ", func3 = BitPat("b111"), opcode = BitPat("b01")),  // C_BNEZ
    CompressedPattern(regType = "C_LI       ", aluopType = "ADD", immType = "LI       ", func3 = BitPat("b010"), opcode = BitPat("b01")),  //   C_LI
    CompressedPattern(regType = "C_LUI      ", aluopType = "ADD", immType = "LUI      ", func3 = BitPat("b011"), opcode = BitPat("b01")),  //  C_LUI/C_ADDI16SP
    CompressedPattern(regType = "C_ADDI     ", aluopType = "ADD", immType = "ADDI     ", func3 = BitPat("b000"), opcode = BitPat("b01")),  // C_ADDI
    CompressedPattern(regType = "C_ADDI4SPN ", aluopType = "ADD", immType = "ADDI4SPN ", func3 = BitPat("b000"), opcode = BitPat("b00")), // C_ADDI4SPN
    CompressedPattern(regType = "C_SLLI     ", aluopType = "SLL", immType = "LI       ", func3 = BitPat("b000"), opcode = BitPat("b10")),                        // C_SLLI
    CompressedPattern(regType = "C_SRLI     ", aluopType = "SRL", immType = "LI       ", func3 = BitPat("b100"), imm3 = BitPat("b?00"), opcode = BitPat("b01")), // C_SRLI
    CompressedPattern(regType = "C_SRAI     ", aluopType = "SRA", immType = "LI       ", func3 = BitPat("b100"), imm3 = BitPat("b?01"), opcode = BitPat("b01")), // C_SRAI
    CompressedPattern(regType = "C_ANDI     ", aluopType = "AND", immType = "LI       ", func3 = BitPat("b100"), imm3 = BitPat("b?10"), opcode = BitPat("b01")), // C_ANDI
    CompressedPattern(regType = "C_MV       ", aluopType = "ADD", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b0??"), opcode = BitPat("b10")), // C_MV
    CompressedPattern(regType = "C_ADD      ", aluopType = "ADD", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b1??"), opcode = BitPat("b10")), // C_ADD
    CompressedPattern(regType = "C_SUB      ", aluopType = "SUB", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b011"), imm2 = BitPat("b00"), opcode = BitPat("b01")), // C_SUB
    CompressedPattern(regType = "C_XOR      ", aluopType = "XOR", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b011"), imm2 = BitPat("b00"), opcode = BitPat("b01")), // C_XOR
    CompressedPattern(regType = "C_OR       ", aluopType = "OR ", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b011"),  imm2 = BitPat("b00"),  opcode = BitPat("b01")),   // C_OR
    CompressedPattern(regType = "C_AND      ", aluopType = "AND", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("b011"),  imm2 = BitPat("b11"),  opcode = BitPat("b01")),  // C_AND
    CompressedPattern(regType = "C_SUBW     ", aluopType = "SUB", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("111"),  opcode = BitPat("b01")), //  C_SUBW
    CompressedPattern(regType = "C_ADDW     ", aluopType = "ADD", immType = "None     ", func3 = BitPat("b100"), imm3 = BitPat("111"),   opcode = BitPat("b01")) //   C_ADDW
  
    // CompressedPattern(immType = "CSS", func3 = BitPat("b111"), opcode = BitPat("b10")), // C_FSWSP    
    // CompressedPattern(func3 = BitPat("b101"), opcode = BitPat("b10")), // C_FSDSP
    // CompressedPattern(immType = "CI", func3 = BitPat("b011"), opcode = BitPat("b10")),  // C_FLWSP
    // CompressedPattern(immType = "CI", func3 = BitPat("b001"), opcode = BitPat("b10")),  // C_FLDSP
    // CompressedPattern(func6 = BitPat("b000???"), opcode = BitPat("b00")),  // C_ILLEGAL
    // CompressedPattern(func6 = BitPat("b101???"), opcode = BitPat("b00")), // C_FSD
    // CompressedPattern(func6 = BitPat("b001???"), opcode = BitPat("b00")), // C_FLD
    // CompressedPattern(immType = "CJ", func3 = BitPat("b001"), opcode = BitPat("b01")),                        // C_JAL
    // CompressedPattern(immType = "CR", func3 = BitPat("b100"), imm3 = BitPat("b1??"), opcode = BitPat("b10")), // C_JALR
    // CompressedPattern(func3 = BitPat("b001"), opcode = BitPat("b01")), // C_ADDIW
    // CompressedPattern(func3 = BitPat("b011"), opcode = BitPat("b01")), // C_ADDI16SP

  )

  val allFields = Seq(
    RV32CAluOpField ,
    RV32CImmField   ,
    RV32CJumpField  ,
    RV32CLoadField  ,
    RV32CStoreField ,
    RV32CDestField  ,
    RV32CSRC1Field  ,
    RV32CSRC2Field  
  )
}
