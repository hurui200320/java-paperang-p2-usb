package info.skyblond.paperang.run

import info.skyblond.paperang.Commands
import info.skyblond.paperang.Constants
import info.skyblond.paperang.findDevice
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
            Commands.setToNormalPaper(inPipe, outPipe)
            Commands.setHeatDensity(inPipe, outPipe, 75u)
            Commands.printTestPage(inPipe, outPipe)
        } finally {
            inPipe.close()
            outPipe.close()
        }
    } finally {
        usbInterface.release()
    }
}

