package solutions

import Chisel._
import Chisel.testers.SteppedHWIOTester

class VendingMachine extends Module {
  val io = new Bundle {
    val nickel = Bool(INPUT)
    val dime   = Bool(INPUT)
    val valid  = Bool(OUTPUT) }
  val sIdle :: s5 :: s10 :: s15 :: sOk :: Nil =
    Enum(UInt(), 5)
  val state = Reg(init = sIdle)
  when (state === sIdle) {
    when (io.nickel) { state := s5 }
    when (io.dime)   { state := s10 }
  }
  when (state === s5) {
    when (io.nickel) { state := s10 }
    when (io.dime)   { state := s15 }
  }
  when (state === s10) {
    when (io.nickel) { state := s15 }
    when (io.dime)   { state := sOk }
  }
  when (state === s15) {
    when (io.nickel) { state := sOk }
    when (io.dime)   { state := sOk }
  }
  when (state === sOk) {
    state := sIdle
  }
  io.valid := (state === sOk)
}

class VendingMachineTests extends SteppedHWIOTester {
  val c = Module(new VendingMachine)
  var money = 0
  var isValid = 0
  for (t <- 0 until 20) {
    val coin     = rnd.nextInt(3)*5
    val isNickel = if(coin == 5) 1 else 0
    val isDime   = if(coin == 10) 1 else 0

    // Advance circuit
    poke(c.io.nickel, isNickel)
    poke(c.io.dime,   isDime)
    step(1)

    // Advance model
    money = if (isValid == 1) 0 else (money + coin)
    isValid = if(money >= 20) 1 else 0

    // Compare
    expect(c.io.valid, isValid)
  }
  install(c)
}
