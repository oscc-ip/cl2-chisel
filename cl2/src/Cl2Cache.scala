// SPDX-License-Identifier: MulanPSL-2.0

package cl2

import chisel3._
import chisel3.util._

class MemRdInput extends Bundle {
  val raddr = Input(UInt(32.W))
  val ren   = Input(Bool())
  val rlen  = Input(UInt(4.W))
}

class MemRdOutput extends Bundle {
  val rdata = Output(UInt(32.W))
  val rresp = Output(UInt(4.W))
}

class MemWrInput extends Bundle {
  val waddr = Input(UInt(32.W))
  val wen   = Input(Bool())
  val wlen  = Input(UInt(3.W))
  val wdata = Input(UInt(32.W))
}

class MemWrOutput extends Bundle {
  val wresp = Output(UInt(4.W))
}

//Not a cache now, just for testing.
class icacheHelper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val in    = new MemRdInput
    val out   = new MemRdOutput
    val clock = Input(Clock())
    val reset = Input(Bool())
  })
  setInline(
    "icacheHelper.v",
    """module icacheHelper(
      |  input [31:0] in_raddr,
      |  input [3:0] in_rlen,
      |  input in_ren,
      |  output reg [31:0] out_rdata,
      |  output [3:0] out_resp,
      |  input reset,
      |  input clock
      |);
      |import "DPI-C" function int unsigned inst_fetch(input int unsigned raddr, int unsigned len);
      |always @(posedge clock) begin
      |  if(reset)
      |      out_rdata = 0;
      |  else begin
      |    if (in_ren) out_rdata = inst_fetch(in_raddr, in_rlen);
      |    else out_rdata = 0;
      |  end
      |end
      |endmodule
        """.stripMargin
  )

}

class dcacheHelper extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val rdIn  = new MemRdInput
    val rdOut = new MemRdOutput
    val wrIn  = new MemWrInput
    val wrOut = new MemWrOutput
    val reset = Input(Bool())
    val clock = Input(Clock())
    setInline(
      "dcacheHelper.v",
      """module dcacheHelper(
        |  input [31:0] rdIn_raddr,
        |  input rdIn_ren,
        |  input [3:0] rdIn_rlen,
        |  output reg [31:0] rdOut_rdata,
        |  output [3:0] rdOut_rresp,
        |  input [31:0] wrIn_waddr,
        |  input wrIn_wen,
        |  input [3:0] wrIn_wlen,
        |  input [31:0] wrIn_wdata,
        |  output [3:0] wrOut_wresp,
        |  input reset,
        |  input clock
        |);
        |import "DPI-C" function int unsigned mem_read(input int unsigned raddr, int unsigned len);
        |import "DPI-C" function void mem_write(input int unsigned addr, input int unsigned len, input int unsigned data);
        | assign rdOut_rresp = 0;
        | assign wrOut_wresp = 0;
        |always @(posedge clock) begin
        |  if(reset)
        |    rdOut_rdata = 0;
        |  else begin
        |    if(rdIn_ren) rdOut_rdata = mem_read(rdIn_raddr, rdIn_rlen);
        |    else rdOut_rdata = 0;
        |  end
        |end
        |
        |always @(posedge clock) begin
        |  if(wrIn_wen) mem_write(wrIn_waddr, wrIn_wlen, wrIn_wdata);
        |end
        |endmodule
        """.stripMargin
    )
  })

}

//Can we use DecoupledIO here ?
class SimpleUglyArbiter extends Module {
  val grp0 = IO(new Bundle {
    val in  = Flipped(Decoupled(new Bundle {
      val rd = new MemRdInput
      val wr = new MemWrInput
    }))
    val out = Decoupled(new Bundle {
      val rd = new MemRdOutput
      val wr = new MemWrOutput
    })
  })

  val grp1 = IO(new Bundle {
    val in  = Flipped(Decoupled(new Bundle {
      val rd = new MemRdInput
      val wr = new MemWrInput
    }))
    val out = Decoupled(new Bundle {
      val rd = new MemRdOutput
      val wr = new MemWrOutput
    })
  })

  val outGrp = IO(new Bundle {
    val out = Decoupled(new Bundle {
      val rd = Flipped(new MemRdInput)
      val wr = Flipped(new MemWrInput)
    })

    val in = Flipped(Decoupled(new Bundle {
      val rd = Flipped(new MemRdOutput)
      val wr = Flipped(new MemWrOutput)
    }))
  })
  dontTouch(grp0)
  dontTouch(grp1)
  dontTouch(outGrp)

  // Select the signal that was not chosen last time
  // -- grp0 -- grp1 -- | -- choice -- |
  //     0       0             0       |
  //     0       1             1       |
  //     1       0             2       |
  //     1       1             3       |
  // -----------------------------------|
  val last   = RegEnable(0.U(1.W), grp0.out.fire || grp1.out.fire)
  val choice = Cat(grp0.in.valid, grp1.in.valid)

  // Can we use combinational logic to do this ?
  // See chisel3 Arbiter implementation.
  // 0 is grp0, 1 is grp1
  val (idle, bypassing0, bypassing1) = (0.U, 1.U, 2.U)

  val state = RegInit(idle)
  state := MuxLookup(state, idle)(
    Seq(
      idle       -> MuxLookup(choice, idle)(
        Seq(
          0.U -> idle,
          1.U -> bypassing1,
          2.U -> bypassing0,
          3.U -> Mux(last === 0.U, bypassing1, bypassing0)
        )
      ),
      bypassing0 -> Mux(grp0.out.fire, Mux(grp1.in.valid, bypassing1, idle), bypassing0),
      bypassing1 -> Mux(grp1.out.fire, Mux(grp0.in.valid, bypassing0, idle), bypassing1)
    )
  )

  last := MuxLookup(state, 0.U)(
    Seq(
      bypassing1 -> 1.U,
      bypassing0 -> 0.U
    )
  )

  when(state === idle) {
    grp0.in.ready  := (false.B)
    grp0.out.bits  := outGrp.in.bits
    grp0.out.valid := false.B

    grp1.in.ready  := false.B
    // Pay attention here, Can I use outGrp twice ?
    grp1.out.bits  := outGrp.in.bits
    grp1.out.valid := false.B

    outGrp.out.valid := false.B
    outGrp.out.bits  := grp0.in.bits
    outGrp.in.ready  := false.B

  }.elsewhen(state === bypassing0) {
    outGrp.out <> grp0.in
    outGrp.in <> grp0.out
    grp1.in.ready  := false.B
    grp1.out.valid := false.B
    grp1.out.bits  := outGrp.in.bits

  }.elsewhen(state === bypassing1) {
    outGrp.out <> grp1.in
    outGrp.in <> grp1.out
    grp0.out.valid := false.B
    grp0.in.ready  := false.B
    grp0.out.bits  := outGrp.in.bits
  }.otherwise {
    grp0.in.ready  := (false.B)
    grp0.out.bits  := outGrp.in.bits
    grp0.out.valid := false.B

    grp1.in.ready  := false.B
    // Pay attention here, Can I use outGrp twice ?
    grp1.out.bits  := outGrp.in.bits
    grp1.out.valid := false.B

    outGrp.out.valid := false.B
    outGrp.out.bits  := grp0.in.bits
    outGrp.in.ready  := false.B

  }
}

class SimpletoAXI extends Module {
  val simple = IO(new Bundle {
    val in = Flipped(Decoupled(new Bundle {
      val rd = new MemRdInput
      val wr = new MemWrInput
    }))

    val out = Decoupled(new Bundle {
      val rd = new MemRdOutput
      val wr = new MemWrOutput
    })
  })

  val axi = IO(new SimpleAXI4Bundle)
  dontTouch(axi)

  // assert(simple.in.bits.rd.ren ^ simple.in.bits.wr.wen)
  val valid = simple.in.valid
  // AW channel
  axi.awvalid := Mux(valid, Mux(simple.in.bits.wr.wen, true.B, false.B), false.B)

  // We make this address be aligned with data width to deal with unalign transfer with sram. But the apb interface use an unaligned address to get byte lane...So we add a mux here.
  // UART: 0x10000000 ~ 0x10000fff
  // SPI: 0x10001000 ~ 0x10001fff
  val waddr = simple.in.bits.wr.waddr
  axi.awaddr  := Mux(waddr(31, 28) === "h1".U, waddr, waddr & "hfffffff8".U)
  axi.awid    := 0.U
  axi.awlen   := 0.U
  axi.awsize  := MuxLookup(simple.in.bits.wr.wlen, 0.U)(
    Seq(
      1.U -> 0.U,
      2.U -> 1.U,
      4.U -> 2.U
    )
  )
  axi.awburst := 1.U // INCR, but we have only one transfer.

  val awfire = axi.awvalid && axi.awready
  // W channel
  axi.wvalid := Mux(valid, Mux(simple.in.bits.wr.wen, true.B, false.B), false.B)

  val data       = simple.in.bits.wr.wdata
  val least3bits = simple.in.bits.wr.waddr(2, 0)
  axi.wdata := MuxLookup(simple.in.bits.wr.wlen, 0.U)(
    Seq(
      1.U -> MuxLookup(least3bits, 0.U)(
        Seq(
          0.U -> Cat(0.U(56.W), data(7, 0)),
          1.U -> Cat(0.U(48.W), data(7, 0), 0.U(8.W)),
          2.U -> Cat(0.U(40.W), data(7, 0), 0.U(16.W)),
          3.U -> Cat(0.U(32.W), data(7, 0), 0.U(24.W)),
          4.U -> Cat(0.U(24.W), data(7, 0), 0.U(32.W)),
          5.U -> Cat(0.U(16.W), data(7, 0), 0.U(40.W)),
          6.U -> Cat(0.U(8.W), data(7, 0), 0.U(48.W)),
          7.U -> Cat(data(7, 0), 0.U(56.W))
        )
      ),
      2.U -> MuxLookup(least3bits, 0.U)(
        Seq(
          0.U -> Cat(0.U(48.W), data(15, 0)),
          2.U -> Cat(0.U(32.W), data(15, 0), 0.U(16.W)),
          4.U -> Cat(0.U(16.W), data(15, 0), 0.U(32.W)),
          6.U -> Cat(data(15, 0), 0.U(48.W))
        )
      ),
      4.U -> MuxLookup(least3bits, 0.U)(
        Seq(
          0.U -> Cat(0.U(32.W), data(31, 0)),
          4.U -> Cat(data(31, 0), 0.U(32.W))
        )
      )
    )
  )

  axi.wstrb := MuxLookup(simple.in.bits.wr.wlen, 0.U)(
    Seq(
      1.U -> MuxLookup(least3bits, 0.U)(
        Seq(
          0.U -> 1.U,
          1.U -> 2.U,
          2.U -> 4.U,
          3.U -> 8.U,
          4.U -> 16.U,
          5.U -> 32.U,
          6.U -> 64.U,
          7.U -> 128.U
        )
      ),
      2.U -> MuxLookup(least3bits, 0.U)(
        Seq(
          0.U -> "h03".U,
          2.U -> "h0c".U,
          4.U -> "h30".U,
          6.U -> "hc0".U
        )
      ),
      4.U -> MuxLookup(least3bits, 0.U)(
        Seq(
          0.U -> "h0f".U,
          4.U -> "hf0".U
        )
      )
    )
  )

  axi.wlast := true.B

  // B channel
  axi.bready := simple.out.ready

  // AR channel
  axi.arvalid := Mux(valid, Mux(simple.in.bits.rd.ren, true.B, false.B), false.B)

  // Note that we might use unaligned tranfer here.
  axi.araddr  := simple.in.bits.rd.raddr
  axi.arid    := 0.U
  axi.arlen   := 0.U
  axi.arsize  := MuxLookup(simple.in.bits.rd.rlen, 0.U)(
    Seq(
      1.U -> 0.U,
      2.U -> 1.U,
      4.U -> 2.U
    )
  ) // Pay attention here */
  axi.arburst := 1.U // INCR, but we have only one transfer.

  // R channel
  axi.rready := simple.out.ready

  simple.in.ready  := (axi.awready && axi.wready) || axi.arready // Pay attention here.
  simple.out.valid := axi.bvalid || axi.rvalid

  val arfire = axi.arvalid & axi.arready

  // The data read from mrom does not comform to the AXI standard, we add a simple register here to indicate whether mrom is being read.
  // Note that the address space of mrom may change.
  val is_mrom = RegEnable(false.B, arfire)
  // mrom: 0x20000000 ~ 0x20000fff
  is_mrom := (simple.in.bits.rd.raddr(31, 28) === 2.U)

  val raddr_l3bs = RegEnable(simple.in.bits.rd.raddr(2, 0), 0.U, arfire)
  val rlen       = RegEnable(simple.in.bits.rd.rlen, 0.U, arfire)
  val sram_rdata = MuxLookup(rlen, 0.U)(
    Seq(
      1.U -> MuxLookup(raddr_l3bs, 0.U)(
        Seq(
          // Will zero extension be done automatically here ?
          0.U -> Cat(0.U(24.W), axi.rdata(7, 0)),
          1.U -> Cat(0.U(24.W), axi.rdata(15, 8)),
          2.U -> Cat(0.U(24.W), axi.rdata(23, 16)),
          3.U -> Cat(0.U(24.W), axi.rdata(31, 24)),
          4.U -> Cat(0.U(24.W), axi.rdata(39, 32)),
          5.U -> Cat(0.U(24.W), axi.rdata(47, 40)),
          6.U -> Cat(0.U(24.W), axi.rdata(55, 48)),
          7.U -> Cat(0.U(24.W), axi.rdata(63, 56))
        )
      ),
      2.U -> MuxLookup(raddr_l3bs, 0.U)(
        Seq(
          0.U -> Cat(0.U(16.W), axi.rdata(15, 0)),
          2.U -> Cat(0.U(16.W), axi.rdata(31, 16)),
          4.U -> Cat(0.U(16.W), axi.rdata(47, 32)),
          6.U -> Cat(0.U(16.W), axi.rdata(63, 48))
        )
      ),
      4.U -> MuxLookup(raddr_l3bs, 0.U)(
        Seq(
          0.U -> axi.rdata(31, 0),
          4.U -> axi.rdata(63, 32)
        )
      )
    )
  )
  val mrom_rdata = MuxLookup(rlen, 0.U)(
    Seq(
      1.U -> axi.rdata(7, 0),
      2.U -> axi.rdata(15, 0),
      4.U -> axi.rdata(31, 0)
    )
  )

  simple.out.bits.rd.rdata := Mux(is_mrom, mrom_rdata, sram_rdata)
  simple.out.bits.rd.rresp := 0.U
  simple.out.bits.wr.wresp := axi.bresp

}
