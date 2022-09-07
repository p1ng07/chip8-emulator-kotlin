package com.emulator.main

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType

class Screen() {

    public object data {
        const val FPS = 60
        const val GAME_X_OFFSET = 150
        const val SIZE_OF_SQUARE_IN_PIXELS = 15
        const val ROWS = 32
        const val COLS = 64
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

        pixels[data.ROWS - y - 1][x] = p.xor(pixels[data.ROWS - y - 1][x])
    }

    public fun getPixels(x: Int, y: Int): Boolean {
        if (x >= data.COLS || x < 0 || y >= data.ROWS || y < 0) {
            throw Exception("getPixels() in Screen.kt, overflow or underflow of parameters")
        } else return pixels[data.ROWS - y - 1][x]
    }

    public fun draw() {
        val shapeRenderer = ShapeRenderer()
        shapeRenderer.begin(ShapeType.Filled)
        shapeRenderer.setColor(GAME_BG_COLOR)

        for (row in pixels.indices) {
            for (col in pixels[row].indices) {
                if (pixels[row][col]) shapeRenderer.setColor(GAME_FG_COLOR)
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
