// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.Disable.Type

case class CompressedPattern(
  val instType: String,
  val func3:    BitPat = BitPat.dontCare(3),
  val imm3:     BitPat = BitPat.dontCare(3),
  val rs1:      BitPat = BitPat.dontCare(3),
  val imm2:     BitPat = BitPat.dontCare(2),
  val rs2:      BitPat = BitPat.dontCare(3),
  val opcode:   BitPat)
    extends DecodePattern {
  def bitPat: BitPat = pattern

  // TODO: read the spec
  val pattern = func3 ## imm3 ## rs1 ## imm2 ## rs2 ## opcode

}
//This can not be compiled right now
object Cl2PreDecodeInfo {
  val possiblePatterns = Seq(
    CompressedPattern(instType = "CI", func3 = BitPat("b010"), opcode = BitPat("b10")),  // C_LWSP
    // CompressedPattern(func3 = BitPat("b011"), opcode = BitPat("b10")),  // C_LDSP
    // CompressedPattern(func3 = BitPat("b011"), opcode = BitPat("b10")),  // C_FLWSP
    // CompressedPattern(func3 = BitPat("b001"), opcode = BitPat("b10")),  // C_FLDSP
    // CompressedPattern(func3 = BitPat("b101"), opcode = BitPat("b10")), // C_FSDSP
    CompressedPattern(instType = "CSS", func3 = BitPat("b110"), opcode = BitPat("b10")), // C_SWSP
    // CompressedPattern(func3 = BitPat("b111"), opcode = BitPat("b10")), // C_FSWSP
    // CompressedPattern(func3 = BitPat("b111"), opcode = BitPat("b10")), // C_SDSP

    // CompressedPattern(func6 = BitPat("b000???"), opcode = BitPat("b00")),  // C_ILLEGAL
    // CompressedPattern(func6 = BitPat("b000???"), opcode = BitPat("b01")),  // C_NOP
    // CompressedPattern(func6 = BitPat("b1001??"), opcode = BitPat("b10")), // C_EBREAK

    // Register-Based Stores and Loads
    // CompressedPattern(func6 = BitPat("b001???"), opcode = BitPat("b00")), // C_FLD
    CompressedPattern(instType = "CL", func3 = BitPat("b010"), opcode = BitPat("b00")), // C_LW
    // CompressedPattern(func6 = BitPat("b011???"), opcode = BitPat("b00")), // C_LD

    // CompressedPattern(func6 = BitPat("b101???"), opcode = BitPat("b00")), // C_FSD
    CompressedPattern(instType = "CS", func3 = BitPat("b110"), opcode = BitPat("b00")),                        // C_SW
    // CompressedPattern(func6 = BitPat("b111???"), opcode = BitPat("b00")), // C_SD
    // Stack-Pointer-Based Stores and Loads
    // // Control Transfer
    CompressedPattern(instType = "CJ", func3 = BitPat("b101"), opcode = BitPat("b01")),                        // C_J
    CompressedPattern(instType = "CJ", func3 = BitPat("b001"), opcode = BitPat("b01")),                        // C_JAL
    CompressedPattern(instType = "CR", func3 = BitPat("b100"), imm3 = BitPat("b0??"), opcode = BitPat("b10")), // C_JR
    CompressedPattern(instType = "CR", func3 = BitPat("b100"), imm3 = BitPat("b1??"), opcode = BitPat("b10")), // C_JALR

    CompressedPattern(instType = "CB", func3 = BitPat("b110"), opcode = BitPat("b01")),  // C_BEQZ
    CompressedPattern(instType = "CB", func3 = BitPat("b111"), opcode = BitPat("b01")),  // C_BNEZ
    // // Integer Constant-Generation
    CompressedPattern(instType = "CI", func3 = BitPat("b010"), opcode = BitPat("b01")),  // C_LI
    CompressedPattern(instType = "CI", func3 = BitPat("b011"), opcode = BitPat("b01")),  // C_LUI/C_ADDI16SP
    // // Integer Register-Immediate Operations
    CompressedPattern(instType = "CI", func3 = BitPat("b000"), opcode = BitPat("b01")),  // C_ADDI
    // CompressedPattern(func3 = BitPat("b001"), opcode = BitPat("b01")), // C_ADDIW
    // CompressedPattern(func3 = BitPat("b011"), opcode = BitPat("b01")), // C_ADDI16SP
    CompressedPattern(instType = "CIW", func3 = BitPat("b000"), opcode = BitPat("b00")), // C_ADDI4SPN

    CompressedPattern(instType = "CI", func3 = BitPat("b000"), opcode = BitPat("b10")),                        // C_SLLI
    CompressedPattern(instType = "CB", func3 = BitPat("b100"), imm3 = BitPat("b?00"), opcode = BitPat("b01")), // C_SRLI
    CompressedPattern(instType = "CB", func3 = BitPat("b100"), imm3 = BitPat("b?01"), opcode = BitPat("b01")), // C_SRAI
    CompressedPattern(instType = "CB", func3 = BitPat("b100"), imm3 = BitPat("b?10"), opcode = BitPat("b01")), // C_ANDI

    // // Integer Register-Register Operations
    CompressedPattern(instType = "CR", func3 = BitPat("b100"), imm3 = BitPat("b0??"), opcode = BitPat("b10")), // C_MV
    CompressedPattern(instType = "CR", func3 = BitPat("b100"), imm3 = BitPat("b1??"), opcode = BitPat("b10")), // C_ADD

    CompressedPattern(
      instType = "CA",
      func3 = BitPat("b100"),
      imm3 = BitPat("b011"),
      imm2 = BitPat("b00"),
      opcode = BitPat("b01")
    ), // C_SUB
    CompressedPattern(
      instType = "CA",
      func3 = BitPat("b100"),
      imm3 = BitPat("b011"),
      imm2 = BitPat("b00"),
      opcode = BitPat("b01")
    ), // C_XOR
    CompressedPattern(
      instType = "CA",
      func3 = BitPat("b100"),
      imm3 = BitPat("b011"),
      imm2 = BitPat("b00"),
      opcode = BitPat("b01")
    ), // C_OR
    CompressedPattern(
      instType = "CA",
      func3 = BitPat("b100"),
      imm3 = BitPat("b011"),
      imm2 = BitPat("b11"),
      opcode = BitPat("b01")
    )  // C_AND
    // CompressedPattern(func6 = BitPat("b100111"), opcode = BitPat("b01")), // C_SUBW
    // CompressedPattern(func6 = BitPat("b100111"), opcode = BitPat("b01"))
  ) // C_ADDW

  val allFields = Seq(
  )
}
