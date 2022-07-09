package info.skyblond.paperang

object Constants {
    const val PAPERANG_P2_USB_VENDOR_ID: Short = 0x4348

    const val PAPERANG_P2_USB_PRODUCT_ID: Short = 0x5584

    const val PAPERANG_P2_USB_IN_ENDPOINT_ADDRESS: Byte = 0x81.toByte()
    const val PAPERANG_P2_USB_OUT_ENDPOINT_ADDRESS: Byte = 0x02

    const val PAPERANG_P2_PRINT_BYTE_PER_LINE = 72

    const val PAPERANG_P2_PRINT_BIT_PER_LINE = PAPERANG_P2_PRINT_BYTE_PER_LINE * Byte.SIZE_BITS


}
