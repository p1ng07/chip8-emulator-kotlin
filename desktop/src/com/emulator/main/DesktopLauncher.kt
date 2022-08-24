package com.emulator.main

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {

    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("Chip 8 emulator")
        config.setResizable(false)
        val yes = Chip8Emulator.screenData
        config.setForegroundFPS(yes.FPS)

        config.setWindowedMode(
                yes.GAME_X_OFFSET + 64 * yes.SIZE_OF_SQUARE_IN_PIXELS,
                32 * yes.SIZE_OF_SQUARE_IN_PIXELS
        )

        Lwjgl3Application(Chip8Emulator(), config)
    }
}
