package com.emulator.main

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.Input.Keys
import java.io.File
import java.util.Vector
import kotlin.math.*
import kotlin.random.Random

data class Vector2Int(var x: Int, var y: Int)

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

    private var stepCounter = 0

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
        val x = array[1]
        val y = array[2]
        val kk = nibblesToInt(array, 2).toUByte()
        val nnn = nibblesToInt(array, 3).toInt()
        val n = array[3]

        when (array[0]) {
            0 -> {
                if (y == 0x0) pc -=2
                else if (y == 0xE) {
                    if (n == 0xE) {
                        pc = stack.removeAt(0)
                        pc -=2
                    } else {
                        screen.resetPixels()
                    }
                }
            }
            1 -> {
                pc = nnn
                pc -=2
            }
            2 -> {
                stack.add(0, pc + 2)
                pc = nnn
                pc -=2
            }
            3 ->
                    if (v[x] == kk) {
                        pc += 2
                    }
            4 ->
                    if (v[x] != kk) {
                        pc += 2
                    }
            5 ->
                    if (v[x] == v[y]) {
                        pc += 2
                    }
            6 -> v[x] = kk
            7 -> v[x] = (v[x] + kk).toUByte()
            8 ->
                    when (n) {
                        0 -> v[x] = v[y]
                        1 -> v[x] = v[x].or(v[y])
                        2 -> v[x] = v[x].and(v[y])
                        3 -> v[x] = v[x].xor(v[y])
                        4 -> {
                            if (v[x] + v[y] > 255u) v[15] = VF_FLAG_ON
                            v[x] = (v[x] + v[y]).toUByte()
                        }
                        5 -> {
                            v[15] = if (v[x] > v[y]) VF_FLAG_ON else VF_CARRY_OFF
                            v[x] = (v[x] - v[y]).toUByte()
                        }
                        6 -> {
                            v[15] = if (v[x].rem(2u).equals(1)) VF_FLAG_ON else VF_CARRY_OFF
                            v[x] = v[x].div(2u).toUByte()
                        }
                        7 -> {
                            v[15] = if (v[y] > v[x]) VF_FLAG_ON else VF_CARRY_OFF
                            v[x] = (v[y] - v[x]).toUByte()
                        }
                        0xe -> {
                            v[15] = if (v[x] >= 128u) VF_FLAG_ON else VF_CARRY_OFF
                            v[x] = v[x].times(2u).toUByte()
                        }
                    }
            9 ->
                    if (v[x].toInt() != v[y].toInt()) {
                        pc += 2
                    }
            0xA -> I = nnn
            0xB -> {
                pc = (nibblesToInt(array, 3) + v[0]).toInt()
                pc -= 2
            }
            0xC -> {
                val random = Random.nextBits(8).toUInt()
                v[x] = (nibblesToInt(array, 2).and(random)).toUByte()
            }
            0xD -> drawSpriteAtXY(x, y, n)
            0xE -> {
                when (n) {
                    0x1 -> if (!Gdx.input.isKeyPressed(keyMap[v[x].toInt()])) pc += 2
                    0xE -> if (Gdx.input.isKeyPressed(keyMap[v[x].toInt()])) pc += 2
                }
            }
            0xF ->
                    when (n) {
                        7 -> v[x] = delay.toUByte()
                        0xA -> {
                            val key = isAnyValidKeyPressed()
                            if (key != null) {
                                for (i in keyMap.indices) if (keyMap[i] == key)
                                        v[x] = i.toUByte()
                            } else {
                                pc -= 2
                            }
                        }
                        8 -> sound = v[x].toInt()
                        0xE -> I += v[x].toInt()
                        9 -> I = (v[x] * 5u).toInt()
                        3 -> {
                            memory[I] = v[x].div(100u).toInt()
                            memory[I + 1] = v[x].rem(100u).div(10u).toInt()
                            memory[I + 2] = v[x].rem(10u).toInt()
                        }
                        else -> {
                            when (y) {
                                1 -> delay = v[x].toInt()
                                5 -> for (i in 0..x) memory[I + i] = v[i].toInt()
                                6 -> for (i in 0..x) v[i] = memory[I + i].toUByte()
                            }
                        }
                    }
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

    public fun drawScreen() {
        screen.draw()
    }

    public fun loadRomToMemory(romFileName: String) {
        File(romFileName).readBytes().toUByteArray().forEachIndexed { index: Int, element: UByte ->
            memory[index + 0x200] = element.toInt()
        }
    }

    private fun nibblesToInt(array: Array<Int>, n: Int, startIndex: Int = 3): UInt {

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
