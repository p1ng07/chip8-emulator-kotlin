package com.emulator.main

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.Input.Keys
import java.io.File
import java.util.Vector
import kotlin.math.*
import kotlin.random.Random

data class Vector2Int(var x: Int, var y: Int)

// Handles the opcode extraction and execution, as well as simulates one cpu tick
@ExperimentalUnsignedTypes
class Cpu(val traceMode: Boolean) {

    // Number of opcodes to be executed per second (HZ)
    val CPU_FREQUENCY = 600

    private var delay = 60
    private var sound = 0

    private var pc = 0x200
    private var stack = Vector<Int>()
    private var memory = IntArray(4096)
    private var v = UByteArray(16)
    private var I = 0

    private var VF_FLAG_ON: UByte = 0x01u
    private var VF_CARRY_OFF: UByte = 0u

    private var screen = Screen()
    private var beepSound = Gdx.audio.newMusic(Gdx.files.internal("beep.mp3"))

    // Tracing variables
    private var stepCounter = 0

    // Maps<Original Key, Mapped key> the chip 8 keys to actual keyboard Keys
    // Original keys:
    // 1 2 3 C
    // 4 5 6 D
    // 7 8 9 E
    // A 0 B F
    // Mapped Keys:
    // 1 2 3 4
    // Q W E R
    // A S D F
    // Z X C V
    private var keyMap = IntArray(16, { _ -> 0 })

    init {
        keyMap[0] = Keys.X
        keyMap[1] = Keys.NUM_1
        keyMap[2] = Keys.NUM_2
        keyMap[3] = Keys.NUM_3
        keyMap[4] = Keys.Q
        keyMap[5] = Keys.W
        keyMap[6] = Keys.E
        keyMap[7] = Keys.A
        keyMap[8] = Keys.S
        keyMap[9] = Keys.D
        keyMap[0xA] = Keys.Z
        keyMap[0xB] = Keys.C
        keyMap[0xC] = Keys.NUM_4
        keyMap[0xD] = Keys.R
        keyMap[0xE] = Keys.F
        keyMap[0xF] = Keys.V

        val spriteData =
                intArrayOf(
                        0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
                        0x20, 0x60, 0x20, 0x20, 0x70, // 1
                        0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
                        0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
                        0x90, 0x90, 0xF0, 0x10, 0x10, // 4
                        0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
                        0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
                        0xF0, 0x10, 0x20, 0x40, 0x40, // 7
                        0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
                        0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
                        0xF0, 0x90, 0xF0, 0x90, 0x90, // A
                        0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
                        0xF0, 0x80, 0x80, 0x80, 0xF0, // C
                        0xE0, 0x90, 0x90, 0x90, 0xE0, // D
                        0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
                        0xF0, 0x80, 0xF0, 0x80, 0x80  // F
                )
        for (i in spriteData.indices) memory[i] = spriteData[i]
    }

    public fun tick() {
        // Frequency is assumed to be locked at 600 HZ and FPS at 60, so the manual stepping as well
        // as the normal stepping execute 10 steps before the timers (60 HZ timers) are decremented
        if (traceMode) {
            if (Gdx.app.input.isKeyJustPressed(Keys.P) || Gdx.app.input.isKeyPressed(Keys.O)) {

                Gdx.app.debug(
                        "fetch",
                        "${fetchCurrentCommand()[0].toString(16)}${fetchCurrentCommand()[1].toString(16)}${fetchCurrentCommand()[2].toString(16)}${fetchCurrentCommand()[3].toString(16)}}"
                )
                Gdx.app.debug("Delay Timer", "${this.delay}")
                Gdx.app.debug("Sound Timer", "${this.sound}")

                step()

                // In trace mode we want to print every single information after every single step
                Gdx.app.debug("Registers", "")
                for (i in v.indices) Gdx.app.debug("v${i.toString(16)}", "${v[i]}")
                Gdx.app.debug("I register", "$I")

                stepCounter++

                if (stepCounter > 9) {
                    if (delay != 0) delay--
                    if (sound != 0) sound--
                    stepCounter = 0
                }
            }
        } else {

            for (i in 1..10) {
                step()
            }

            if (delay > 0) delay--
            if (sound > 0){
                if(!this.beepSound.isPlaying()) this.beepSound.play()
                sound--
                }
        }
    }

    private fun step() {
        executeOpCode(fetchCurrentCommand())
        pc +=2
    }

    private fun executeOpCode(array: Array<Int>) {
        val first = array[0]
        val second = array[1]
        val third = array[2]
        val fourth = array[3]
        when (first) {
            0 -> {
                // This is not an opcode, it serves as a stopper when the program reaches invalid
                // code
                if (third == 0x0) {
                    pc -=2
                    return
                }

                if (third == 0xE) {
                    if (fourth == 0xE) {
                        pc = stack.removeAt(0)
                        pc -=2
                    } else {
                        screen.resetPixels()
                    }
                }
            }
            1 -> {
                pc = nibblesToInt(array, 3).toInt()
                pc -=2
            }
            2 -> {
                stack.add(0, pc + 2)
                pc = nibblesToInt(array, 3).toInt()
                pc -=2
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
                            if (v[second] + v[third] > 255u) v[15] = VF_FLAG_ON
                            v[second] = (v[second] + v[third]).toUByte()
                        }
                        5 -> {
                            v[15] = if (v[second] > v[third]) VF_FLAG_ON else VF_CARRY_OFF
                            v[second] = (v[second] - v[third]).toUByte()
                        }
                        6 -> {
                            v[15] = if (v[second].rem(2u).equals(1)) VF_FLAG_ON else VF_CARRY_OFF
                            v[second] = v[second].div(2u).toUByte()
                        }
                        7 -> {
                            v[15] = if (v[third] > v[second]) VF_FLAG_ON else VF_CARRY_OFF
                            v[second] = (v[third] - v[second]).toUByte()
                        }
                        0xe -> {
                            v[15] = if (v[second] >= 128u) VF_FLAG_ON else VF_CARRY_OFF
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
                pc -= 2
            }
            0xC -> {
                val random = Random.nextBits(8).toUInt()
                v[second] = (nibblesToInt(array, 2).and(random)).toUByte()
            }
            0xD -> drawSpriteAtXY(second, third, fourth)
            0xE -> {
                when (fourth) {
                    0x1 -> if (!Gdx.input.isKeyPressed(keyMap[v[second].toInt()])) pc += 2
                    0xE -> if (Gdx.input.isKeyPressed(keyMap[v[second].toInt()])) pc += 2
                }
            }
            0xF ->
                    when (fourth) {
                        7 -> v[second] = delay.toUByte()
                        0xA -> {
                            val key = isAnyValidKeyPressed()
                            // Gdx.app.debug("Key", "Await for a key press of ${v[second]}")

                            if (key != null) {
                                for (i in keyMap.indices) if (keyMap[i] == key)
                                        v[second] = i.toUByte()
                            } else {
                                pc -= 2
                            }
                        }
                        8 -> sound = v[second].toInt()
                        0xE -> I += v[second].toInt()
                        9 -> I = (v[second] * 5u).toInt()
                        3 -> {
                            memory[I] = v[second].div(100u).toInt()
                            memory[I + 1] = v[second].rem(100u).div(10u).toInt()
                            memory[I + 2] = v[second].rem(10u).toInt()
                        }
                        else -> {
                            when (third) {
                                1 -> delay = v[second].toInt()
                                5 -> for (i in 0..second) memory[I + i] = v[i].toInt()
                                6 -> for (i in 0..second) v[i] = memory[I + i].toUByte()
                            }
                        }
                    }
            else ->
                    Gdx.app.log(
                            "ERROR",
                            "Unknown instruction: ${first.toString(16)}${second.toString(16)}${third.toString(16)}${fourth.toString(16)}}"
                    )
        }
    }

    // Returns which valid key is pressed, if none are pressed then returns null
    private fun isAnyValidKeyPressed(): Int? {
        if (Gdx.input.isKeyPressed(Keys.NUM_1)) return Keys.NUM_1
        else if (Gdx.input.isKeyPressed(Keys.NUM_2)) return Keys.NUM_2
        else if (Gdx.input.isKeyPressed(Keys.NUM_3)) return Keys.NUM_3
        else if (Gdx.input.isKeyPressed(Keys.NUM_4)) return Keys.NUM_4
        else if (Gdx.input.isKeyPressed(Keys.Q)) return Keys.Q
        else if (Gdx.input.isKeyPressed(Keys.W)) return Keys.W
        else if (Gdx.input.isKeyPressed(Keys.E)) return Keys.E
        else if (Gdx.input.isKeyPressed(Keys.R)) return Keys.R
        else if (Gdx.input.isKeyPressed(Keys.A)) return Keys.A
        else if (Gdx.input.isKeyPressed(Keys.S)) return Keys.S
        else if (Gdx.input.isKeyPressed(Keys.D)) return Keys.D
        else if (Gdx.input.isKeyPressed(Keys.F)) return Keys.F
        else if (Gdx.input.isKeyPressed(Keys.Z)) return Keys.Z
        else if (Gdx.input.isKeyPressed(Keys.X)) return Keys.X
        else if (Gdx.input.isKeyPressed(Keys.C)) return Keys.C
        else if (Gdx.input.isKeyPressed(Keys.V)) return Keys.V
        return null
    }

    private fun drawSpriteAtXY(x: Int, y: Int, n: Int) {
        // Represents the pixel to draw
        var point =
                Vector2Int(v[x].toInt().rem(Screen.data.COLS), v[y].toInt().rem(Screen.data.ROWS))

        if (traceMode) {
            for (i in memory.copyOfRange(this.I, this.I + n)) Gdx.app.debug(
                    "sprite",
                    "${i.toString(2)}"
            )
        }

        // VF (v[15]) is used if the any sprite has collided
        v[15] = VF_CARRY_OFF

        for (sprite in memory.copyOfRange(this.I, this.I + n)) {

            // Iterate through every bit of every sprite starting at the most significant bit
            for (i in 7 downTo 0) {
                var bit = if ((sprite.toInt() shr i).and(1) == 1) true else false

                val oldBit = screen.getPixels(point.x, point.y)

                screen.setPixel(point.x, point.y, bit)

                if (v[15] == VF_CARRY_OFF) {
                    if (screen.getPixels(point.x, point.y) == false  &&  oldBit == true) v[15] = VF_FLAG_ON
                }

                point.x++

                // If the coordinate of the pixel to set is outside of display, it stops drawing the
                // current sprite
                if (point.x >= Screen.data.COLS) break
            }
            // Reset the x coord
            point.x = v[x].toInt().rem(Screen.data.COLS)
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
            memory[index + 0x200] = element.toInt()
        }
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
}
