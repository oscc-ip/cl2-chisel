// package cl2

// import chisel3._
// import chisel3.util._

// import Control._

// class BPU extends Module {
//   val io = IO(new Bundle {
//     val inst = Input(UInt(32.W))
//     val snpc = Input(UInt(32.W))
//     val target = Output(UInt(32.W))
//     val BValid = Output(Bool())
//   })

//   val instr = io.inst
//   val immJ = SignExt(Cat(instr(31), instr(19, 12), instr(20), instr(30, 21), 0.U(1.W)), 32)
//   val immB = SignExt(Cat(instr(31), instr(7), instr(30, 25), instr(11, 8), 0.U(1.W)), 32)
//   val offset = MuxLookup(Cl2Decoder().io.out.jType, 0.U)(
//     Seq(
//       J_XXX  -> 0.U,
//       J_EQ   -> IMM_B,
//       J_NE   -> IMM_B,
//       J_LT   -> IMM_B,
//       J_LTU  -> IMM_B,
//       J_GE   -> IMM_B,
//       J_GEU  -> IMM_B,
//       J_JAL  -> IMM_J
//     )
//   )
  
//   val predict = MuxLookup(Cl2Decoder().io.out.jType, false.B)(
//     Seq(
//       J_XXX -> false.B,
//       J_EQ  -> instr(31),
//       J_NE  -> instr(31),
//       J_LT  -> instr(31),
//       J_LTU -> instr(31),
//       J_GE  -> instr(31),
//       J_GEU -> instr(31)
//     )
//   )

//   io.target := io.snpc + offset
//   io.BValid := predict
// }