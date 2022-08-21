package com.emulator.main.Chip8Emulator

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ScreenUtils

class Chip8Emulator : ApplicationAdapter() {

    // Initialize integer array with all elements to zero
    private val array = IntArray(16, { _ -> 0 })

    override public fun create() {
        println(array.get(0))
    }

    override public fun render() {
        ScreenUtils.clear(Color.BLACK)
    }

    override public fun dispose() {}
}
