// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode
import chisel3.Disable.Type

object Cl2PreDecodeInfo    {
  val possiblePatterns = Seq(
    InstructionPattern(instType = "C", func3 = BitPat("b000"), opcode = BitPat("b00")),         //C_ILLEGAL 
    InstructionPattern(instType = "C", func3 = BitPat("b000"), opcode = BitPat("b01")),         //C_NOP
    InstructionPattern(instType = "C", func4 = BitPat("b1001"), opcode = BitPat("b10")),        //C_EBREAK  

    //Register-Based Stores and Loads   
    InstructionPattern(instType = "C", func3 = BitPat("b001"), opcode = BitPat("b00")),         //C_FLD 
    InstructionPattern(instType = "C", func3 = BitPat("b010"), opcode = BitPat("b00")),         //C_LW
    InstructionPattern(instType = "C", func3 = BitPat("b011"), opcode = BitPat("b00")),         //C_LD

    InstructionPattern(instType = "C", func3 = BitPat("b101"), opcode = BitPat("b00")),         //C_FSD
    InstructionPattern(instType = "C", func3 = BitPat("b110"), opcode = BitPat("b00")),         //C_SW
    InstructionPattern(instType = "C", func3 = BitPat("b111"), opcode = BitPat("b00")),         //C_SD
    //Stack-Pointer-Based Stores and Loads          
    InstructionPattern(instType = "C", func3 = BitPat("b101"), opcode = BitPat("b10")),         //C_FSDSP   
    InstructionPattern(instType = "C", func3 = BitPat("b110"), opcode = BitPat("b10")),         //C_SWSP
    InstructionPattern(instType = "C", func3 = BitPat("b111"), opcode = BitPat("b10")),         //C_FSWSP
    InstructionPattern(instType = "C", func3 = BitPat("b111"), opcode = BitPat("b10")),         //C_SDSP

    InstructionPattern(instType = "C", func3 = BitPat("b001"), opcode = BitPat("b10")),         //C_FLDSP
    InstructionPattern(instType = "C", func3 = BitPat("b010"), opcode = BitPat("b10")),         //C_LWSP
    InstructionPattern(instType = "C", func3 = BitPat("b011"), opcode = BitPat("b10")),         //C_FLWSP   
    InstructionPattern(instType = "C", func3 = BitPat("b011"), opcode = BitPat("b10")),         //C_LDSP  
    //Control Transfer      
    InstructionPattern(instType = "C", func3 = BitPat("b101"), opcode = BitPat("b01")),         //C_J
    InstructionPattern(instType = "C", func3 = BitPat("b001"), opcode = BitPat("b01")),         //C_JAL 
    InstructionPattern(instType = "C", func4 = BitPat("b1000"), opcode = BitPat("b10")),        //C_JR 
    InstructionPattern(instType = "C", func4 = BitPat("b1001"), opcode = BitPat("b10")),        //C_JALR  

    InstructionPattern(instType = "C", func3 = BitPat("b110"), opcode = BitPat("b01")),         //C_BEQZ
    InstructionPattern(instType = "C", func3 = BitPat("b111"), opcode = BitPat("b01")),         //C_BNEZ
    //Integer Constant-Generation       
    InstructionPattern(instType = "C", func3 = BitPat("b010"), opcode = BitPat("b01")),         //C_LI
    InstructionPattern(instType = "C", func3 = BitPat("b011"), opcode = BitPat("b01")),         //C_LUI
    //Integer Register-Immediate Operations     
    InstructionPattern(instType = "C", func3 = BitPat("b000"), opcode = BitPat("b01")),         //C_ADDI        
    InstructionPattern(instType = "C", func3 = BitPat("b001"), opcode = BitPat("b01")),         //C_ADDIW 
    InstructionPattern(instType = "C", func3 = BitPat("b011"), opcode = BitPat("b01")),         //C_ADDI16SP   
    InstructionPattern(instType = "C", func3 = BitPat("b000"), opcode = BitPat("b00")),         //C_ADDI4SPN

    InstructionPattern(instType = "C", func3 = BitPat("b000"), opcode = BitPat("b10")),         //C_SLLI
    InstructionPattern(instType = "C", func3 = BitPat("b100"), opcode = BitPat("b01")),         //C_SRLI    
    InstructionPattern(instType = "C", func3 = BitPat("b100"), opcode = BitPat("b01")),         //C_SRAI
    InstructionPattern(instType = "C", func3 = BitPat("b100"), opcode = BitPat("b01")),         //C_ANDI

    //Integer Register-Register Operations  
    InstructionPattern(instType = "C", func4 = BitPat("b1000"), opcode = BitPat("b10")),        //C_MV
    InstructionPattern(instType = "C", func4 = BitPat("b1001"), opcode = BitPat("b10")),        //C_ADD  

    InstructionPattern(instType = "C", func6 = BitPat("b100011"), opcode = BitPat("b01")),      //C_SUB
    InstructionPattern(instType = "C", func6 = BitPat("b100011"), opcode = BitPat("b01")),      //C_XOR
    InstructionPattern(instType = "C", func6 = BitPat("b100011"), opcode = BitPat("b01")),      //C_OR
    InstructionPattern(instType = "C", func6 = BitPat("b100011"), opcode = BitPat("b01")),      //C_AND
    InstructionPattern(instType = "C", func6 = BitPat("b100111"), opcode = BitPat("b01")),      //C_SUBW
    InstructionPattern(instType = "C", func6 = BitPat("b100111"), opcode = BitPat("b01")))      //C_ADDW

    val allFields = Seq(
        TestField
  )
}
