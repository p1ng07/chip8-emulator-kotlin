package com.emulator.main

import com.badlogic.gdx.Gdx
import java.io.File
import java.util.Timer
import java.util.Vector
import kotlin.concurrent.timer
import kotlin.math.*
import kotlin.random.Random

data class Vector2Int(var x: Int, var y: Int)

// Handles the opcode extraction and execution, as well as simulates one cpu tick
@ExperimentalUnsignedTypes
class Cpu() {

    // Number of opcodes to be executed per second (HZ)
    val CPU_FREQUENCY = 500

    private var oneSecondTimer = timerReset()
    private var delay = 60
    private var sound = 60

    private val PC_START = 0x200
    private var pc = PC_START
    private var stack = Vector<Int>()
    private var memory = UByteArray(4096)
    private var v = UByteArray(16)
    private var I = 0

    private var screen = Screen()

    public fun tick() {
        // for (i in 1..(CPU_FREQUENCY / Screen.data.FPS)) {
        Gdx.app.log(
                "fetch",
                "${fetchCurrentCommand()[0]} ${fetchCurrentCommand()[1]} ${fetchCurrentCommand()[2]} ${fetchCurrentCommand()[3]}}"
        )

        executeOpCode(fetchCurrentCommand())
        pc += 2
        // }
    }

    private fun executeOpCode(array: Array<Int>) {

        val first = array[0]
        val second = array[1]
        val third = array[2]
        val fourth = array[3]

        when (first) {
            0 -> {
                if (fourth == 0xE && third == 0xE) {
                    pc = stack.firstElement()
                    stack.remove(0)
                } else screen.resetPixels()
            }
            1 -> pc = nibblesToInt(array, 3).toInt()
            2 -> {
                stack.add(0, pc)
                pc = nibblesToInt(array, 3).toInt()
            }
            3 -> if (v[second] == nibblesToInt(array, 2).toUByte()) pc += 2
            4 -> if (v[second].compareTo(nibblesToInt(array, 2)) != 0) pc += 2
            5 -> if (v[second].equals(v[third])) pc += 2
            6 -> v[second] = nibblesToInt(array, 2).toUByte()
            7 -> v[second] = v[second].plus(nibblesToInt(array, 2)).toUByte()
            8 ->
                    when (fourth) {
                        0 -> v[second] = v[third]
                        1 -> v[second] = v[second].or(v[third])
                        2 -> v[second] = v[second].and(v[third])
                        3 -> v[second] = v[second].xor(v[third])
                        4 -> {
                            if (v[second].plus(v[third]) > 255u) v[15] = 0xffu
                            v[second] = v[second].plus(v[third]).toUByte()
                        }
                        5 -> {
                            v[15] = if (v[second] > v[third]) 1u else 0u
                            v[second] = v[second].minus(v[third]).toUByte()
                        }
                        6 -> {
                            v[15] = if (v[second].rem(2u).equals(1)) 1u else 0u
                            v[second] = v[second].div(2u).toUByte()
                        }
                        7 -> {
                            v[15] = if (v[third] > v[second]) 1u else 0u
                            v[second] = v[third].minus(v[second]).toUByte()
                        }
                        0xe -> {
                            v[15] = if (v[second] >= 128u) 1u else 0u
                            v[second] = v[second].times(2u).toUByte()
                        }
                    }
            9 -> if (v[second].toInt() != v[third].toInt()) pc += 2
            0xA -> this.I = nibblesToInt(array, 3).toInt()
            0xB -> pc = (nibblesToInt(array, 3) + v[0]).toInt()
            0xC -> v[second] = nibblesToInt(array, 2).and(Random.nextBits(8).toUInt()).toUByte()
            // Draw sprite DXYN
            0xD -> drawSpriteAtXY(second, third, fourth)
        }
    }

    // Search for DXYN chip-8 instruction
    // TODO
    private fun drawSpriteAtXY(x: Int, y: Int, n: Int) {
        // Represents the pixel to draw
        var point =
                Vector2Int(
                        v[x].toInt().rem(Screen.data.COLS - 1),
                        v[y].toInt().rem(Screen.data.ROWS - 1)
                )

        // VF (v[15]) is used if the any sprite has collided
        v[15] = 0u

        for (sprite in memory.copyOfRange(this.I, this.I + n)) {

            // Iterate through every bit of every sprite starting at the most significant bit
            for (i in 7 downTo 0) {
                var bit = if ((sprite.toInt() shr i).and(1) == 1) true else false

                val oldBit = screen.getPixels(point.x, point.y)

                screen.setPixel(point.x, point.y, bit)

                if (v[15].equals(0u)) {
                    if (screen.getPixels(point.x, point.y) != oldBit) v[15] = 1u
                }

                point.x++

                // If the coordinate of the pixel to set is outside of display, it stops drawing the
                // current sprite
                if (point.x >= Screen.data.COLS) break
            }
            // Reset the x coord
            point.x = v[x].toInt().rem(Screen.data.COLS - 1)
            point.y++

            // If the coordinate of the pixel to set is outside of display, it stops drawing
            if (point.y >= Screen.data.ROWS) break
        }
    }

    // Independent function that draws the screen
    public fun drawScreen() {
        screen.draw()
    }

    public fun loadRomToMemory(romFileName: String) {
        File(romFileName).readBytes().toUByteArray().forEachIndexed { index: Int, element: UByte ->
            memory[index + PC_START] = element
        }
    }

    private fun nibblesToInt(array: Array<Int>, n: Int, startIndex: Int = 3): UInt {

        if (startIndex > 3 || startIndex < 0) throw Exception("Nibbles to int use was invalid")
        if (startIndex - n + 1 < 0) throw Exception("Nibbles to int use was invalid")

        var res = 0u
        for (i in startIndex downTo startIndex - n + 1) {
            res += array[i].toUInt() * 16f.pow(startIndex - i).toUInt()
        }
        println(res)
        return res
    }

    // Fetches the next 2 Bytes at pc and divides them into nibbles
    // ----
    // A nibble is an integer that represents a 4 bit number
    // Example:
    // If a byte is 0001 1111
    // Then this Byte has two nibble: 1 and 15
    // ----
    // This nibbles are stored Big-Endian
    // The first nibble of the first Byte is array[0]
    // The second nibble of the first Byte is array[1]
    // And so forth
    private fun fetchCurrentCommand(): Array<Int> {
        val array = Array<Int>(4, { _ -> 0 })
        array[0] = memory.get(pc).and(0xF0.toUByte()).toInt() shr 4
        array[1] = memory.get(pc).and(0x0F.toUByte()).toInt()
        array[2] = memory.get(pc + 1).and(0xF0.toUByte()).toInt() shr 4
        array[3] = memory.get(pc + 1).and(0x0F.toUByte()).toInt()
        return array
    }

    private fun timerReset(): Timer {
        return timer(
                "OneSecondTimer",
                false,
                0,
                1000,
                {
                    delay--
                    sound--
                    if (delay == -1) delay = 60
                    if (sound == -1) sound = 60
                }
        )
    }
}
