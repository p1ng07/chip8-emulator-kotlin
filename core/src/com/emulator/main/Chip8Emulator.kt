package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ScreenUtils
import java.util.Timer
import java.util.Vector
import kotlin.concurrent.timer

class Chip8Emulator : ApplicationAdapter() {

    private var stack = Vector<Int>()
    private var memory = IntArray(4096, { _ -> 0 })
    private var v = IntArray(16, { _ -> 0 })
    private var i = 0
    private var oneSecondTimer = 0.0
    private var oneSecondTime = Timer()
    private var delay = 60
    private var sound = 60

    private var test = 0
    private var timerWork = false

    private fun timerReset() {
        this.oneSecondTime =
                timer(
                        "OneSecondTimer",
                        false,
                        0,
                        1000,
                        {
                            delay--
                            sound--
                            test++
                            if (delay == -1) delay = 60
                            if (sound == -1) sound = 60
                            println("A second has passed: $test")
                        }
                )
    }

    /*List of things to do:*/
    /*1 - Determine the representation of the emulator parts */
    /*2 - Figure out how to read the rom*/
    /*3 - Map the codes read on the rom to operations on the data*/
    /* TO FILL */
    override public fun create() {
        timerReset()
        println(this.test)
    }

    override public fun render() {
        ScreenUtils.clear(Color.BLACK)
    }

    override public fun dispose() {}
}
