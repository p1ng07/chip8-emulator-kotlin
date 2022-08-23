package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ScreenUtils
import java.io.File
import java.util.Timer
import java.util.Vector
import kotlin.concurrent.timer

class Chip8Emulator : ApplicationAdapter() {

    private val PC_START = 0x200
    // TODO: Make choosing a new game interactive
    private val romFileName = "roms/breakout.ch8"

    private var pc = PC_START
    private var stack = Vector<Int>()
    private var memory = ByteArray(4096)
    private var v = IntArray(16, { _ -> 0 })
    private var i = 0
    private var screenPixels = Array(64) { BooleanArray(32) }

    private var oneSecondTimer = timerReset()
    private var delay = 60
    private var sound = 60

    /*List of things to do:*/
    /*1 - Determine the representation of the emulator parts */
    /*2 - Figure out how to read the rom*/
    /*3 - Map the codes read on the rom to operations on the data*/
    /* TO FILL */
    override public fun create() {
        File(romFileName).readBytes().forEachIndexed { index: Int, element: Byte ->
            memory[index] = element
        }
        if (memory == File(romFileName).readBytes()) println("Success") else println("Uh Oh")

        timerReset()
    }

    override public fun render() {
        ScreenUtils.clear(Color.BLACK)
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
