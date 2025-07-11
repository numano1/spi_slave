package spi_slave

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import chisel3.experimental._
import chisel3.withClock
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}
import async_set_register._

class spi_slave(val cfg_length : Int = 8, val mon_length : Int = 8) extends Module {
    val io = IO(new Bundle{
      val mosi = Input(UInt(1.W))
      val cs   = Input(UInt(1.W))
      val miso = Output(UInt(1.W))
      val sclk = Input(Clock())
      val config_out = Output(UInt(cfg_length.W))
      val monitor_in = Input(UInt(mon_length.W))
    })

    def risingEdge(x: Bool) = x && !RegNext(x)

    //val inv_sclk = (!clock.asUInt.asBool).asClock()
    val inv_sclk = (!io.sclk.asUInt.asBool).asClock()

    // Here' my two cents
    //
    // SPI registers
    val shiftingConfig = withClock(inv_sclk){ Reg(UInt((cfg_length+1).W)) }
    val stateConfig = Reg(UInt((cfg_length+1).W))
    val shiftingMonitor = withClock(inv_sclk){ Reg(UInt(mon_length.W)) }
    val misoPosEdgeBuffer = withClock(io.sclk){ Reg(UInt(1.W)) }

    // TODO this guy has to have an asynchronous reset!
    // TODO ... and clocked with a falling edge of sclk
    //
    val spiFirstCycle = Wire(UInt(1.W))
    val asyncRegister = Module(new async_set_register(n=1)).io
    asyncRegister.D := 0.U
    spiFirstCycle := asyncRegister.Q
    asyncRegister.clock := inv_sclk
    asyncRegister.set := io.cs.asBool

    // masks applied for inserting MSB
    val configMask = Cat(io.mosi,0.U(cfg_length.W))
    val monitorMask = Cat(shiftingConfig(0),0.U((mon_length-1).W))
    
    // pre-shifted vectors for register assignment 
    //val nextShiftingConfig = (shiftingConfig << 1) | io.mosi
    //val monitorRegShifted = (shiftingMonitor << 1) | shiftingConfig(cfg_length)
    val nextShiftingConfig = configMask | (shiftingConfig >> 1)
    val monitorRegShifted = monitorMask | (shiftingMonitor >> 1)

    // juggling with the monitor register input
    val monitorMuxControl = !io.cs.asBool && spiFirstCycle.asBool
    val nextShiftingMonitor = Mux(monitorMuxControl, io.monitor_in, monitorRegShifted)
    shiftingMonitor := nextShiftingMonitor

    // SPI transfer happens during CS line being low
    when (!io.cs.asBool) {
      shiftingConfig := nextShiftingConfig
      //misoPosEdgeBuffer := nextShiftingMonitor(mon_length-1)
      misoPosEdgeBuffer := nextShiftingMonitor(0)
      io.miso := misoPosEdgeBuffer
    } .otherwise {
        // first cycle of internal clock after CS rises again
        when (risingEdge(io.cs.asBool)){
          stateConfig := shiftingConfig
        }
        io.miso:=0.U(1.W)
    }

    // provide a snapshot of shifting Config register to the chip
    io.config_out := stateConfig(cfg_length,1)
}


//This is the object to provide verilog
object spi_slave extends App {
    // Getopts parses the "Command line arguments for you"  
    def getopts(options : Map[String,String], 
        arguments: List[String]) : (Map[String,String], List[String]) = {
        //This the help
        val usage = """
            |Usage: spi_slave.spi_slave [-<option>]
            |
            | Options
            |     cfg_length       [Int]     : Number of bits in the config register. Default 8
            |     mon_length       [Int]     : Number of bits in the monitor register. Default 8
            |     h                          : This help 
          """.stripMargin
        val optsWithArg: List[String]=List(
            "-cfg_length",
            "-mon_length"
        )
        //Handling of flag-like options to be defined 
        arguments match {
            case "-h" :: tail => {
                println(usage)
                val (newopts, newargs) = getopts(options, tail)
                sys.exit()
                (Map("h"->"") ++ newopts, newargs)
            }
            case option :: value :: tail if optsWithArg contains option => {
               val (newopts, newargs) = getopts(
                   options++Map(option.replace("-","") -> value), tail
               )
               (newopts, newargs)
            }
              case argument :: tail => {
                 val (newopts, newargs) = getopts(options,tail)
                 (newopts, argument.toString +: newargs)
              }
            case Nil => (options, arguments)
        }
    }
     
    // Default options
    val defaultoptions : Map[String,String]=Map(
        "cfg_length"->"8",
        "mon_length"->"8"
        ) 
    // Parse the options
    val (options,arguments)= getopts(defaultoptions,args.toList)
  
  (new ChiselStage).execute(arguments.toArray, Seq(
    ChiselGeneratorAnnotation(() =>
      new spi_slave(
        cfg_length = options("cfg_length").toInt,
        mon_length = options("mon_length").toInt
      )
    )
  ))
}

class spi_slaveTest extends AnyFlatSpec with ChiselScalatestTester {
  "spi_slave" should "initialize and respond" in {
    test(new spi_slave(cfg_length = 8, mon_length = 8)) { dut =>
      // Example test: poke and check that it compiles
      dut.io.mosi.poke(1.U)
      dut.io.cs.poke(0.U)
      dut.io.monitor_in.poke(42.U)
      dut.clock.step()
      // You can add expectations here if needed
      // dut.io.miso.expect(1.U)
    }
  }
}
