package com.ricoh.printservice

import android.content.Context
import android.os.ParcelFileDescriptor
import android.print.PrinterId
import android.print.PrinterInfo
import android.print.PrintJobInfo
import android.printservice.PrintJob
import android.printservice.PrintService
import android.printservice.PrinterDiscoverySession
import android.util.Log
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Main PrintService implementation for Ricoh Aficio 1515.
 * Handles PDF rasterization, PCL encoding, and socket communication.
 */
class RicohPrintService : PrintService() {

    private val executor = Executors.newSingleThreadExecutor()
    private val pdfRasterizer = PdfRasterizer()
    private val pclEncoder = PclEncoder()

    companion object {
        private const val TAG = "RicohPrintService"
        private const val PREFS_NAME = "RicohPrintPrefs"
        private const val KEY_PRINTER_IP = "printer_ip"
        private const val DEFAULT_IP = "192.168.0.50"
        private const val PRINTER_PORT = 9100
        private const val CONNECTION_TIMEOUT = 5000 // 5 seconds
    }

    override fun onCreatePrinterDiscoverySession(): PrinterDiscoverySession {
        return object : PrinterDiscoverySession() {
            override fun onStartPrinterDiscovery(priorityList: List<PrinterId>) {
                val printerIp = getPrinterIp()
                val printerId = generatePrinterId(printerIp)
                
                val printerInfo = PrinterInfo.Builder(printerId, "Ricoh Aficio 1515", PrinterInfo.STATUS_IDLE)
                    .setDescription("Ricoh PCL 5e Printer at $printerIp")
                    .build()
                
                addPrinters(listOf(printerInfo))
            }

            override fun onStopPrinterDiscovery() {}

            override fun onValidatePrinters(printerIds: List<PrinterId>) {}

            override fun onStartPrinterStateTracking(printerId: PrinterId) {}

            override fun onStopPrinterStateTracking(printerId: PrinterId) {}

            override fun onDestroy() {}
        }
    }

    override fun onPrintJobQueued(printJob: PrintJob) {
        Log.d(TAG, "onPrintJobQueued: ${printJob.id}")
        
        // Mark job as started
        if (printJob.isQueued) {
            printJob.start()
        }
        
        executor.execute {
            processPrintJob(printJob)
        }
    }

    override fun onRequestCancelPrintJob(printJob: PrintJob) {
        Log.d(TAG, "onRequestCancelPrintJob: ${printJob.id}")
        printJob.cancel()
    }

    private fun processPrintJob(printJob: PrintJob) {
        try {
            val document = printJob.document
            val pfd = document.data
            
            if (pfd == null) {
                Log.e(TAG, "PrintJob document data is null")
                printJob.fail("Document data is null")
                return
            }

            val printerIp = getPrinterIp()
            
            // 1. Connect to printer
            Socket().use { socket ->
                socket.connect(InetSocketAddress(printerIp, PRINTER_PORT), CONNECTION_TIMEOUT)
                socket.getOutputStream().use { outputStream ->
                    
                    // 2. Send PCL Header
                    outputStream.write(pclEncoder.getHeader())
                    
                    // 3. Rasterize and send pages one-by-one to avoid OOM
                    pdfRasterizer.rasterize(pfd, 300) { bitmap ->
                        Log.d(TAG, "Processing page: ${bitmap.width}x${bitmap.height}")
                        
                        // Encode page to PCL
                        val pageData = pclEncoder.encodePage(bitmap, 300)
                        
                        // Send page data to printer
                        outputStream.write(pageData)
                        outputStream.flush()
                        
                        // Recycle bitmap immediately to free memory
                        bitmap.recycle()
                    }
                    
                    // 4. Send PCL Footer
                    outputStream.write(pclEncoder.getFooter())
                    outputStream.flush()
                }
            }
            
            // 5. Complete the job
            printJob.complete()
            Log.d(TAG, "PrintJob completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing print job", e)
            printJob.fail(e.message ?: "Unknown error during print job processing")
        }
    }

    private fun getPrinterIp(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PRINTER_IP, DEFAULT_IP) ?: DEFAULT_IP
    }
}
