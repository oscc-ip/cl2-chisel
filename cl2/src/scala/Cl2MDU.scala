package cl2

import chisel3._
import chisel3.util._

object MDUOpType {
  def mul    = "b000".U
  def mulh   = "b001".U
  def mulhsu = "b010".U
  def mulhu  = "b011".U
  def div    = "b100".U
  def divu   = "b101".U
  def rem    = "b110".U
  def remu   = "b111".U

  def isDiv(op: UInt) = op(2)
  def isDivSign(op: UInt) = isDiv(op) && !op(0)
}

class MDUBundle(len: Int = 32)  extends Bundle {
  val in   = Flipped(DecoupledIO(Vec(2, Output(UInt(len.W)))))
  val out  = DecoupledIO(Output(UInt((len * 2).W)))
  val sign = Input(Bool())
}

class BoothMultiplier (len: Int = 32)  extends Module {
  val io = IO(new MDUBundle(len))

  val multiplicandReg = RegInit(0.U((len * 2).W))
  val multiplierReg   = RegInit(0.U((len + 1).W)) // One more bit
  val resultReg       = RegInit(0.U((len * 2).W))

  val shiftCounter = RegInit(0.U(8.W)) // Shift counter
  val busy         = (multiplierReg =/= 0.U((len + 1).W) && shiftCounter < 16.U(8.W)).asBool

  val signedMultiplicand = Wire(SInt((len * 2).W))
  val signedMultiplier   = Wire(SInt((len + 1).W))

  when(io.sign) {
    signedMultiplicand := io.in.bits(0).asTypeOf(SInt(len.W)).asSInt.pad(len * 2)
    signedMultiplier   := io.in.bits(1).asTypeOf(SInt(len.W)).asSInt.pad(len + 1)
  } .otherwise {
    signedMultiplicand := io.in.bits(0).asUInt.pad(len * 2).asSInt
    signedMultiplier   := io.in.bits(1).asUInt.pad(len + 1).asSInt
  }

  when(io.in.valid && ~busy) {
    resultReg       := 0.U((len * 2).W)
    shiftCounter    := 0.U(8.W)
    multiplicandReg := signedMultiplicand // Signed extend to 64 bit
    multiplierReg   := Cat(signedMultiplier.asUInt, 0.U(1.W))       // Add one more 0 bit right next to it
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
  val io = IO(new MDUBundle(len))

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

class MDUIO extends Bundle{
  val in = Flipped(Decoupled(new Bundle {
    val src1 = Output(UInt(32.W))
    val src2 = Output(UInt(32.W))
    val func = Output(UInt(3.W))
  }))
  val out = Decoupled(Output(UInt(32.W)))
}

object LookupTree {
  def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
    Mux1H(mapping.map(p => (p._1 === key, p._2)))
}

class Cl2MDU extends Module{
  val io = IO(new MDUIO)

  val (valid, src1, src2, func) = (io.in.valid, io.in.bits.src1, io.in.bits.src2, io.in.bits.func)
  def access(valid: Bool, src1: UInt, src2: UInt, func: UInt): UInt = {
    this.valid := valid
    this.src1 := src1
    this.src2 := src2
    this.func := func
    io.out.bits
  }

  val isDiv = MDUOpType.isDiv(func)
  val isDivSign = MDUOpType.isDivSign(func)

  val mul = Module(new BoothMultiplier(32))
  val div = Module(new RestoringDivider(32))
  List(mul.io, div.io).map { case x =>
    x.sign := isDivSign
    x.out.ready := io.out.ready
  }

  val signext = SignExt(_: UInt, 33)
  val zeroext = ZeroExt(_: UInt, 33)
  val mulInputFuncTable = List(
    MDUOpType.mul    -> (zeroext, zeroext),
    MDUOpType.mulh   -> (signext, signext),
    MDUOpType.mulhsu -> (signext, zeroext),
    MDUOpType.mulhu  -> (zeroext, zeroext)
  )

  mul.io.in.bits(0) := LookupTree(func(1,0), mulInputFuncTable.map(p => (p._1(1,0), p._2._1(src1))))
  mul.io.in.bits(1) := LookupTree(func(1,0), mulInputFuncTable.map(p => (p._1(1,0), p._2._2(src2))))

  val divInputFunc = (x: UInt) => Mux(isDivSign, SignExt(x(31,0), 32), ZeroExt(x(31,0), 32))
  div.io.in.bits(0) := divInputFunc(src1)
  div.io.in.bits(1) := divInputFunc(src2)

  mul.io.in.valid := io.in.valid && !isDiv
  div.io.in.valid := io.in.valid && isDiv

  val mulRes = Mux(func(1,0) === MDUOpType.mul(1,0), mul.io.out.bits(31,0), mul.io.out.bits(63,32))
  val divRes = Mux(func(1) , div.io.out.bits(63,32), div.io.out.bits(31,0))
  val res = Mux(isDiv, divRes, mulRes)
  io.out.bits := res
}