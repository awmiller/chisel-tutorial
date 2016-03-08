package problems

import Chisel._
import Chisel.hwiotesters._

abstract class Filter[T <: Data](dtype: T) extends Module {
  val io = new Bundle {
    val in  = Valid(dtype).asInput
    val out = Valid(dtype).asOutput
} }

class PredicateFilter[T <: Data](dtype: T, f: T => Bool) extends Filter(dtype) {
  io.out.valid := io.in.valid && f(io.in.bits)
  io.out.bits  := io.in.bits
}

object SingleFilter {
  def apply[T <: UInt](dtype: T) = 
    Module(new PredicateFilter(dtype, (x: T) => Bool(false))) // FILL IN FUNCTION
}

object EvenFilter {
  def apply[T <: UInt](dtype: T) = 
    Module(new PredicateFilter(dtype, (x: T) => Bool(false))) // FILL IN FUNCTION
}

class SingleEvenFilter[T <: UInt](dtype: T) extends Filter(dtype) {
  // CREATE COMPOSITION OF SINGLE AND EVEN FILTERS HERE ...
  io.out <> io.in
}

class SingleEvenFilterTests(w: Int) extends SteppedHWIOTester {
  val device_under_test = Module(new SingleEvenFilter(UInt(width = w)))
  val c = device_under_test
  enable_all_debug = true

  val maxInt  = 1 << w
  for (i <- 0 until 10) {
    val in = rnd.nextInt(maxInt)
    poke(c.io.in.valid, 1)
    poke(c.io.in.bits, in)
    val isSingleEven = if ((in <= 9) && (in%2 == 1)) 1 else 0
    expect(c.io.out.valid, isSingleEven)
    expect(c.io.out.bits, in)
    step(1)
  }

}
