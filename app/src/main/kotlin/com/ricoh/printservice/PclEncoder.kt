package com.ricoh.printservice

import android.graphics.Bitmap
import android.graphics.Color
import java.io.ByteArrayOutputStream

/**
 * Converts Android Bitmaps into PCL 5e raster graphics sequences.
 * Optimized for Ricoh Aficio 1515 and similar PCL 5e compatible printers.
 */
class PclEncoder {

    companion object {
        private const val ESC = "\u001B"
        private const val RESET = "$ESC E"
        private const val START_RASTER = "$ESC*r1A"
        private const val END_RASTER = "$ESC*rB"
        private const val FORM_FEED = "\u000C"
        
        /**
         * Sets the raster graphics resolution.
         * ESC * t # R
         */
        private fun setResolution(dpi: Int) = "$ESC*t${dpi}R"

        /**
         * Transfer Raster Data command (Plane-based).
         * ESC * v # W [data]
         * As requested in the task description for Ricoh PCL 5e.
         */
        private fun transferRasterDataPlane(size: Int) = "$ESC*v${size}W"
        
        /**
         * Transfer Raster Data command (Row-based).
         * ESC * b # W [data]
         * Listed as a key PCL sequence.
         */
        private fun transferRasterDataRow(size: Int) = "$ESC*b${size}W"
    }

    /**
     * Returns the PCL header (Reset command).
     */
    fun getHeader(): ByteArray = RESET.toByteArray()

    /**
     * Returns the PCL footer (Reset command).
     */
    fun getFooter(): ByteArray = RESET.toByteArray()

    /**
     * Encodes a list of Bitmaps into a PCL 5e byte stream.
     *
     * @param bitmaps List of Bitmaps to encode (one per page).
     * @param dpi Resolution in dots per inch (default 300).
     * @return Byte array containing PCL commands and raster data.
     */
    fun encode(bitmaps: List<Bitmap>, dpi: Int = 300): ByteArray {
        val output = ByteArrayOutputStream()
        
        output.write(getHeader())
        
        for (bitmap in bitmaps) {
            output.write(encodePage(bitmap, dpi))
        }
        
        output.write(getFooter())
        
        return output.toByteArray()
    }

    /**
     * Encodes a single Bitmap as a PCL page.
     *
     * @param bitmap The Bitmap to encode.
     * @param dpi Resolution in dots per inch (default 300).
     * @return Byte array containing PCL commands for the page.
     */
    fun encodePage(bitmap: Bitmap, dpi: Int = 300): ByteArray {
        val output = ByteArrayOutputStream()
        
        // Set resolution for the raster data
        output.write(setResolution(dpi).toByteArray())
        
        // Start raster graphics at current cursor position
        output.write(START_RASTER.toByteArray())
        
        val height = bitmap.height
        
        // Process each row of the bitmap
        for (y in 0 until height) {
            val rowData = getRowData(bitmap, y)
            
            // Transfer raster data for the current row using ESC*v#W as requested
            // Note: In monochrome, a plane transfer of one row is equivalent to a row transfer.
            output.write(transferRasterDataPlane(rowData.size).toByteArray())
            output.write(rowData)
        }
        
        // End raster graphics
        output.write(END_RASTER.toByteArray())
        
        // Form Feed to eject the page
        output.write(FORM_FEED.toByteArray())
        
        return output.toByteArray()
    }

    /**
     * Extracts a row of pixels from the bitmap and converts it to 1-bit monochrome data.
     * In PCL raster data: 1 = Black, 0 = White.
     *
     * @param bitmap The source bitmap.
     * @param y The row index.
     * @return Byte array where each bit represents a pixel.
     */
    private fun getRowData(bitmap: Bitmap, y: Int): ByteArray {
        val width = bitmap.width
        val byteWidth = (width + 7) / 8
        val row = ByteArray(byteWidth)
        
        // Optimize by fetching the entire row of pixels at once
        val pixels = IntArray(width)
        bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
        
        for (x in 0 until width) {
            val pixel = pixels[x]
            
            // Calculate luminance: 0.299R + 0.587G + 0.114B
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)
            val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            
            // Threshold at 128. If darker than 128, it's black (1).
            if (luminance < 128) {
                val byteIndex = x / 8
                val bitIndex = 7 - (x % 8)
                row[byteIndex] = (row[byteIndex].toInt() or (1 shl bitIndex)).toByte()
            }
        }
        
        return row
    }
}
