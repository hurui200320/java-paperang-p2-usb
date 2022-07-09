package info.skyblond.paperang

import javax.usb.*
import kotlin.random.Random



private fun printBlock(inPipe: UsbPipe, outPipe: UsbPipe, block: (UsbPipe, UsbPipe) -> Unit) {
    // sent paper type - normal paper
    sendPacket(inPipe, outPipe, makePackage(44, 0x00, byteArrayOf(0x00)))
    // feed space
    sendPacket(inPipe, outPipe, makePackage(26, 0x00, (20).toLittleEndian()))
    // do the logic
    block.invoke(inPipe, outPipe)
    // feed space
    sendPacket(inPipe, outPipe, makePackage(26, 0x00, (250).toLittleEndian()))
}

fun main() {
    val usbServices = UsbHostManager.getUsbServices()
    println("USB Service Implementation: " + usbServices.impDescription)
    println("Implementation version: " + usbServices.impVersion)
    println("Service API version: " + usbServices.apiVersion)
    println()

    val p2UsbDevice = findDevice()
    println(p2UsbDevice.usbDeviceDescriptor)
    require(p2UsbDevice.usbConfigurations.size == 1) { "More than 1 usb configurations" }
    val usbConfiguration = p2UsbDevice.usbConfigurations[0] as UsbConfiguration
    println(usbConfiguration.usbConfigurationDescriptor)
    require(usbConfiguration.usbInterfaces.size == 1) { "More than 1 usb interfaces" }
    val usbInterface = usbConfiguration.usbInterfaces[0] as UsbInterface
    println(usbInterface.usbInterfaceDescriptor)
    usbInterface.usbEndpoints.forEach {
        val endPoint = it as UsbEndpoint
        println(endPoint.usbEndpointDescriptor)
    }

    // force claim, might need root
    usbInterface.claim { true }
    try {
        val inEndpoint = usbInterface.getUsbEndpoint(Constants.PAPERANG_P2_USB_IN_ENDPOINT_ADDRESS)
        val outEndpoint = usbInterface.getUsbEndpoint(Constants.PAPERANG_P2_USB_OUT_ENDPOINT_ADDRESS)

        val inPipe = inEndpoint.usbPipe
        val outPipe = outEndpoint.usbPipe

        inPipe.open()
        outPipe.open()
        try {
            // test line length:
            // we got 9 vertical line -> 9*8 = 72 bytes per line
            // max data length is 1023. 1024 will result in empty
            // for alignment, 1008 = 72bytes/line * 14 line
            printBlock(inPipe, outPipe) { inp, outp ->
                val data = ByteArray(1008) {
                    if (it % 8 == 0) 0xFF.toByte() else 0x00
                }
                // here we print on aligned column, by counting the column and multiply by 8
                // we got the line length
                sendPacket(inp, outp, makePackage(0, 0, data), false)
            }
            // verify column
            printBlock(inPipe, outPipe) { inp, outp ->
                val data = ByteArray(1008) {
                    if (it % 8 == 0) 0xAA.toByte() else 0x00
                }
                // here we print on 10101010 on the first column
                // if we see 4 black column, it means 1 bit = 1 column
                sendPacket(inp, outp, makePackage(0, 0, data), false)
            }
            // verify row
            printBlock(inPipe, outPipe) { inp, outp ->
                var flag = false
                val data = ByteArray(1008) {
                    // flip flag every line
                    if (it % 72 == 0) flag = !flag
                    if (flag) 0xFF.toByte() else 0x00.toByte()
                }
                // here we print on black/white lines
                // if we see 7 black row, it means 1 bit = 1 line
                sendPacket(inp, outp, makePackage(0, 0, data), false)
            }
            // print some random data
            printBlock(inPipe, outPipe) { inp, outp ->
                for (i in 16 downTo 0) {
                    sendPacket(inp, outp, makePackage(0, i.toByte(), Random.nextBytes(1008)), false)
                }
            }
        } finally {
            inPipe.close()
            outPipe.close()
        }
    } finally {
        usbInterface.release()
    }
}



