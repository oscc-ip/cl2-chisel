// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._

class SimpleAXI4Bundle extends Bundle {
  val awvalid = Output(Bool())
  val awready = Input(Bool())
  val awaddr  = Output(UInt(32.W))
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  val wvalid = Output(Bool())
  val wready = Input(Bool())
  val wdata  = Output(UInt(64.W))
  val wstrb  = Output(UInt(8.W))
  val wlast  = Output(Bool())

  val bvalid = Input(Bool())
  val bready = Output(Bool())
  val bresp  = Input(UInt(2.W))
  val bid    = Input(UInt(4.W))

  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(32.W))
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))

  val rvalid = Input(Bool())
  val rready = Output(Bool())
  val rresp  = Input(UInt(2.W))
  val rdata  = Input(UInt(64.W))
  val rlast  = Input(Bool())
  val rid    = Input(UInt(4.W))
}
