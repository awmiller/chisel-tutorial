package examples

import Chisel._
import Chisel.testers._

class ByteSelector extends Module {
  val io = new Bundle {
    val in     = UInt(INPUT, 32)
    val offset = UInt(INPUT, 2)
    val out    = UInt(OUTPUT, 8)
  }
  io.out := UInt(0, width=8)
  when (io.offset === UInt(0, width=2)) {
    io.out := io.in(7,0)
  } .elsewhen (io.offset === UInt(1)) {
    io.out := io.in(15,8)
  } .elsewhen (io.offset === UInt(2)) {
    io.out := io.in(23,16)
  } .otherwise {
    io.out := io.in(31,24)
  }
}

class ByteSelectorUnitTester extends SteppedHWIOTester {
  val device_under_test = Module(new ByteSelector)
  val c = device_under_test

  val test_in = 12345678
  for (t <- 0 until 4) {
    poke(c.io.in, test_in)
    poke(c.io.offset, t)
    expect(c.io.out, (test_in >> (t * 8)) & 0xFF)
    step(1)
  }
}
