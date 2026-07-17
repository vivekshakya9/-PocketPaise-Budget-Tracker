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
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportToPdf(context: Context, transactions: List<Transaction>, totalIncome: Double, totalExpense: Double) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Title
        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        canvas.drawText("PocketPaise Financial Report", 40f, 60f, paint)

        // Summary
        paint.textSize = 16f
        paint.isFakeBoldText = false
        canvas.drawText("Total Income: ₹$totalIncome", 40f, 100f, paint)
        canvas.drawText("Total Expense: ₹$totalExpense", 40f, 125f, paint)
        canvas.drawText("Net Balance: ₹${totalIncome - totalExpense}", 40f, 150f, paint)

        // Headers
        paint.isFakeBoldText = true
        var yPos = 200f
        canvas.drawText("Date", 40f, yPos, paint)
        canvas.drawText("Title", 160f, yPos, paint)
        canvas.drawText("Type", 320f, yPos, paint)
        canvas.drawText("Amount", 440f, yPos, paint)

        // List
        paint.isFakeBoldText = false
        paint.textSize = 12f
        yPos += 30f
        
        transactions.forEach { tx ->
            if (yPos > 800f) {
                // To keep it simple, we don't paginate yet, just cut off or implement pagination
                // Here we just stop drawing if it exceeds a page for simplicity.
                return@forEach
            }
            canvas.drawText(getFormattedDate(tx.timestamp).substring(0, 11), 40f, yPos, paint)
            val shortTitle = if (tx.title.length > 15) tx.title.substring(0, 15) + ".." else tx.title
            canvas.drawText(shortTitle, 160f, yPos, paint)
            canvas.drawText(tx.type, 320f, yPos, paint)
            canvas.drawText("₹${tx.amount}", 440f, yPos, paint)
            yPos += 25f
        }

        pdfDocument.finishPage(page)

        val fileName = "PocketPaise_Report_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
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
