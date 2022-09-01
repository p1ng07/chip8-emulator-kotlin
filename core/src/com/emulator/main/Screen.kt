package com.emulator.main

class Screen() :  {
    
    private val ROWS = 64
    private val COLS = 32
    private var pixels = Array(ROWS) { BooleanArray(COLS, { _ -> false }) }

    public fun resetPixels() { this.pixels = Array(ROWS){BooleanArray(COLS,{_ -> false})} }

    // The origin of screen on original chip 8 implementations is on the Top left so magic
    public fun setPixel(x: Int, y: Int, p: Boolean){
        if(x >= COLS || x < 0 || y >= ROWS || y < 0) return

        pixels[x][COLS-y-1] = p
    }

    // TODO
    public fun getPixels() {
        
    }
}
