package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.Logger
import com.badlogic.gdx.utils.ScreenUtils

@ExperimentalUnsignedTypes
class Chip8Emulator : ApplicationAdapter() {

    // TODO: Sprites??? Display????
    // TODO: Figure out how to test the already implemented opcodes
    // TODO:
    // TODO: Make choosing a new game interactive
    private var romFileName = "roms/test.ch8"

    private var cpu = Cpu()

    override public fun create() {
        // Implement a full machine restart for dynamic rom selection
        // restartEmulator()
        cpu.loadRomToMemory(romFileName)
        Gdx.app.setLogLevel(Logger.INFO)
    }

    override public fun render() {
        ScreenUtils.clear(Color.YELLOW)
        cpu.tick()
        cpu.drawScreen()
    }

    // Read data from rom to memory, adress 0x0000 to 0x0200 is reserved

    // Converts n nibbles to Int
    // start: index of least significance bit where the conversion will start
    // n: Number of bits to convert from the start

    override public fun dispose() {}
}
