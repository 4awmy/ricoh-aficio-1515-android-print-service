package com.ricoh.printservice

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.IOException

/**
 * Utility to convert PDF pages to monochrome Bitmaps using Android's PdfRenderer.
 * Optimized for Ricoh Aficio 1515 PCL 5e printing.
 */
class PdfRasterizer {

    /**
     * Rasterizes a PDF file page by page.
     *
     * @param pdfFile The PDF file to rasterize.
     * @param dpi The resolution for rasterization (default 300 DPI).
     * @param onPage Callback invoked for each rasterized page.
     * @throws IOException If the file cannot be opened or rendered.
     */
    @Throws(IOException::class)
    fun rasterize(pdfFile: File, dpi: Int = 300, onPage: (Bitmap) -> Unit) {
        val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            ?: throw IOException("Failed to open PDF file descriptor")
        rasterize(pfd, dpi, onPage)
    }

    /**
     * Rasterizes a PDF from a ParcelFileDescriptor page by page.
     *
     * @param pfd The ParcelFileDescriptor of the PDF.
     * @param dpi The resolution for rasterization (default 300 DPI).
     * @param onPage Callback invoked for each rasterized page.
     * @throws IOException If the file cannot be rendered.
     */
    @Throws(IOException::class)
    fun rasterize(pfd: ParcelFileDescriptor, dpi: Int = 300, onPage: (Bitmap) -> Unit) {
        val renderer = try {
            PdfRenderer(pfd)
        } catch (e: Exception) {
            pfd.close()
            throw IOException("Failed to initialize PdfRenderer", e)
        }

        try {
            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                try {
                    // PDF points are 1/72 inch. Calculate dimensions based on target DPI.
                    val width = (page.width * dpi / 72)
                    val height = (page.height * dpi / 72)

                    // Create a monochrome-compatible bitmap (ARGB_8888 is standard for rendering)
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    // Render the PDF page onto the bitmap
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                    
                    // Invoke callback with the rasterized page
                    onPage(bitmap)
                } finally {
                    page.close()
                }
            }
        } finally {
            renderer.close()
            pfd.close()
        }
    }
}
