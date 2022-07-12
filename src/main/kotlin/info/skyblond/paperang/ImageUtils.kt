package info.skyblond.paperang

import java.awt.Graphics2D
import java.awt.image.BufferedImage
import kotlin.experimental.or

/**
 * Convert a [BufferedImage] to pure black and white(no gray).
 * Note: this is a hard convert and no fancy filter is applied.
 * This should be used as an image type converter.
 * Since normal RGB file will give hideous result.
 * */
fun BufferedImage.toMonoChrome(): BufferedImage {
    val blackWhite = BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_BINARY)
    val g2d: Graphics2D = blackWhite.createGraphics()
    g2d.drawImage(this, 0, 0, null)
    g2d.dispose()
    return blackWhite
}

fun BufferedImage.toByteArrays(): Array<ByteArray> {
    require(this.width == Constants.PAPERANG_P2_PRINT_BIT_PER_LINE) {
        "The image's width must be ${Constants.PAPERANG_P2_PRINT_BIT_PER_LINE}"
    }
    if (this.type != BufferedImage.TYPE_BYTE_BINARY) {
        return this.toMonoChrome().toByteArrays()
    }
    val raster = this.raster
    val result = Array(this.height) { ByteArray(Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE) }
    for (y in 0 until this.height) {
        for (x in 0 until this.width) {
            // the getPixel should return a single element with 1 or 0
            val color = raster.getPixel(x, y, null as IntArray?)[0]
            val bytePos = x / 8
            val bitPos = 7 - (x % 8)
            var byte = result[y][bytePos].toInt()
            byte = if (color == 0) { // 0 means black, aka hot when print, aka 1 for printer
                byte or (1 shl bitPos)
            } else {
                byte and (1 shl bitPos).inv()
            }
            result[y][bytePos] = byte.toByte()
        }
    }
    return result
}

fun Array<ByteArray>.generatePacketPayloads(): Array<ByteArray> {
    val remainLineCount = this.size % Constants.PAPERANG_P2_USB_LINE_PER_PACKET
    val packetCount = this.size / Constants.PAPERANG_P2_USB_LINE_PER_PACKET +
            if (remainLineCount == 0) 0 else 1
    val result = Array(packetCount) {
        if (it == packetCount - 1) {
            // the last one
            if (remainLineCount == 0) {
                // full packet
                ByteArray(Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE * Constants.PAPERANG_P2_USB_LINE_PER_PACKET)
            } else {
                // smaller one
                ByteArray(remainLineCount * Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE)
            }
        } else {
            ByteArray(Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE * Constants.PAPERANG_P2_USB_LINE_PER_PACKET)
        }
    }

    var p = 0
    for (i in result.indices) {
        var q = 0
        for (j in 0 until (result[i].size / Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE)) {
            System.arraycopy(this[p++], 0, result[i], q, Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE)
            q += Constants.PAPERANG_P2_PRINT_BYTE_PER_LINE
        }
    }

    return result
}
