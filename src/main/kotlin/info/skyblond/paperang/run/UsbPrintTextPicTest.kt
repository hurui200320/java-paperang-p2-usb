package info.skyblond.paperang.run

import info.skyblond.paperang.*
import java.io.File
import javax.imageio.ImageIO
import javax.usb.UsbConfiguration
import javax.usb.UsbInterface

fun main() {
    val p2UsbDevice = findDevice()
    require(p2UsbDevice.usbConfigurations.size == 1) { "More than 1 usb configurations" }
    val usbConfiguration = p2UsbDevice.usbConfigurations[0] as UsbConfiguration
    require(usbConfiguration.usbInterfaces.size == 1) { "More than 1 usb interfaces" }
    val usbInterface = usbConfiguration.usbInterfaces[0] as UsbInterface

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
            // sent paper type - normal paper
            Commands.setToNormalPaper(inPipe, outPipe)
            // feed space
            Commands.feedToHeadLine(inPipe, outPipe, 20)
            // print something
            val image = ImageIO.read(File("./typesetting_test.png"))
            val packetData = image.toByteArrays().generatePacketPayloads()
            for (i in packetData.indices) {
                Commands.sendPrintData(inPipe, outPipe, packetData[i], packetData.size - 1 - i)
            }


            // feed space
            Commands.feedSpaceLine(inPipe, outPipe, 250)
        } finally {
            inPipe.close()
            outPipe.close()
        }
    } finally {
        usbInterface.release()
    }
}

