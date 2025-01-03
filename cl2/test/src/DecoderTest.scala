package cl2

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import cl2.Control._

class DecoderTest extends AnyFreeSpec {
  "docoder test" in {
    simulate(new Cl2Decoder()) { dut =>
      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      dut.io.instr.poke("h00100073".U(32.W))
      dut.io.out.isEbreak.expect(true.B)
      dut.io.instr.poke("h094040ef".U(32.W))
      dut.io.out.wbType.expect(WB_PC4)

    }
  }
}
