package com.emulator.main

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType

class Screen() {

    public object data {
        const val FPS = 15
        const val GAME_X_OFFSET = 150
        const val SIZE_OF_SQUARE_IN_PIXELS = 15
        const val ROWS = 64
        const val COLS = 32
    }

    val GAME_BG_COLOR = Color.BLACK
    val GAME_FG_COLOR = Color.WHITE
    val BG_COLOR = Color.YELLOW

    private var pixels = Array(data.ROWS) { BooleanArray(data.COLS, { _ -> false }) }

    public fun resetPixels() {
        this.pixels = Array(data.ROWS) { BooleanArray(data.COLS, { _ -> false }) }
    }

    // The origin of screen on original chip 8 implementations is on the Top left so magic
    public fun setPixel(x: Int, y: Int, p: Boolean) {
        if (x >= data.COLS || x < 0 || y >= data.ROWS || y < 0) return

        pixels[x][data.COLS - y - 1] = p.xor(pixels[x][data.COLS - y - 1])
    }

    public fun getPixels(x: Int, y: Int): Boolean {
        if (x >= data.COLS || x < 0 || y >= data.ROWS || y < 0) {
            throw Exception("getPixels() in Screen.kt, overflow or underflow of parameters")
        } else return pixels[x][data.COLS - y - 1]
    }

    public fun draw() {
        val shapeRenderer = ShapeRenderer()
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.setColor(GAME_BG_COLOR)

        for (col in pixels.indices) {
            for (row in pixels[col].indices) {
                if (pixels[col][row]) shapeRenderer.setColor(GAME_FG_COLOR)
                else shapeRenderer.setColor(GAME_BG_COLOR)

                shapeRenderer.rect(
                        (data.GAME_X_OFFSET + data.SIZE_OF_SQUARE_IN_PIXELS * col).toFloat(),
                        (data.SIZE_OF_SQUARE_IN_PIXELS * row).toFloat(),
                        (data.SIZE_OF_SQUARE_IN_PIXELS).toFloat(),
                        (data.SIZE_OF_SQUARE_IN_PIXELS).toFloat()
                )
            }
        }
        shapeRenderer.end()
    }
}
