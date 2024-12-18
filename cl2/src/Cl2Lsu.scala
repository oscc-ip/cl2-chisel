// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class SimpleMemInterface extends Bundle {
  val rdin  = new MemRdInput()
  val rdOut = new MemRdOutput()
  val wrIn  = new MemWrInput()
  val wrOut = new MemWrOutput()

}

class Cl2Lsu extends Module {
  val io = IO(new Bundle {
    val simple = new SimpleMemInterface()
  })

}
