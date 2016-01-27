package solutions

import Chisel._
import Chisel.testers.SteppedHWIOTester

class Accumulator extends Module {
  val io = new Bundle {
    val in  = UInt(width = 1, dir = INPUT)
    val out = UInt(width = 8, dir = OUTPUT)
  }
  val accumulator = Reg(init=UInt(0, 8))
  accumulator := accumulator + io.in
  io.out := accumulator
}

class AccumulatorTests extends SteppedHWIOTester {
  val device_under_test = Module(new Accumulator)
  val c = device_under_test
  var tot = 0

  testBlock {
    for (t <- 0 until 16) {
      val in = rnd.nextInt(2)
      poke(c.io.in, in)
      step(1)
      if (in == 1) tot += 1
      expect(c.io.out, tot)
    }
  }
}
