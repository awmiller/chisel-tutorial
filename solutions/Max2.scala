package solutions

import Chisel._
import Chisel.hwiotesters.SteppedHWIOTester

class Max2 extends Module {
  val io = new Bundle {
    val in0 = UInt(INPUT,  8)
    val in1 = UInt(INPUT,  8)
    val out = UInt(OUTPUT, 8)
  }
  io.out := Mux(io.in0 > io.in1, io.in0, io.in1)
}

class Max2Tests extends SteppedHWIOTester {
  val device_under_test = Module(new Max2)
  val c = device_under_test

  for (i <- 0 until 10) {
    // FILL THIS IN HERE
    val in0 = rnd.nextInt(256)
    val in1 = rnd.nextInt(256)
    poke(c.io.in0, in0)
    poke(c.io.in1, in1)
    // FILL THIS IN HERE
    expect(c.io.out, if (in0 > in1) in0 else in1)
    step(1)
  }
}
