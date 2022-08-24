package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.utils.ScreenUtils
import java.io.File
import java.util.Timer
import java.util.Vector
import kotlin.concurrent.timer
import kotlin.math.pow

@ExperimentalUnsignedTypes
class Chip8Emulator : ApplicationAdapter() {

    object screenData {
        const val FPS = 60
        const val GAME_X_OFFSET = 150
        const val SIZE_OF_SQUARE_IN_PIXELS = 15
    }

    // Number of opcodes to be executed per second (HZ)
    val CPU_FREQUENCY = 500

    private val PC_START = 0x200

    // TODO: Make choosing a new game interactive
    private var romFileName = "roms/picture.ch8"

    private var pc = PC_START
    private var stack = Vector<Int>()
    private var memory = UByteArray(4096)
    private var v = UByteArray(16)
    private var i = 0
    private var screenPixels = Array(64) { BooleanArray(32, { _ -> false }) }

    private var oneSecondTimer = timerReset()
    private var delay = 60
    private var sound = 60

    /*List of things to do:*/
    /*1 - Determine the representation of the emulator parts */
    /*2 - Figure out how to read the rom*/
    /*3 - Map the codes read on the rom to operations on the data*/
    /* TO FILL */
    override public fun create() {
        // Implement a full machine restart for dynamic rom selection
        // restartEmulator()
        loadRomToMemory()
        for (i in fetchCurrentCommand()) println(i)
    }

    // We want to execute about 600-1000 opcodes per second, so 15 per timer tick
    override public fun render() {
        ScreenUtils.clear(Color.BLACK)

        // for (i in 1..screenData.FPS / CPU_FREQUENCY) executeOpCode(fetchCurrentCommand())

        drawScreen()
    }

    // Independent function that draws the screen
    private fun drawScreen() {
        val shapeRenderer = ShapeRenderer()
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.setColor(Color.CORAL)
        for (col in screenPixels.indices) {
            for (row in screenPixels[col].indices) {
                if (screenPixels[col][row]) {

                    shapeRenderer.rect(
                            (screenData.GAME_X_OFFSET + screenData.SIZE_OF_SQUARE_IN_PIXELS * col)
                                    .toFloat(),
                            (screenData.SIZE_OF_SQUARE_IN_PIXELS * row).toFloat(),
                            (screenData.SIZE_OF_SQUARE_IN_PIXELS).toFloat(),
                            (screenData.SIZE_OF_SQUARE_IN_PIXELS).toFloat()
                    )
                }
            }
        }
        shapeRenderer.end()
    }

    // Read data from rom to memory, adress 0x0000 to 0x0200 is reserved
    private fun loadRomToMemory() {
        File(romFileName).readBytes().toUByteArray().forEachIndexed { index: Int, element: UByte ->
            memory[index + PC_START] = element
        }
    }

    // Converts n nibbles to Int
    // start: index of least significance bit where the conversion will start
    // n: Number of bits to convert from the start
    private fun nibblesToInt(array: Array<Int>, n: Int, startIndex: Int = 3): UInt {

        if (startIndex > 3 || startIndex < 0) throw Exception("Nibbles to int use was invalid")
        if (startIndex - n + 1 < 0) throw Exception("Nibbles to int use was invalid")

        var res = 0u
        for (i in startIndex downTo startIndex - n + 1) {
            res += array[i].toUInt() * 16f.pow(startIndex - i).toUInt()
        }
        return res
    }

    private fun executeOpCode(array: Array<Int>) {

        val first = array[0]
        val second = array[1]
        val third = array[2]
        val fourth = array[3]

        when (first) {
            0 ->
                    if (fourth == 0xE) {
                        pc = stack.firstElement()
                        stack.remove(0)
                    } else screenPixels = Array(64) { BooleanArray(32, { _ -> false }) }
            1 -> pc = nibblesToInt(array, 3).toInt()
            2 -> {
                stack.add(pc)
                pc = nibblesToInt(array, 3).toInt()
            }
            3 -> if (v[second].equals(nibblesToInt(array, 2))) pc += 2
            4 -> if (!v[second].equals(nibblesToInt(array, 2))) pc += 2
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
                            v[15] = if (v[second] > v[third]) 0xffu else 0u
                            v[second] = v[second].minus(v[third]).toUByte()
                        }
                        6 -> {
                            v[15] = if (v[second].toInt() % 2 == 1) 0xffu else 0u
                            v[second] = v[second].div(2).toUByte()
                        }
                    }
            9 -> 0
        }
    }

    // Returns the nibbles of the command in memory at index pc
    // These numbers are also stored Big-Endian (the most significant bit is on the right)
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

    override public fun dispose() {}
}
