package com.emulator.main

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("Shit")
        var pixelSizeOfSquare = 10
        config.setTitle("Chip 8 emulator")
        config.setResizable(false)
        config.setWindowedMode(64 * pixelSizeOfSquare, 32 * pixelSizeOfSquare)

        Lwjgl3Application(Chip8Emulator(), config)
    }
}
