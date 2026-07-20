package com.vsdigitallabs.pocketpaise.stdbt.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    suspend fun exportTransactionsToPdf(context: Context, transactions: List<Transaction>) {
        withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size (approx 72 ppi)
                var page = pdfDocument.startPage(pageInfo)
                var canvas: Canvas = page.canvas

                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                }
                
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 24f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }

                val headerPaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 14f
                    isFakeBoldText = true
                }

                val textPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                }
                
                // Draw Title
                canvas.drawText("PocketPaise - Passbook Statement", pageInfo.pageWidth / 2f, 50f, titlePaint)
                
                // Date generated
                val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                canvas.drawText("Generated on: ${sdf.format(Date())}", 50f, 90f, textPaint)

                // Table Headers
                var currentY = 140f
                canvas.drawText("Date", 50f, currentY, headerPaint)
                canvas.drawText("Category", 150f, currentY, headerPaint)
                canvas.drawText("Title", 280f, currentY, headerPaint)
                canvas.drawText("Type", 430f, currentY, headerPaint)
                canvas.drawText("Amount", 500f, currentY, headerPaint)
                
                currentY += 10f
                canvas.drawLine(50f, currentY, 545f, currentY, paint)
                currentY += 20f
                
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                for (tx in transactions) {
                    if (currentY > 800f) {
                        // Create a new page if the current one is full
                        pdfDocument.finishPage(page)
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = 50f
                    }

                    val dateStr = dateFormatter.format(Date(tx.timestamp))
                    val amountStr = String.format("%.2f", tx.amount)

                    canvas.drawText(dateStr, 50f, currentY, textPaint)
                    
                    // Truncate category if needed
                    var catStr = tx.category
                    if (catStr.length > 15) catStr = catStr.substring(0, 12) + "..."
                    canvas.drawText(catStr, 150f, currentY, textPaint)
                    
                    // Truncate title if needed
                    var titleStr = tx.title
                    if (titleStr.length > 20) titleStr = titleStr.substring(0, 17) + "..."
                    canvas.drawText(titleStr, 280f, currentY, textPaint)
                    
                    canvas.drawText(tx.type, 430f, currentY, textPaint)
                    canvas.drawText(amountStr, 500f, currentY, textPaint)
                    
                    currentY += 25f
                }

                pdfDocument.finishPage(page)

                // Save Document
                val fileName = "PocketPaise_Statement_${System.currentTimeMillis()}.pdf"
                var outputStream: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PocketPaise")
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        outputStream = resolver.openOutputStream(uri)
                    }
                } else {
                    val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val pocketPaiseDir = File(downloadDir, "PocketPaise")
                    if (!pocketPaiseDir.exists()) pocketPaiseDir.mkdirs()
                    val file = File(pocketPaiseDir, fileName)
                    outputStream = FileOutputStream(file)
                }

                outputStream?.use {
                    pdfDocument.writeTo(it)
                }
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF saved to Downloads/PocketPaise", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
