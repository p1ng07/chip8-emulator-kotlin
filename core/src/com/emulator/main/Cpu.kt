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
class Cpu {

    // Number of opcodes to be executed per second (HZ)
    val CPU_FREQUENCY = 1000

    private var oneSecondTimer = timerReset()
    private var delay = 60
    private var sound = 60

    private val PC_START = 0x200
    private var pc = PC_START
    private var stack = Vector<Int>()
    private var memory = IntArray(4096)
    private var endOfProgram: Int = 0
    private var v = UByteArray(16)
    private var I = 0

    private var shouldIncrement = true

    private var VF_CARRY_ON: UByte = 1u
    private var VF_CARRY_OFF: UByte = 0u

    private var screen = Screen()

    public fun tick() {
        for (i in 1..(CPU_FREQUENCY / Screen.data.FPS)) {
            Gdx.app.log(
                    "fetch",
                    "${fetchCurrentCommand()[0].toString(16)}${fetchCurrentCommand()[1].toString(16)}${fetchCurrentCommand()[2].toString(16)}${fetchCurrentCommand()[3].toString(16)}}"
            )

            executeOpCode(fetchCurrentCommand())

            if (shouldIncrement) pc += 2 else shouldIncrement = true
            if (pc >= 4096) {
                pc -= 2
            }
        }
    }

    private fun executeOpCode(array: Array<Int>) {

        val first = array[0]
        val second = array[1]
        val third = array[2]
        val fourth = array[3]

        when (first) {
            0 -> {
                if (third == 0xE) {
                    if (fourth == 0xE) {
                        Gdx.app.log("pc was ", "$pc")

                        pc = stack.removeAt(0)

                        Gdx.app.log("pc removed from stack and set", "$pc")

                        shouldIncrement = false
                    } else {
                        screen.resetPixels()
                    }
                }
            }
            1 -> {
                pc = nibblesToInt(array, 3).toInt()
                shouldIncrement = false
            }
            2 -> {
                stack.add(0, pc + 2)
                Gdx.app.log("Added to the stack", "$pc")

                pc = nibblesToInt(array, 3).toInt()
                Gdx.app.log("PC set ", "${nibblesToInt(array,3)}")

                shouldIncrement = false
            }
            3 ->
                    if (v[second] == nibblesToInt(array, 2).toUByte()) {
                        pc += 2
                    }
            4 ->
                    if (v[second].toUInt() != nibblesToInt(array, 2)) {
                        pc += 2
                    }
            5 ->
                    if (v[second] == v[third]) {
                        pc += 2
                    }
            6 -> v[second] = nibblesToInt(array, 2).toUByte()
            7 -> v[second] = (v[second] + nibblesToInt(array, 2)).toUByte()
            8 ->
                    when (fourth) {
                        0 -> v[second] = v[third]
                        1 -> v[second] = v[second].or(v[third])
                        2 -> v[second] = v[second].and(v[third])
                        3 -> v[second] = v[second].xor(v[third])
                        4 -> {
                            if (v[second] + v[third] > 255u) v[15] = VF_CARRY_ON
                            v[second] = (v[second] + v[third]).toUByte()
                        }
                        5 -> {
                            v[15] = if (v[second] > v[third]) VF_CARRY_ON else VF_CARRY_OFF
                            v[second] = (v[second] - v[third]).toUByte()
                        }
                        6 -> {
                            v[15] = if (v[second].rem(2u).equals(1)) VF_CARRY_ON else VF_CARRY_OFF
                            v[second] = v[second].div(2u).toUByte()
                        }
                        7 -> {
                            v[15] = if (v[third] > v[second]) VF_CARRY_ON else VF_CARRY_OFF
                            v[second] = (v[third] - v[second]).toUByte()
                        }
                        0xe -> {
                            v[15] = if (v[second] >= 128u) VF_CARRY_ON else VF_CARRY_OFF
                            v[second] = v[second].times(2u).toUByte()
                        }
                    }
            9 ->
                    if (v[second].toInt() != v[third].toInt()) {
                        pc += 2
                    }
            0xA -> I = nibblesToInt(array, 3).toInt()
            0xB -> {
                pc = (nibblesToInt(array, 3) + v[0]).toInt()
                shouldIncrement = false
            }
            0xC -> {
                val random = Random.nextBits(8).toUInt()
                v[second] = (nibblesToInt(array, 2).and(random)).toUByte()
            }
            0xD -> drawSpriteAtXY(second, third, fourth)
            0xE -> {
                when (fourth) {
                    0xE -> if (Gdx.input.isKeyPressed(v[second].toInt())) pc += 2
                    0x1 -> if (!Gdx.input.isKeyPressed(v[second].toInt())) pc += 2
                }
            }
            0xF ->
                    when (fourth) {
                        7 -> v[second] = delay.toUByte()
                        0xA -> println("TODO Await for a key press")
                        8 -> sound = v[second].toInt()
                        0xE -> I += v[second].toInt()
                        9 ->
                                println(
                                        "TODO set I to the location in memory of sprite data for digit v[second]"
                                )
                        3 ->
                                println(
                                        "TODO store the BCD representation of v[second] in I, I+1 and I+2"
                                )
                        else -> {
                            when (third) {
                                1 -> delay = v[second].toInt()
                                5 -> for (i in 0..second) memory[I + i] = v[i].toInt()
                                6 -> for (i in 0..second) v[i] = memory[I + i].toUByte()
                            }
                        }
                    }
            else ->
                    Gdx.app.error(
                            "ERROR",
                            "Unknown instruction: ${first.toString(16)}${second.toString(16)}${third.toString(16)}${fourth.toString(16)}}"
                    )
        }
    }

    // Search for DXYN chip-8 instruction
    private fun drawSpriteAtXY(x: Int, y: Int, n: Int) {
        // Represents the pixel to draw
        var point = Vector2Int(v[x].toInt().rem(63), v[y].toInt().rem(31))

        Gdx.app.log("Print call!!!", "")
        // VF (v[15]) is used if the any sprite has collided
        v[15] = VF_CARRY_OFF

        for (sprite in memory.copyOfRange(this.I, this.I + n)) {

            // Iterate through every bit of every sprite starting at the most significant bit
            for (i in 7 downTo 0) {
                var bit = if ((sprite.toInt() shr i).and(1) == 1) true else false

                val oldBit = screen.getPixels(point.x, point.y)

                screen.setPixel(point.x, point.y, bit)

                if (v[15] == VF_CARRY_OFF) {
                    if (screen.getPixels(point.x, point.y) != oldBit) v[15] = VF_CARRY_ON
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
            // if (point.y >= Screen.data.ROWS) break
        }
    }

    // Independent function that draws the screen
    public fun drawScreen() {
        screen.draw()
    }

    public fun loadRomToMemory(romFileName: String) {
        File(romFileName).readBytes().toUByteArray().forEachIndexed { index: Int, element: UByte ->
            memory[index + PC_START] = element.toInt()
        }
        endOfProgram = File(romFileName).readBytes().size
        Gdx.app.log("End of program", "$endOfProgram")
    }

    private fun nibblesToInt(array: Array<Int>, n: Int, startIndex: Int = 3): UInt {

        if (startIndex > 3 || startIndex < 0) throw Exception("Nibbles to int use was invalid")
        if (startIndex - n + 1 < 0) throw Exception("Nibbles to int use was invalid")

        var res = 0u
        for (i in startIndex downTo startIndex - n + 1) {
            res += array[i].toUInt() * 16f.pow(startIndex - i).toUInt()
        }
        return res
    }

    // Fetches the next 2 Bytes at pc and divides them into nibbles
    // ----
    // A nibble is an integer that represents a 4 bit number
    // Example:
    // If a byte is 0001 1111
    // Then this Byte has two nibbles: 1 and 15
    // ----
    // This nibbles are stored Big-Endian
    // The first nibble (most significant) of the first Byte is array[0]
    // The second nibble (least significant) of the first Byte is array[1]
    // And so forth
    private fun fetchCurrentCommand(): Array<Int> {
        val array = Array<Int>(4, { _ -> 0 })
        array[0] = memory.get(pc).and(0xF0) shr 4
        array[1] = memory.get(pc).and(0x0F)
        array[2] = memory.get(pc + 1).and(0xF0) shr 4
        array[3] = memory.get(pc + 1).and(0x0F)
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
