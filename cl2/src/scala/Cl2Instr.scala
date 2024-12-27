// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode
import chisel3.Disable.Type

object RISCVBasicOpCode {}

object Cl2DecodeInfo {
  val possiblePatterns = Seq(
    InstructionPattern(instType = "U", opcode = BitPat("b0110111")),                         // LUI
    InstructionPattern(instType = "U", opcode = BitPat("b0010111")),                         // AUIPC
    InstructionPattern(instType = "J", opcode = BitPat("b1101111")),                         // JAL
    InstructionPattern(instType = "I", func3 = BitPat("b000"), opcode = BitPat("b1100111")), // JALR

    InstructionPattern(instType = "B", func3 = BitPat("b000"), opcode = BitPat("b1100011")),                    // BEQ
    InstructionPattern(instType = "B", func3 = BitPat("b001"), opcode = BitPat("b1100011")),                    // BNE
    InstructionPattern(instType = "B", func3 = BitPat("b100"), opcode = BitPat("b1100011")),                    // BLT
    InstructionPattern(instType = "B", func3 = BitPat("b101"), opcode = BitPat("b1100011")),                    // BGE
    InstructionPattern(instType = "B", func3 = BitPat("b110"), opcode = BitPat("b1100011")),                    // BLTU
    InstructionPattern(instType = "B", func3 = BitPat("b111"), opcode = BitPat("b1100011")),                    // BGEU
    InstructionPattern(instType = "I", func3 = BitPat("b000"), ldType = "LD_LB", opcode = BitPat("b0000011")),  // LB
    InstructionPattern(instType = "I", func3 = BitPat("b001"), ldType = "LD_LH", opcode = BitPat("b0000011")),  // LH
    InstructionPattern(instType = "I", func3 = BitPat("b010"), ldType = "LD_LW", opcode = BitPat("b0000011")),  // LW
    InstructionPattern(instType = "I", func3 = BitPat("b100"), ldType = "LD_LBU", opcode = BitPat("b0000011")), // LBU
    InstructionPattern(instType = "I", func3 = BitPat("b101"), ldType = "LD_LHU", opcode = BitPat("b0000011")), // LHU
    InstructionPattern(instType = "S", func3 = BitPat("b000"), stType = "ST_SB", opcode = BitPat("b0100011")),  // SB
    InstructionPattern(instType = "S", func3 = BitPat("b001"), stType = "ST_SH", opcode = BitPat("b0100011")),  // SB
    InstructionPattern(instType = "S", func3 = BitPat("b010"), stType = "ST_SW", opcode = BitPat("b0100011")),  // SW
    InstructionPattern(instType = "I", func3 = BitPat("b000"), opcode = BitPat("b0010011")),                    // ADDI
    InstructionPattern(instType = "I", func3 = BitPat("b010"), opcode = BitPat("b0010011")),                    // SLTI
    InstructionPattern(instType = "I", func3 = BitPat("b011"), opcode = BitPat("b0010011")),                    // SLTIU
    InstructionPattern(instType = "I", func3 = BitPat("b100"), opcode = BitPat("b0010011")),                    // XORI
    InstructionPattern(instType = "I", func3 = BitPat("b110"), opcode = BitPat("b0010011")),                    // ORI
    InstructionPattern(instType = "I", func3 = BitPat("b111"), opcode = BitPat("b0010011")),                    // ANDI
    InstructionPattern(
      instType = "I",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b001"),
      opcode = BitPat("b0010011")
    ),                                                                                                          // SLLI
    InstructionPattern(
      instType = "I",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b101"),
      opcode = BitPat("b0010011")
    ),                                                                                                          // SRLI
    InstructionPattern(
      instType = "I",
      func7 = BitPat("b0100000"),
      func3 = BitPat("b101"),
      opcode = BitPat("b0010011")
    ),                                                                                                          // SRAI
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b000"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // ADD
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0100000"),
      func3 = BitPat("b000"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // SUB
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b001"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // SLL
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b010"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // SLT
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b011"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // SLTU
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b100"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // XOR
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b101"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // SRL
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0100000"),
      func3 = BitPat("b101"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // SRA
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b110"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // OR
    InstructionPattern(
      instType = "R",
      func7 = BitPat("b0000000"),
      func3 = BitPat("b111"),
      opcode = BitPat("b0110011")
    ),                                                                                                          // AND
    InstructionPattern(instType = "R", func3 = BitPat("b000"), opcode = BitPat("b0001111")),                    // FENCE

    InstructionPattern(instType = "R", func3 = BitPat("b000"), opcode = BitPat("b1110011")) //  ECALL/EBREAK

  )

  val allFields = Seq(
    AselField,
    BselField,
    AluOpField,
    ImmSelField,
    JumpField,
    LoadField,
    StoreField,
    WBackField,
    WenField
    // IsCSRField,
    // IllegalField

  )

}
