package examples

import Chisel._
import Chisel.hwiotesters._

object Risc {
  object Opcode extends Enumeration(1) {
    type Opcode = Value
    val add, imm = Value
  }
  import Opcode._

  def I (op: Opcode, rc: Int, ra: Int, rb: Int) = 
    op.id << 24 | rc << 16 | ra << 8 | rb
}

import Risc._

class Risc extends Module {
  
  val io = new Bundle {
    val isWr   = Bool(INPUT)
    val wrAddr = UInt(INPUT, 8)
    val wrData = Bits(INPUT, 32)
    val boot   = Bool(INPUT)
    val valid  = Bool(OUTPUT)
    val out    = Bits(OUTPUT, 32)
  }
  val file = Mem(256, Bits(width = 32))
  val code = Mem(256, Bits(width = 32))
  val pc   = Reg(init=UInt(0, 8))
  
  val inst = code(pc)
  val op   = inst(31,24)
  val rci  = inst(23,16)
  val rai  = inst(15, 8)
  val rbi  = inst( 7, 0)

  val ra = Mux(rai === Bits(0), Bits(0), file(rai))
  val rb = Mux(rbi === Bits(0), Bits(0), file(rbi))
  val rc = Wire(Bits(width = 32))

  io.valid := Bool(false)
  io.out   := Bits(0)
  rc       := Bits(0)

  when (io.isWr) {
    code(io.wrAddr) := io.wrData
  } .elsewhen (io.boot) {
    pc := UInt(0)
  } .otherwise {
    switch(op) {
      is(UInt(Opcode.add.id)) { rc := ra + rb }
      is(UInt(Opcode.imm.id)) { rc := (rai << UInt(8)) | rbi }
    }
    io.out := rc
    when (rci === UInt(255)) {
      io.valid := Bool(true)
    } .otherwise {
      file(rci) := rc
      pc := pc + UInt(1)
    }
  }
}

class RiscUnitTester extends SteppedHWIOTester {
  import Risc.Opcode._

  val device_under_test = Module(new Risc)
  val c = device_under_test

  def wr(addr: Int, data: Int) = {
    poke(c.io.isWr, 1)
    poke(c.io.wrAddr, addr)
    poke(c.io.wrData, data)
    step(1)
  }
  def boot() = {
    poke(c.io.isWr, 0)
    poke(c.io.boot, 1)
    step(1)
  }
  def tick() = {
    poke(c.io.isWr, 0)
    poke(c.io.boot, 0)
    step(1)
  }
  val app = Array(I(imm, 1, 0, 1), // r1 <- 1
    I(add, 1, 1, 1), // r1 <- r1 + r1
    I(add, 1, 1, 1), // r1 <- r1 + r1
    I(add, 255, 1, 0)) // rh <- r1
  wr(0, 0) // skip reset
  for (addr <- app.indices)
    wr(addr, app(addr))
  boot()

  for (instruction <- app) {
    tick()
  }
  expect(c.io.valid, 1)
  expect(c.io.out, 4)

  //  var k = 0
  //  do {
  //    when(c.io.valid) {
  //      printf("io.valid, io.out %d\n", c.io.out)
  //      setDone := Bool(true)
  ////      stop(0)
  //    } otherwise {
  //      tick(); k += 1
  //    }
  //  } while (/*peek(c.io.valid) === Bool(false) &&*/ k < 10)
  ////  expect(k < 10, "TIME LIMIT")
  //  expect(c.io.out, 4)
}
