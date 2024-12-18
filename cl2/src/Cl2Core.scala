// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class Cl2Core extends Module {
  val io = IO(new Bundle {
    val hart_id   = Input(UInt(32.W))
    val boot_addr = Input(UInt(32.W))
  })
}
