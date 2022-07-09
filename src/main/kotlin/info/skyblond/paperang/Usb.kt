package info.skyblond.paperang

import javax.usb.UsbDevice
import javax.usb.UsbHostManager
import javax.usb.UsbHub
import javax.usb.UsbPipe

fun findDevice(): UsbDevice {
    val usbServices = UsbHostManager.getUsbServices()
    return internalFindDevice(
        usbServices.rootUsbHub,
        Constants.PAPERANG_P2_USB_VENDOR_ID,
        Constants.PAPERANG_P2_USB_PRODUCT_ID
    ) ?: error("Device not found")
}

private fun internalFindDevice(hub: UsbHub, vendorId: Short, productId: Short): UsbDevice? {
    for (device in hub.attachedUsbDevices) {
        val desc = (device as UsbDevice).usbDeviceDescriptor
        if (desc.idVendor() == vendorId && desc.idProduct() == productId) return device
        if (device.isUsbHub) {
            val result = internalFindDevice(device as UsbHub, vendorId, productId)
            if (result != null) return result
        }
    }
    return null
}

fun ByteArray.toHexString() = this.joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun Short.toLittleEndian(): ByteArray {
    val result = ByteArray(Short.SIZE_BYTES)
    var value = this.toInt()
    val mask = 0xFF

    for (i in result.indices) {
        result[i] = (value and mask).toByte()
        value = value shr Byte.SIZE_BITS
    }

    return result
}

fun Int.toLittleEndian(): ByteArray {
    val result = ByteArray(Int.SIZE_BYTES)
    var value = this
    val mask = 0xFF

    for (i in result.indices) {
        result[i] = (value and mask).toByte()
        value = value shr Byte.SIZE_BITS
    }

    return result
}

// TODO cmd list?
fun makePackage(cmd: Byte, packetRemainCount: Byte, data: ByteArray = ByteArray(0)): ByteArray {
    val crc = CRC32().let {
        it.reset(0x35769521L and 0xffffffffL)
        it.update(data, 0, data.size)
        it.value
    }
    val payload = ByteArray(10 + data.size)
    var p = 0
    // 0x02: fixed
    payload[p++] = 0x02
    // 0x10: cmd
    payload[p++] = cmd
    // 0x00: packet remain count: 0 means no more packet, this is the last one
    payload[p++] = packetRemainCount
    // 0x00, 0x00: data length in little endian
    check(data.size < Short.MAX_VALUE) { "Data is too big" }
    val dataLengthBytes = data.size.toShort().toLittleEndian()
    for (i in dataLengthBytes.indices) {
        payload[p++] = dataLengthBytes[i]
    }
    // x: no data here
    for (i in data.indices) {
        payload[p++] = data[i]
    }
    // crc in little endian
    for (i in 0 until 4) {
        payload[p++] = crc[i]
    }
    // 0x03: the end
    payload[p] = 0x03

    return payload
}

fun toShort(lb: Byte, hb: Byte): Short {
    var result = 0
    result = result or lb.toInt()
    result = result or (hb.toInt() shl Byte.SIZE_BITS)
    return (result and 0xffff).toShort()
}

fun readPackages(payload: ByteArray): List<Triple<Byte, Byte, ByteArray>> {
    val result = mutableListOf<Triple<Byte, Byte, ByteArray>>()
    var p = 0
    while (true) {
        // 02: start
        while (p < payload.size) {
            // keep reading until get 0x02
            if (payload[p++] == (0x02).toByte()) {
                break
            }
        }
        if (p >= payload.size)
            return result

        val command = payload[p++]
        val packetIndex = payload[p++]
        val dataLength = toShort(payload[p], payload[p + 1]).toInt()
        p += 2
        val data = ByteArray(dataLength).also {
            for (i in it.indices) {
                it[i] = payload[p++]
            }
        }
        val crc = ByteArray(4).also {
            for (i in it.indices) {
                it[i] = payload[p++]
            }
        }
        // 03: end
        check(payload[p++] == (0x03).toByte()) { "Packet not end with 0x03" }

        // check crc
        val calcCrc = CRC32().let {
            it.reset(0x35769521L and 0xffffffffL)
            it.update(data, 0, data.size)
            it.value
        }
        check(calcCrc.contentEquals(crc)) { "CRC32 not match: Packet crc: ${crc.toHexString()}, calculated crc: ${calcCrc.toHexString()}" }

        result.add(Triple(command, packetIndex, data))
    }
}

fun sendPacket(inPipe: UsbPipe, outPipe: UsbPipe, packet: ByteArray, needReply: Boolean = true) {
    println("Send: ${packet.toHexString()}")
    outPipe.syncSubmit(packet)
    if (needReply) {
        val readBuffer = ByteArray(2048)
        inPipe.syncSubmit(readBuffer)
        println(
            "Get:  ${
                readBuffer.toHexString().let {
                    var str = it
                    var length = str.length
                    while (true) {
                        str = str.removeSuffix("00")
                        if (str.length == length)
                            break
                        length = str.length
                    }
                    str
                }
            }"
        )
        readPackages(readBuffer).forEach { (command, index, result) ->
            println("Reply command: $command")
            println("Reply data #${index}: ${result.toHexString()}")
        }
    }
}
