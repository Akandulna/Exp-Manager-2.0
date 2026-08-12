package com.example.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.example.data.TransactionEntity
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

object PdfTransactionParser {

    suspend fun parsePdfUri(context: Context, uri: Uri): ParseResult = withContext(Dispatchers.IO) {
        try {
            PDFBoxResourceLoader.init(context)

            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext ParseResult.Error("Could not open PDF file stream.")

            // Create temporary file
            val tempFile = File.createTempFile("statement_parse_", ".pdf", context.cacheDir)
            tempFile.deleteOnExit()

            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            // Also check page count just for info
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(pfd)
            val pageCount = pdfRenderer.pageCount
            pdfRenderer.close()
            pfd.close()

            val fileName = getFileName(context, uri)
            val statementSource = "Slice Bank Statement"

            // Use PDFBox to extract text
            val document = PDDocument.load(tempFile)
            val stripper = PDFTextStripper()
            val extractedText = stripper.getText(document)
            document.close()

            val extractedTransactions = parseTransactionsFromText(extractedText, statementSource)

            ParseResult.Success(
                transactions = extractedTransactions,
                pageCount = pageCount,
                fileName = fileName,
                sourceName = statementSource
            )
        } catch (e: Exception) {
            ParseResult.Error("Failed to parse PDF: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun parseTransactionsFromText(text: String, source: String): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        var seq = 0L

        // Regex for Date: "11 Mar '26"
        val dateRegex = Regex("^(\\d{2}\\s+[A-Za-z]{3}\\s+'\\d{2})\\s+(.*)")
        // Regex for End of Transaction: e.g. "804260706767544 -₹34 ₹973.6"
        val endRegex = Regex("(?:^|\\s)(\\d{9,30})\\s+([+-]?₹?[\\d,]+(?:\\.\\d+)?)\\s+([+-]?₹?[\\d,]+(?:\\.\\d+)?)$")

        val lines = text.lines()
        var currentDate = ""
        var currentDesc = ""

        for (line in lines) {
            val dateMatch = dateRegex.find(line.trim())
            if (dateMatch != null) {
                currentDate = dateMatch.groupValues[1]
                val remainder = dateMatch.groupValues[2]

                // Check if it's a single-line transaction
                val endMatch = endRegex.find(remainder)
                if (endMatch != null) {
                    val refNo = endMatch.groupValues[1]
                    val amtRaw = endMatch.groupValues[2]
                    val desc = remainder.substring(0, endMatch.range.first).trim()
                    
                    val tx = createTransaction(currentDate, desc, refNo, amtRaw, source, seq)
                    if (tx != null) {
                        list.add(tx)
                        seq += 1000L
                    }
                    currentDate = ""
                    currentDesc = ""
                } else {
                    currentDesc = remainder
                }
            } else {
                if (currentDate.isNotEmpty()) {
                    val trimmedLine = line.trim()
                    val endMatch = endRegex.find(trimmedLine)
                    if (endMatch != null) {
                        val refNo = endMatch.groupValues[1]
                        val amtRaw = endMatch.groupValues[2]
                        val descChunk = trimmedLine.substring(0, endMatch.range.first).trim()
                        currentDesc += if (descChunk.isNotEmpty()) " $descChunk" else ""
                        
                        val tx = createTransaction(currentDate, currentDesc, refNo, amtRaw, source, seq)
                        if (tx != null) {
                            list.add(tx)
                            seq += 1000L
                        }
                        currentDate = ""
                        currentDesc = ""
                    } else {
                        currentDesc += " " + trimmedLine
                    }
                }
            }
        }
        
        list.sortByDescending { it.rawTimestamp }
        return list
    }

    private fun createTransaction(
        dateStr: String,
        desc: String,
        refNo: String,
        amtRaw: String,
        source: String,
        seq: Long
    ): TransactionEntity? {
        val cleanAmt = amtRaw.replace("₹", "").replace(",", "").replace("Rs", "").trim()
        var amount = cleanAmt.toDoubleOrNull() ?: return null

        var type = "CREDIT"
        if (amount < 0 || amtRaw.startsWith("-")) {
            type = "DEBIT"
            amount = abs(amount)
        } else if (desc.contains("Debit", ignoreCase = true) && !desc.contains("Reversal", ignoreCase = true)) {
            type = "DEBIT"
        }

        // e.g. "11 Mar '26" -> "11 Mar, 2026"
        val dateFormatted = formatSliceDate(dateStr)

        val parsedTs = DateUtils.parseToMillis(dateFormatted, "12:00 PM") + seq
        
        var payee = desc
        if (payee.contains("UPI Debit-", ignoreCase = true)) {
            payee = payee.substringAfter("UPI Debit-").substringBefore("-paytm").substringBefore("@")
        } else if (payee.contains("UPI-Debit-", ignoreCase = true)) {
            payee = payee.substringAfter("UPI-Debit-").substringAfter("-").substringBefore("-YESB").substringBefore("-UTIB").substringBefore("-HDFC").substringBefore("-SBIN").substringBefore("-paytm").substringBefore("@")
        } else if (payee.contains("UPI Credit-", ignoreCase = true)) {
            payee = payee.substringAfter("UPI Credit-").substringBefore("-akan")
        } else if (payee.contains("UPI-Credit-", ignoreCase = true)) {
            payee = payee.substringAfter("UPI-Credit-").substringAfter("-").substringBefore("-akan")
        }
        
        if (payee.isBlank()) payee = desc

        val cat = CategoryClassifier.classify(payee, payee)

        return TransactionEntity(
            date = dateFormatted,
            time = "12:00 PM",
            rawTimestamp = parsedTs,
            title = payee,
            payee = payee,
            amount = amount,
            type = type,
            category = cat,
            upiTransactionId = refNo,
            paymentMethod = "Bank Transfer",
            statementSource = source,
            notes = desc
        )
    }

    private fun formatSliceDate(dateStr: String): String {
        // "11 Mar '26" -> "11 Mar, 2026"
        val parts = dateStr.split(" ")
        if (parts.size == 3) {
            val day = parts[0]
            val month = parts[1]
            val yearStr = parts[2].replace("'", "")
            return "$day $month, 20$yearStr"
        }
        return dateStr
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Bank_Statement.pdf"
    }

    sealed class ParseResult {
        data class Success(
            val transactions: List<TransactionEntity>,
            val pageCount: Int,
            val fileName: String,
            val sourceName: String
        ) : ParseResult()
        data class Error(val message: String) : ParseResult()
    }
}
