package com.emulator.main

import java.io.File
import java.util.Timer
import java.util.Vector
import kotlin.concurrent.timer
import kotlin.math.*

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
    private var i = 0

    public var screenPixels = Array(64) { BooleanArray(32, { _ -> false }) }

    public fun tick() {
        // for (i in 1..screenData.FPS / CPU_FREQUENCY) executeOpCode(fetchCurrentCommand())
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
                            v[second] = v[second].div(2u).toUByte()
                        }
                        7 -> {
                            v[15] = if (v[third] > v[second]) 0xffu else 0u
                            v[second] = v[third].minus(v[second]).toUByte()
                        }
                        0xe -> {
                            v[15] = if (v[second] >= 128u) 0xffu else 0u
                            v[second] = v[second].times(2u).toUByte()
                        }
                    }
        }
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
        return res
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
}
