package cl2

import chisel3._
import chisel3.util._

class DivArithBundle(len: Int = 32)  extends Bundle {
  val in   = Flipped(DecoupledIO(Vec(2, Output(UInt(len.W)))))
  val out  = DecoupledIO(Output(UInt((len * 2).W)))
  val sign = Input(Bool())
}
class MultArithBundle(len: Int = 32) extends Bundle {
  val in  = Flipped(DecoupledIO(Vec(2, Output(SInt(len.W)))))
  val out = DecoupledIO(Output(SInt((2 * len).W)))
}
class BoothMultiplier                extends Module {
  val io = IO(new MultArithBundle(32))

  val multiplicandReg = RegInit(0.U(64.W))
  val multiplierReg   = RegInit(0.U(33.W)) // One more bit
  val resultReg       = RegInit(0.U(64.W))

  val shiftCounter = RegInit(0.U(8.W)) // Shift counter
  val busy         = (multiplierReg =/= 0.U(33.W) && shiftCounter < 16.U(8.W)).asBool

  when(io.in.valid && ~busy) {
    resultReg       := 0.U(64.W)
    shiftCounter    := 0.U(8.W)
    multiplicandReg := io.in.bits(0).asTypeOf(SInt(64.W)).asUInt // Signed extend to 64 bit
    multiplierReg   := Cat(io.in.bits(1).asUInt, 0.U(1.W))       // Add one more 0 bit right next to it
  }.otherwise {
    when(busy) {
      resultReg       := resultReg + MuxCase(
        0.U(64.W),
        Seq(
          (multiplierReg(2, 0) === "b000".U) -> 0.U(64.W),
          (multiplierReg(2, 0) === "b001".U) -> multiplicandReg,
          (multiplierReg(2, 0) === "b010".U) -> multiplicandReg,
          (multiplierReg(2, 0) === "b011".U) -> (multiplicandReg << 1.U),
          (multiplierReg(2, 0) === "b100".U) -> (-(multiplicandReg << 1.U)),
          (multiplierReg(2, 0) === "b101".U) -> (-multiplicandReg),
          (multiplierReg(2, 0) === "b110".U) -> (-multiplicandReg),
          (multiplierReg(2, 0) === "b111".U) -> 0.U(64.W)
        )
      )
      multiplicandReg := multiplicandReg << 2.U
      multiplierReg   := multiplierReg >> 2.U
      shiftCounter    := shiftCounter + 1.U(8.W)
    }.otherwise {
      resultReg       := resultReg
      multiplicandReg := multiplicandReg
      multiplierReg   := multiplierReg
      shiftCounter    := shiftCounter
    }
  }
  // Pay attention here !!
  io.out.bits  := resultReg.asSInt
  io.out.valid := !busy
  io.in.ready  := !busy
}

class RestoringDivider(len: Int = 32) extends Module {
  val io = IO(new DivArithBundle(len))

  def abs(a: UInt, sign: Bool): (Bool, UInt) = {
    val s = a(len - 1) && sign
    (s, Mux(s, -a, a))
  }

  val s_idle :: s_log2 :: s_shift :: s_compute :: s_finish :: Nil = Enum(5)
  val state                                                       = RegInit(s_idle)
  val newReq                                                      = (state === s_idle) && io.in.fire

  val (dividend, divisor) = (io.in.bits(0), io.in.bits(1))
  val divBy0              = divisor === 0.U(len.W)

  val shiftReg = Reg(UInt((1 + len * 2).W))
  val hi       = shiftReg(len * 2, len)
  val lo       = shiftReg(len - 1, 0)

  val (dividendSign, dividendVal) = abs(dividend, io.sign)
  val (divisorSign, divisorVal)   = abs(divisor, io.sign)
  val dividendSignReg             = RegEnable(dividendSign, newReq)
  val quotientSignReg             = RegEnable((dividendSign ^ divisorSign) && !divBy0, newReq)
  val divisorReg                  = RegEnable(divisorVal, newReq)
  val dividendValx2Reg            = RegEnable(Cat(dividendVal, "b0".U), newReq)

  val cnt = Counter(len)
  when(newReq) {
    state := s_log2
  }.elsewhen(state === s_log2) {
    val canSkipShift = (len.U | Log2(divisorReg)) - Log2(dividendValx2Reg)
    cnt.value := Mux(divBy0, 0.U, Mux(canSkipShift >= (len - 1).U, (len - 1).U, canSkipShift))
    state     := s_shift
  }.elsewhen(state === s_shift) {
    shiftReg := dividendValx2Reg << cnt.value
    state    := s_compute
  }.elsewhen(state === s_compute) {
    val enough = hi.asUInt >= divisorReg.asUInt
    shiftReg := Cat(Mux(enough, hi - divisorReg, hi)(len - 1, 0), lo, enough)
    cnt.inc()
    when(cnt.value === (len - 1).U) { state := s_finish }
  }.elsewhen(state === s_finish) {
    state := s_idle
  }

  val r         = hi(len, 1)
  val Quotient  = Mux(quotientSignReg, -lo, lo)
  val Remainder = Mux(dividendSignReg, -r, r)
  io.out.bits := Cat(Remainder, Quotient)

  io.out.valid := state === s_finish
  io.in.ready  := (state === s_idle)
}
