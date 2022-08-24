package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.utils.ScreenUtils

@ExperimentalUnsignedTypes
class Chip8Emulator : ApplicationAdapter() {

    object screenData {
        const val FPS = 60
        const val GAME_X_OFFSET = 150
        const val SIZE_OF_SQUARE_IN_PIXELS = 15
    }

    // TODO: Make choosing a new game interactive
    private var romFileName = "roms/picture.ch8"

    private var cpu = Cpu()

    override public fun create() {
        // Implement a full machine restart for dynamic rom selection
        // restartEmulator()
        cpu.loadRomToMemory(romFileName)
    }

    override public fun render() {
        ScreenUtils.clear(Color.BLACK)
        cpu.tick()
        drawScreen()
    }

    // Independent function that draws the screen
    private fun drawScreen() {
        val shapeRenderer = ShapeRenderer()
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.setColor(Color.CORAL)
        for (col in cpu.screenPixels.indices) {
            for (row in cpu.screenPixels[col].indices) {
                if (cpu.screenPixels[col][row]) {

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

    // Converts n nibbles to Int
    // start: index of least significance bit where the conversion will start
    // n: Number of bits to convert from the start

    override public fun dispose() {}
}
