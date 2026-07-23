package com.vsdigitallabs.pocketpaise.stdbt.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtil {

    private fun getFormattedDate(timestamp: Long): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun exportToCsv(context: Context, transactions: List<Transaction>) {
        val fileName = "PocketPaise_Report_${System.currentTimeMillis()}.csv"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        try {
            val writer = file.bufferedWriter()
            writer.write("Date,Title,Type,Category,Amount,Notes\n")
            
            transactions.forEach { tx ->
                val date = getFormattedDate(tx.timestamp)
                val title = tx.title.replace(",", " ")
                val type = tx.type
                val cat = tx.category
                val amt = tx.amount.toString()
                val notes = tx.notes.replace(",", " ").replace("\n", " ")
                writer.write("$date,$title,$type,$cat,$amt,$notes\n")
            }
            writer.close()
            android.widget.Toast.makeText(context, "Exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    fun exportToPdf(context: Context, transactions: List<Transaction>, totalIncome: Double, totalExpense: Double) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        val paint = Paint()

        val primaryColor = Color.parseColor("#4f46e5")
        val greenColor = Color.parseColor("#10b981")
        val redColor = Color.parseColor("#ef4444")
        val lightGray = Color.parseColor("#f3f4f6")
        
        fun drawHeaderAndSummary() {
            // Header Background
            paint.color = primaryColor
            canvas.drawRect(0f, 0f, 595f, 100f, paint)

            // Title
            paint.color = Color.WHITE
            paint.textSize = 28f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("PocketPaise Financial Report", 595f / 2, 60f, paint)

            // Summary Info
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 14f
            paint.color = Color.BLACK
            
            val generatedOn = "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}"
            canvas.drawText(generatedOn, 40f, 140f, paint)

            // Income / Expense / Balance Summary
            paint.textSize = 16f
            paint.isFakeBoldText = true
            
            paint.color = greenColor
            canvas.drawText("Total Income: ₹$totalIncome", 40f, 180f, paint)
            
            paint.color = redColor
            canvas.drawText("Total Expense: ₹$totalExpense", 250f, 180f, paint)
            
            val net = totalIncome - totalExpense
            paint.color = if (net >= 0) primaryColor else redColor
            canvas.drawText("Net Balance: ₹$net", 450f, 180f, paint)
        }

        fun drawTableHeader(yPos: Float) {
            paint.color = primaryColor
            canvas.drawRect(40f, yPos - 20f, 555f, yPos + 10f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("Date", 50f, yPos, paint)
            canvas.drawText("Title", 150f, yPos, paint)
            canvas.drawText("Category", 300f, yPos, paint)
            canvas.drawText("Type", 420f, yPos, paint)
            canvas.drawText("Amount", 490f, yPos, paint)
        }

        drawHeaderAndSummary()
        var yPos = 240f
        drawTableHeader(yPos)
        
        yPos += 30f
        paint.isFakeBoldText = false
        paint.textSize = 12f
        var isAltRow = false

        for (tx in transactions) {
            if (yPos > 800f) {
                pdfDocument.finishPage(page)
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f
                drawTableHeader(yPos)
                yPos += 30f
                paint.isFakeBoldText = false
                paint.textSize = 12f
            }

            // Draw alternating row background
            if (isAltRow) {
                paint.color = lightGray
                canvas.drawRect(40f, yPos - 15f, 555f, yPos + 5f, paint)
            }
            
            paint.color = Color.BLACK
            val dateStr = getFormattedDate(tx.timestamp).substring(0, 11)
            val titleStr = if (tx.title.length > 18) tx.title.substring(0, 18) + ".." else tx.title
            val catStr = if (tx.category.length > 15) tx.category.substring(0, 15) + ".." else tx.category
            
            canvas.drawText(dateStr, 50f, yPos, paint)
            canvas.drawText(titleStr, 150f, yPos, paint)
            canvas.drawText(catStr, 300f, yPos, paint)
            
            paint.color = if (tx.type == "INCOME") greenColor else redColor
            paint.isFakeBoldText = true
            canvas.drawText(tx.type, 420f, yPos, paint)
            canvas.drawText("₹${tx.amount}", 490f, yPos, paint)
            
            paint.isFakeBoldText = false
            yPos += 20f
            isAltRow = !isAltRow
        }

        pdfDocument.finishPage(page)

        val fileName = "PocketPaise_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            android.widget.Toast.makeText(context, "Exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
