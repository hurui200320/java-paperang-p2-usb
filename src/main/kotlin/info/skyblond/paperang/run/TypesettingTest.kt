package info.skyblond.paperang.run

import info.skyblond.paperang.*
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    val font = Font("宋体", Font.PLAIN, 32)
    val width = 72 * 8
    val height = 300

    val image = BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    val graphics = image.graphics
    graphics.setClip(0, 0, width, height)
    // file background with white
    graphics.color = Color.WHITE
    graphics.fillRect(0, 0, width, height)
    // use black for font
    graphics.color = Color.BLACK

    val realHeight = typesetting(
        paperWidth = width, lineStartX = 10, lineEndMargin = 10,
        initialY = 50, lineHeight = 40, bottomMargin = 10,
        typesettingUnits = listOf(
            Space(32,2),
            generateUnifiedWord("English", font),
            *generateSingletonWords("中文123、日本語です", font),
            NewLine,
            *generateSingletonWords("新的一行，哈哈哈哈哈123", font),
            Tab(16, 4),
            *generateSingletonWords("123asddagasdf.", font),
            generateUnifiedWord("这是一个超级长~~~~的不可换行的句子。", font),
            NewLine,
            *generateSingletonWords("这一行用来作为退格（Backspace）的测试。", font),
            Backspace(32, 8, true),
            *generateSingletonWords("覆盖掉退格哈哈哈哈。", font),
        ),
        graphics = graphics
    )

    println(realHeight)

    graphics.dispose()
    ImageIO.write(image, "png", File("./typesetting_test.png"))
}
