package info.skyblond.paperang

import java.awt.Font
import java.awt.Graphics
import kotlin.math.max
import kotlin.math.min

sealed class TypesettingUnit

/**
 * A word means it will be together when printing, shift to new line if current
 * line doesn't have enough space.
 * */
data class Word(val chars: List<Pair<Char, Font>>) : TypesettingUnit()

/**
 * Tab: Shift the cursor to next aligned position.
 * [unit] set the width of a single space, and [count] means how many spaces in
 * a tab: unit=16,count=4 means a tab equals 4 space and each space is 16 width.
 *
 * If there is not enough space, the cursor will be the end of the line.
 * */
data class Tab(val unit: Int, val count: Int) : TypesettingUnit()

/**
 * Forward the cursor: a char width is [unit], forward [count] times.
 * If [goNextLine] is false, the maximum amount will set to the end of the line.
 * Otherwise, the cursor will go to next line and keep spacing.
 * */
data class Space(val unit: Int, val count: Int, val goNextLine: Boolean = false) : TypesettingUnit()

/**
 * Rewind the cursor: a char width is [unit], rewind [count] times.
 * If [goPrevLine] is false, the minimal amount will set to the beginning of the line.
 * Otherwise, the cursor will back to previous line and keep rewinding.
 * */
data class Backspace(val unit: Int, val count: Int, val goPrevLine: Boolean = false) : TypesettingUnit()

/**
 * Shift to a new line when meet.
 * */
object NewLine : TypesettingUnit()

/**
 * Generate a word using the same [font]
 * */
fun generateUnifiedWord(str: String, font: Font): Word = Word(str.map { it to font })

/**
 * Generate a list of [Word] from [str], where each single char is a [Word],
 * using the same [font].
 * */
fun generateSingletonWords(str: String, font: Font): Array<Word> = str.map { Word(listOf(it to font)) }.toTypedArray()

fun typesetting(
    paperWidth: Int, lineStartX: Int, lineEndMargin: Int,
    initialY: Int, lineHeight: Int, bottomMargin: Int,
    typesettingUnits: List<TypesettingUnit>,
    graphics: Graphics
): Int {
    var x = lineStartX
    var y = initialY
    var maxY = y
    for (unit in typesettingUnits) {
        when (unit) {
            is Backspace -> {
                var backspaceWidth = unit.unit * unit.count
                if (unit.goPrevLine) {
                    while (backspaceWidth > 0) {
                        val currentLineRemain = max(0, x - lineStartX)
                        x -= min(backspaceWidth, currentLineRemain)
                        backspaceWidth -= currentLineRemain
                        if (backspaceWidth > 0) {
                            // to prev line
                            x = paperWidth - lineEndMargin
                            y -= lineHeight
                        }
                    }
                } else {
                    x = max(x - backspaceWidth, lineStartX)
                }
            }
            is Space -> {
                var spaceWidth = unit.unit * unit.count
                if (unit.goNextLine) {
                    while (spaceWidth > 0) {
                        val currentLineRemain = max(0, paperWidth - lineEndMargin - x)
                        x += min(spaceWidth, currentLineRemain)
                        spaceWidth -= currentLineRemain
                        if (spaceWidth > 0) {
                            // to new line
                            x = lineStartX
                            y += lineHeight
                            // record max y
                            maxY = max(maxY, y)
                        }
                    }
                } else {
                    x += spaceWidth
                }
            }
            is Tab -> {
                val tabWidth = unit.unit * unit.count
                // move to the prev tab and add one tab
                x = ((x - lineStartX) / tabWidth) * tabWidth + tabWidth + lineStartX
            }
            is Word -> {
                var wordWidth = 0
                for ((c, f) in unit.chars) {
                    val fontMetrics = graphics.getFontMetrics(f)
                    val charWidth = fontMetrics.charWidth(c)
                    wordWidth += charWidth
                }
                if (x + wordWidth + lineEndMargin > paperWidth) {
                    // new line is needed
                    x = lineStartX
                    y += lineHeight
                    maxY = max(maxY, y)
                }
                // if the word is super long, f*ck it, just print.
                for ((c, f) in unit.chars) {
                    graphics.font = f
                    val fontMetrics = graphics.getFontMetrics(f)
                    val charWidth = fontMetrics.charWidth(c)
                    graphics.drawString(c.toString(), x, y)
                    x += charWidth
                }
            }
            NewLine -> {
                x = lineStartX
                y += lineHeight
                maxY = max(maxY, y)
            }
        }
    }
    // return the height
    return maxY + bottomMargin
}
