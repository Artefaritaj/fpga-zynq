package zynq
package boom

import chisel3._
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.util.DontTouch
import freechips.rocketchip.config.Config
import testchipip._
import _root_.boom.system.{BoomSubsystem, BoomSubsystemModule}

class TestHarness(implicit val p: Parameters) extends Module {
  val io = IO(new Bundle {
    val success = Output(Bool())
  })

  val driver = Module(LazyModule(new TestHarnessDriver).module)
  val dut = Module(LazyModule(new FPGAZynqTop).module)

  dut.reset := driver.io.sys_reset
  dut.debug := DontCare
  dut.tieOffInterrupts()
  dut.dontTouchPorts()
  dut.connectSimAXIMem()

  driver.io.serial <> dut.serial
  driver.io.bdev <> dut.bdev
  io.success := driver.io.success
}

class Top(implicit val p: Parameters) extends Module {
  val address = p(ZynqAdapterBase)
  val config = p(ExtIn).get
  val target = Module(LazyModule(new FPGAZynqTop).module)
  val adapter = Module(LazyModule(new ZynqAdapter(address, config)).module)

  require(target.mem_axi4.size == 1)

  val io = IO(new Bundle {
    val ps_axi_slave = Flipped(adapter.axi.cloneType)
    val mem_axi = target.mem_axi4.head.cloneType
  })

  io.mem_axi <> target.mem_axi4.head
  adapter.axi <> io.ps_axi_slave
  adapter.io.serial <> target.serial
  adapter.io.bdev <> target.bdev

  target.debug := DontCare
  target.tieOffInterrupts()
  target.dontTouchPorts()
  target.reset := adapter.io.sys_reset
}

class FPGAZynqTop(implicit p: Parameters) extends BoomSubsystem
    with CanHaveMasterAXI4MemPort
//    with HasSystemErrorSlave
    with HasPeripheryBootROM
    with HasSyncExtInterrupts
    with HasNoDebug
    with HasPeripherySerial
    with HasPeripheryBlockDevice {
  override lazy val module = new FPGAZynqTopModule(this)
}

class FPGAZynqTopModule(outer: FPGAZynqTop) extends BoomSubsystemModule(outer)
    with HasRTCModuleImp
    with CanHaveMasterAXI4MemPortModuleImp
    with HasPeripheryBootROMModuleImp
    with HasExtInterruptsModuleImp
    with HasNoDebugModuleImp
    with HasPeripherySerialModuleImp
    with HasPeripheryBlockDeviceModuleImp
    with DontTouch

class SmallBoomZynqConfig extends Config(
  new WithBootROM ++ new WithZynqAdapter ++ new _root_.boom.system.SmallBoomConfig)
class MediumBoomZynqConfig extends Config(
  new WithBootROM ++ new WithZynqAdapter ++ new _root_.boom.system.MediumBoomConfig)


class WithZynqAdapterZCU104 extends Config((site, here, up) => {
  case SerialFIFODepth => 16
  case ResetCycles => 10
  case ZynqAdapterBase => BigInt(0x43C00000L)
  case ExtMem => up(ExtMem, site) map (_.copy(idBits = 6))
  case ExtIn => up(ExtIn, site) map (_.copy(beatBytes = 4, idBits = 12))
  case BlockDeviceKey => BlockDeviceConfig(nTrackers = 2)
  case BlockDeviceFIFODepth => 16
  case NetworkFIFODepth => 16
})

class SmallBoomZynqConfigZCU104 extends Config(
  new WithBootROM ++ new WithZynqAdapterZCU104 ++ new _root_.boom.system.SmallBoomConfig)
class MediumBoomZynqConfigZCU104 extends Config(
  new WithBootROM ++ new WithZynqAdapterZCU104 ++ new _root_.boom.system.MediumBoomConfig)
class InriaConfigZCU104 extends Config(
  new WithBootROM ++ new WithZynqAdapterZCU104 ++ new _root_.boom.system.SmallBoomConfig)