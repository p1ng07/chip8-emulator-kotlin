package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.utils.ScreenUtils

@ExperimentalUnsignedTypes
class Chip8Emulator : ApplicationAdapter() {

    private var romFileName = "roms/breakout.ch8"

    private var cpu = Cpu(traceMode= false)

    override public fun create() {
        cpu.loadRomToMemory(romFileName)
        Gdx.app.setLogLevel(Logger.DEBUG)
    }

    override public fun render() {
        ScreenUtils.clear(Color.YELLOW)
        cpu.tick()
        cpu.drawScreen()
    }

    override public fun dispose() {
        System.exit(0)
    }
}
