package info.skyblond.paperang

import javax.usb.UsbPipe

object Commands {
    private const val PRINT_DATA: Byte = 0.toByte()

    /**
     * Send print data to printer.
     * [data] must equal or smaller than [Constants.PAPERANG_P2_USB_MAX_PACKET_SIZE_IN_BYTE].
     * [remainCounter] must be byte
     * */
    fun sendPrintData(
        inPipe: UsbPipe, outPipe: UsbPipe,
        data: ByteArray, remainCounter: Int
    ) {
        require(data.size <= Constants.PAPERANG_P2_USB_MAX_PACKET_SIZE_IN_BYTE)
        require(remainCounter <= UByte.MAX_VALUE.toInt())
        sendPacket(
            inPipe, outPipe,
            makePackage(PRINT_DATA, remainCounter.toUByte(), data), false
        )
    }

    private const val SET_HEAT_DENSITY: Byte = 25.toByte()

    /**
     * Set the heat density before print.
     * [density] max is 100, min is 0.
     * Note: 0 is not white, but every light.
     * */
    fun setHeatDensity(
        inPipe: UsbPipe, outPipe: UsbPipe, density: UByte
    ) {
        sendPacket(
            inPipe, outPipe,
            makePackage(SET_HEAT_DENSITY, 0u, byteArrayOf(density.toByte()))
        )
    }

    private const val FEED_LINE: Byte = 26.toByte()

    /**
     * Feed some space, aka print nothing
     * */
    fun feedSpaceLine(
        inPipe: UsbPipe, outPipe: UsbPipe, amount: Short
    ) {
        sendPacket(
            inPipe, outPipe,
            makePackage(FEED_LINE, 0u, amount.toLittleEndian())
        )
    }

    private const val PRINT_TEST_PAGE: Byte = 27.toByte()

    /**
     * Print a default test page.
     * */
    fun printTestPage(
        inPipe: UsbPipe, outPipe: UsbPipe
    ) {
        sendPacket(
            inPipe, outPipe,
            makePackage(PRINT_TEST_PAGE, 0u, byteArrayOf())
        )
    }

    private const val FEED_TO_HEAD_LINE: Byte = 33.toByte()

    /**
     * Same as [feedSpaceLine], not sure why.
     * */
    fun feedToHeadLine(
        inPipe: UsbPipe, outPipe: UsbPipe, amount: Short
    ) {
        sendPacket(
            inPipe, outPipe,
            makePackage(FEED_TO_HEAD_LINE, 0u, amount.toLittleEndian())
        )
    }

    private const val SET_PAPER_TYPE: Byte = 44.toByte()

    /**
     * Set the paper type to normal paper (0).
     * Other value is unknown.
     * */
    fun setToNormalPaper(inPipe: UsbPipe, outPipe: UsbPipe) {
        sendPacket(
            inPipe, outPipe,
            makePackage(SET_PAPER_TYPE, 0u, byteArrayOf(0))
        )
    }

}
