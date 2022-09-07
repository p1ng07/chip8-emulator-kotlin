package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.utils.ScreenUtils

@ExperimentalUnsignedTypes
class Chip8Emulator : ApplicationAdapter() {

    private var romFileName = "roms/space_invaders.ch8"

    private var cpu = Cpu()

    override public fun create() {
        cpu.loadRomToMemory(romFileName)
        Gdx.app.setLogLevel(Logger.NONE)
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
