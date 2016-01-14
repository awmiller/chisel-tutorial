package examples

import Chisel._
import Chisel.testers.UnitTester

class ReadCmd extends Bundle {
  val addr = UInt(width = 32)
}

class WriteCmd extends ReadCmd {
  val data = UInt(width = 32)
}

class Packet extends Bundle {
  val header = UInt(width = 8)
  val body   = Bits(width = 64)
}

/**
 * This router circuit
 * It routes a packet placed on it's input to one of n output ports
 *
 * @param n is number of fanned outputs for the routed packet
 */
class RouterIO(n: Int) extends Bundle {
  override def cloneType           = new RouterIO(n).asInstanceOf[this.type]
  val read_routing_table_request   = new DeqIO(new ReadCmd())
  val read_routing_table_response  = new EnqIO(UInt(width = 8))
  val load_routing_table_request   = new DeqIO(new WriteCmd())
  val in                           = new DeqIO(new Packet())
  val outs                         = Vec(n, new EnqIO(new Packet()))
}

class Router extends Module {
  val depth = 32
  val n     = 4
  val io    = new RouterIO(n)
  val tbl   = Mem(depth, UInt(width = BigInt(n).bitLength))

  io.read_routing_table_request.init
  io.load_routing_table_request.init
  io.read_routing_table_response.init
  io.in.init
  io.outs.foreach { out => out.init}

  val ti = Reg(init=UInt(0, width = 16))
  ti := ti + UInt(1)
  printf("                    tbl: %d : %d : %d : %d",
    tbl(0),
    tbl(1),
    tbl(2),
    tbl(3)
  )

  when(io.read_routing_table_request.valid && io.read_routing_table_response.ready) {
    val cmd = io.read_routing_table_request.deq()
    io.read_routing_table_response.enq(tbl(cmd.addr))
  }
  .elsewhen(io.load_routing_table_request.valid) {
    val cmd = io.load_routing_table_request.deq()
    tbl(cmd.addr) := cmd.data
  }
  .elsewhen(io.in.valid) {
    val pkt = io.in.bits
    val idx = tbl(pkt.header(0))
    when(io.outs(idx).ready) {
      io.in.deq()
      io.outs(idx).enq(pkt)
    }
  } 
}

class RouterUnitTester extends UnitTester {
  val c = Module(new Router)

  def rd(addr: Int, data: Int) = {
    poke(c.io.in.valid,        0)     // initialize the in queue
    poke(c.io.load_routing_table_request.valid,    0)     // initialize
    poke(c.io.read_routing_table_request.valid,    1)
    poke(c.io.read_routing_table_response.ready,   1)
    poke(c.io.read_routing_table_request.bits.addr, addr)
    expect(c.io.read_routing_table_response.bits, data)
    step(1)
  }

  def wr(addr: Int, data: Int)  = {
    poke(c.io.in.valid,         0)
    poke(c.io.read_routing_table_request.valid,      0)
    poke(c.io.load_routing_table_request.valid,     1)
    poke(c.io.load_routing_table_request.bits.addr, addr)
    poke(c.io.load_routing_table_request.bits.data, data)
    step(1)
  }

  def rt(header: Int, body: Int)  = {
    for (out <- c.io.outs)
      poke(out.ready, 1)
    poke(c.io.read_routing_table_request.valid,    0)
    poke(c.io.load_routing_table_request.valid,   0)
    poke(c.io.in.valid,       1)
    poke(c.io.in.bits.header, header)
    poke(c.io.in.bits.body,   body)

//    for (out <- c.io.outs) {
//      when(out.valid) {
//        printf("io.valid, io.pc %d\n", pc)
////      stop(0)
//      } otherwise {
//        step(1)
//      }
//    }
//    expect(io.pc < UInt(10))
  }
  rd(0, 0)
  wr(0, 1)
  rd(0, 1)
  rt(0, 1)
  install(c)
}

import Chisel.testers.DecoupledTester

class RouterDecoupledTester extends DecoupledTester {
  val device_under_test = Module(new Router)
  val c = device_under_test

  val ti = Reg(init=UInt(0, width = 16))
  ti := ti + UInt(1)
  when(ti >= UInt(50)) { stop() }

  printf("ti %d, read %d %d,   write %d %d   in.ready %d %d",
        ti,
        c.io.read_routing_table_request.ready,
        c.io.read_routing_table_request.valid,
        c.io.load_routing_table_request.ready,
        c.io.load_routing_table_request.valid,
        c.io.in.ready,
        c.io.in.valid
  )

  def rd(addr: Int, data: Int) = {
    input_event(List(c.io.read_routing_table_request.bits.addr -> addr))
    output_event(List(c.io.read_routing_table_response.bits -> data))
  }

  def wr(addr: Int, data: Int)  = {
    input_event(List(
      c.io.load_routing_table_request.bits.addr -> addr,
      c.io.load_routing_table_request.bits.data -> data
    ))
  }

  def rt(header: Int, body: Int)  = {

    //    for (out <- c.io.outs)
    //      poke(out.ready, 1)
    //    poke(c.io.read_routing_table_request.valid,    0)
    //    poke(c.io.load_routing_table_request.valid,   0)
    //    poke(c.io.in.valid,       1)
    //    poke(c.io.in.bits.header, header)
    //    poke(c.io.in.bits.body,   body)

    //    for (out <- c.io.outs) {
    //      when(out.valid) {
    //        printf("io.valid, io.pc %d\n", pc)
    ////      stop(0)
    //      } otherwise {
    //        step(1)
    //      }
    //    }
    //    expect(io.pc < UInt(10))
  }
  rd(0, 0)
  wr(0, 1)
  rd(0, 1)
  rt(0, 1)

  finish(show_io_table = true)
}
