package com.emulator.main

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType
import com.badlogic.gdx.utils.ScreenUtils
import java.io.File
import java.util.Timer
import java.util.Vector
import kotlin.concurrent.timer

@ExperimentalUnsignedTypes
class Chip8Emulator : ApplicationAdapter() {

    object screenData {
        const val FPS = 60
        const val GAME_X_OFFSET = 150
        const val SIZE_OF_SQUARE_IN_PIXELS = 15
    }

    val CPU_FREQUENCY = 500
    private val PC_START = 0x200

    // TODO: Make choosing a new game interactive
    private var romFileName = "roms/picture.ch8"

    private var pc = PC_START
    private var stack = Vector<Int>()
    private var memory = UByteArray(4096)
    private var v = UByteArray(16)
    private var i = 0
    private var screenPixels = Array(64) { BooleanArray(32, { _ -> true }) }

    private var oneSecondTimer = timerReset()
    private var delay = 60
    private var sound = 60

    /*List of things to do:*/
    /*1 - Determine the representation of the emulator parts */
    /*2 - Figure out how to read the rom*/
    /*3 - Map the codes read on the rom to operations on the data*/
    /* TO FILL */
    override public fun create() {
        // Implement a full machine restart for dynamic rom selection
        // restartEmulator()
        loadRomToMemory()
    }

    // We want to execute about 600-1000 opcodes per second, so 15 per timer tick
    override public fun render() {
        ScreenUtils.clear(Color.BLACK)

        for (i in 1..screenData.FPS / CPU_FREQUENCY) executeOpCode(fetchCurrentCommand())

        drawScreen()
    }

    // Independent function that draws the screen
    private fun drawScreen() {
        val shapeRenderer = ShapeRenderer()
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.setColor(Color.CORAL)
        for (col in screenPixels.indices) for (row in screenPixels[col].indices) if (screenPixels[
                        col][row]
        )
                shapeRenderer.rect(
                        (screenData.GAME_X_OFFSET + screenData.SIZE_OF_SQUARE_IN_PIXELS * col)
                                .toFloat(),
                        (screenData.SIZE_OF_SQUARE_IN_PIXELS * row).toFloat(),
                        (screenData.SIZE_OF_SQUARE_IN_PIXELS).toFloat(),
                        (screenData.SIZE_OF_SQUARE_IN_PIXELS).toFloat()
                )
        shapeRenderer.end()
    }

    // Read data from rom to memory, adress 0x0000 to 0x0200 is reserved
    private fun loadRomToMemory() {
        File(romFileName).readBytes().toUByteArray().forEachIndexed { index: Int, element: UByte ->
            memory[index + PC_START] = element
        }
    }

    private fun executeOpCode(array: Array<UByte>) {
        // https://en.wikipedia.org/wiki/CHIP-8#Opcode_table
        // when(array[0]){
        //     1 -> {
        //         if (array[3] == 0xe) {

        //         }

        //     }
        //     2 ->
        //     3 ->
        //     4 ->
        //     5 ->
        //     6 ->
        // }
    }

    private fun fetchCurrentCommand(): Array<UByte> {
        val array = Array<UByte>(4, { _ -> 0u })
        array[0] = (memory.get(pc).and(0xF0.toUByte()).toInt() shr 4).toUByte()
        array[1] = memory.get(pc).and(0x0F.toUByte())
        array[2] = (memory.get(pc + 1).and(0xF0.toUByte()).toInt() shr 4).toUByte()
        array[3] = memory.get(pc + 1).and(0x0F.toUByte())
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

    override public fun dispose() {}
}
