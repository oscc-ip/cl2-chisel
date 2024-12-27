// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

import Control._

class Cl2Lsu extends Module {
  val io = IO(new Bundle {
    val memReq  = Flipped(Decoupled(Input(new SimpleMemReq(32))))
    val memResp = Decoupled(Output(new SimpleMemResp(32)))

    val ldType = Input(UInt(3.W))
    val stType = Input(UInt(2.W))
  })

  /* We do some transfer here, such as Simple to AXI and Simple to Simple */

  val dcache_helper = Module(new cache_helper)

  val addr  = io.memReq.bits.addr
  val wdata = io.memReq.bits.wdata
  val wen   = io.memReq.bits.wen
  val mask  = io.memReq.bits.mask

  val err   = dcache_helper.io.out.bits.err
  val rdata = dcache_helper.io.out.bits.rdata

  /* Simple to Simple */
  dcache_helper.io.reset         := reset
  dcache_helper.io.clock         := clock
  dcache_helper.io.in.bits.addr  := addr
  dcache_helper.io.in.bits.mask  := MuxLookup(io.stType, 15.U)(
    Seq(
      ST_SB  -> 1.U,
      ST_SH  -> 3.U,
      ST_SW  -> 15.U,
      ST_XXX -> 15.U
    )
  )
  dcache_helper.io.in.bits.wdata := wdata
  dcache_helper.io.in.bits.wen   := Mux(io.stType === ST_XXX, false.B, true.B)

  dcache_helper.io.in.valid := io.memReq.valid
  io.memReq.ready           := dcache_helper.io.in.ready

  io.memResp.bits.err        := err
  io.memResp.bits.rdata      := MuxLookup(io.ldType, 0.U)(
    Seq(
      LD_LB  -> SignExt(rdata(7, 0), 32),
      LD_LBU -> ZeroExt(rdata(7, 0), 32),
      LD_LH  -> SignExt(rdata(15, 0), 32),
      LD_LHU -> SignExt(rdata(15, 0), 32),
      LD_LW  -> rdata,
      LD_XXX -> 0.U
    )
  )
  io.memResp.valid           := dcache_helper.io.out.valid
  dcache_helper.io.out.ready := io.memResp.ready

}
