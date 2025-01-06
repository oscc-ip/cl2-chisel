package utils

import chisel3._
import chisel3.util._

class SRAMBundleA(val set: Int) extends Bundle {
  val setIdx = Output(UInt(log2Up(set).W))

  def apply(setIdx: UInt) = {
    this.setIdx := setIdx
    this
  }
}

class SRAMBundleAW[T <: Data](private val gen: T, set: Int, val way: Int = 1) extends SRAMBundleA(set) {
  val data    = Output(gen)
  val waymask = if (way > 1) Some(Output(UInt(way.W))) else None

  def apply(data: T, setIdx: UInt, waymask: UInt) = {
    super.apply(setIdx)
    this.data := data
    this.waymask.map(_ := waymask)
    this
  }
}

class SRAMBundleR[T <: Data](private val gen: T, val way: Int = 1) extends Bundle {
  val data = Output(Vec(way, gen))
}

class SRAMReadBus[T <: Data](private val gen: T, val set: Int, val way: Int = 1) extends Bundle {
  val req  = Decoupled(new SRAMBundleA(set))
  val resp = Flipped(new SRAMBundleR(gen, way))

  def apply(valid: Bool, setIdx: UInt) = {
    this.req.bits.apply(setIdx)
    this.req.valid := valid
    this
  }
}

class SRAMWriteBus[T <: Data](private val gen: T, val set: Int, val way: Int = 1) extends Bundle {
  val req = Decoupled(new SRAMBundleAW(gen, set, way))

  def apply(valid: Bool, data: T, setIdx: UInt, waymask: UInt) = {
    this.req.bits.apply(data = data, setIdx = setIdx, waymask = waymask)
    this.req.valid := valid
    this
  }
}

class SRAMTemplate[T <: Data](
  gen:         T,
  set:         Int,
  way:         Int = 1,
  shouldReset: Boolean = false,
  holdRead:    Boolean = false,
  singlePort:  Boolean = false)
    extends Module {
  val io = IO(new Bundle {
    val r = Flipped(new SRAMReadBus(gen, set, way))
    val w = Flipped(new SRAMWriteBus(gen, set, way))
  })

  val wordType               = UInt(gen.getWidth.W)
  val array                  = SyncReadMem(set, Vec(way, wordType))
  val (resetState, resetSet) = (WireInit(false.B), WireInit(0.U))

  if (shouldReset) {
    val _resetState              = RegInit(true.B)
    val (_resetSet, resetFinish) = Counter(_resetState, set)
    when(resetFinish) { _resetState := false.B }

    resetState := _resetState
    resetSet   := _resetSet
  }

  val (ren, wen) = (io.r.req.valid, io.w.req.valid || resetState)
  val realRen    = (if (singlePort) ren && !wen else ren)

  val setIdx    = Mux(resetState, resetSet, io.w.req.bits.setIdx)
  val wdataword = Mux(resetState, 0.U.asTypeOf(wordType), io.w.req.bits.data.asUInt)
  val waymask   = Mux(resetState, Fill(way, "b1".U), io.w.req.bits.waymask.getOrElse("b1".U))
  val wdata     = VecInit(Seq.fill(way)(wdataword))
  when(wen) { array.write(setIdx, wdata, waymask.asBools) }

  val rdata = (if (holdRead) ReadAndHold(array, io.r.req.bits.setIdx, realRen)
               else array.read(io.r.req.bits.setIdx, realRen)).map(_.asTypeOf(gen))
  io.r.resp.data := VecInit(rdata)

  io.r.req.ready := !resetState && (if (singlePort) !wen else true.B)
  io.w.req.ready := true.B
}

class SRAMTemplateWithArbiter[T <: Data](nRead: Int, gen: T, set: Int, way: Int = 1, shouldReset: Boolean = false)
    extends Module {
  val io = IO(new Bundle {
    val r = Flipped(Vec(nRead, new SRAMReadBus(gen, set, way)))
    val w = Flipped(new SRAMWriteBus(gen, set, way))
  })

  val ram = Module(new SRAMTemplate(gen, set, way, shouldReset, holdRead = false, singlePort = true))
  ram.io.w <> io.w

  val readArb = Module(new Arbiter(chiselTypeOf(io.r(0).req.bits), nRead))
  readArb.io.in <> io.r.map(_.req)
  ram.io.r.req <> readArb.io.out

  // latch read results
  io.r.map {
    case r => {
      r.resp.data := HoldUnless(ram.io.r.resp.data, RegNext(r.req.fire))
    }
  }
}
