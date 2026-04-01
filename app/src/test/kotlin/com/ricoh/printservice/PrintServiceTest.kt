package com.ricoh.printservice

import android.graphics.Bitmap
import android.graphics.Color
import io.mockk.*
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.net.InetSocketAddress

class PrintServiceTest {

    private lateinit var pclEncoder: PclEncoder

    @Before
    fun setUp() {
        pclEncoder = PclEncoder()
        mockkStatic(Bitmap::class)
        mockkStatic(Color::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `PclEncoder should generate correct PCL header and footer`() {
        // Arrange
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns 8
        every { bitmap.height } returns 1
        every { bitmap.getPixel(any(), any()) } returns 0xFF000000.toInt() // Black
        
        every { Color.red(any()) } returns 0
        every { Color.green(any()) } returns 0
        every { Color.blue(any()) } returns 0
        
        val bitmaps = listOf(bitmap)

        // Act
        val result = pclEncoder.encode(bitmaps, 300)

        // Assert
        val resultString = result.toString(Charsets.US_ASCII)
        // Note: PclEncoder uses "$ESC E" which is ESC + space + E
        assertTrue("Should start with RESET", result.startsWith("\u001B E".toByteArray()))
        assertTrue("Should contain resolution command", resultString.contains("\u001B*t300R"))
        assertTrue("Should contain START_RASTER", resultString.contains("\u001B*r1A"))
        assertTrue("Should contain END_RASTER", resultString.contains("\u001B*rB"))
        assertTrue("Should contain FORM_FEED", resultString.contains("\u000C"))
        assertTrue("Should end with RESET", result.endsWith("\u001B E".toByteArray()))
    }

    @Test
    fun `PclEncoder should correctly encode a black pixel`() {
        // Arrange
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns 8
        every { bitmap.height } returns 1
        
        // First pixel black, others white
        every { bitmap.getPixel(0, 0) } returns 0xFF000000.toInt()
        for (i in 1 until 8) {
            every { bitmap.getPixel(i, 0) } returns 0xFFFFFFFF.toInt()
        }
        
        every { Color.red(0xFF000000.toInt()) } returns 0
        every { Color.green(0xFF000000.toInt()) } returns 0
        every { Color.blue(0xFF000000.toInt()) } returns 0
        
        every { Color.red(0xFFFFFFFF.toInt()) } returns 255
        every { Color.green(0xFFFFFFFF.toInt()) } returns 255
        every { Color.blue(0xFFFFFFFF.toInt()) } returns 255

        val bitmaps = listOf(bitmap)

        // Act
        val result = pclEncoder.encode(bitmaps, 300)

        // Assert
        // The row data for 10000000 should be 0x80 (128)
        // PCL command for row data is ESC*v1W followed by the byte
        val expectedRowData = byteArrayOf(0x1B, 0x2A, 0x76, 0x31, 0x57, 0x80.toByte())
        assertTrue("Should contain correct row data for black pixel", result.contains(expectedRowData))
    }

    @Test
    fun `PclEncoder should correctly encode Arabic-like pattern`() {
        // Arrange
        val bitmap = mockk<Bitmap>()
        every { bitmap.width } returns 16
        every { bitmap.height } returns 1
        
        // Pattern: B W B W B W B W | W B W B W B W B
        for (i in 0 until 16) {
            val isBlack = if (i < 8) i % 2 == 0 else i % 2 != 0
            val color = if (isBlack) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            every { bitmap.getPixel(i, 0) } returns color
        }
        
        every { Color.red(0xFF000000.toInt()) } returns 0
        every { Color.green(0xFF000000.toInt()) } returns 0
        every { Color.blue(0xFF000000.toInt()) } returns 0
        
        every { Color.red(0xFFFFFFFF.toInt()) } returns 255
        every { Color.green(0xFFFFFFFF.toInt()) } returns 255
        every { Color.blue(0xFFFFFFFF.toInt()) } returns 255

        val bitmaps = listOf(bitmap)

        // Act
        val result = pclEncoder.encode(bitmaps, 300)

        // Assert
        val expectedRowData = byteArrayOf(0x1B, 0x2A, 0x76, 0x32, 0x57, 0xAA.toByte(), 0x55.toByte())
        assertTrue("Should contain correct row data for pattern", result.contains(expectedRowData))
    }

    @Test
    fun `RicohPrintService should send data to printer via socket`() {
        // Arrange
        val mockOutputStream = ByteArrayOutputStream()
        
        mockkConstructor(Socket::class)
        every { anyConstructed<Socket>().connect(any(), any()) } returns Unit
        every { anyConstructed<Socket>().getOutputStream() } returns mockOutputStream
        every { anyConstructed<Socket>().close() } returns Unit
        
        val testData = "Test PCL Data".toByteArray()
        val printerIp = "192.168.0.50"
        
        val realService = RicohPrintService()
        val method = RicohPrintService::class.java.getDeclaredMethod("sendToPrinter", String::class.java, ByteArray::class.java)
        method.isAccessible = true
        
        // Act
        method.invoke(realService, printerIp, testData)
        
        // Assert
        // Verify data was written to output stream
        assertTrue("Data should be written to socket output stream", mockOutputStream.toByteArray().contentEquals(testData))
    }

    /**
     * Helper extension to check if a ByteArray contains another ByteArray.
     */
    private fun ByteArray.contains(other: ByteArray): Boolean {
        if (other.isEmpty()) return true
        for (i in 0..this.size - other.size) {
            var found = true
            for (j in other.indices) {
                if (this[i + j] != other[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }

    private fun ByteArray.startsWith(other: ByteArray): Boolean {
        if (this.size < other.size) return false
        for (i in other.indices) {
            if (this[i] != other[i]) return false
        }
        return true
    }

    private fun ByteArray.endsWith(other: ByteArray): Boolean {
        if (this.size < other.size) return false
        for (i in other.indices) {
            if (this[this.size - other.size + i] != other[i]) return false
        }
        return true
    }
}
