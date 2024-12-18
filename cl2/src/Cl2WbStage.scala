// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class Cl2WbStage extends Module {

  val io = IO(new Bundle {
    val idEx2Wb = Flipped(Decoupled(new IdEx2WbSignals()))
    val dummy   = Input(Bool())
  })

  /* Pipeline control */

}
