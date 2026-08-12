import java.util.regex.Pattern

fun main() {
    val text = """
    Transaction statement period
01 February 2026 - 31 July 2026
Sent
₹3,34,853.58
Received
₹3,91,546
Date & time Transaction details Amount
01 Feb, 2026
12:26 PM
Received from ABHILASH BHOI
UPI Transaction ID: 385627951460
Paid to Federal Bank 8110
₹1,000
04 Feb, 2026
02:36 PM
Received from GONE PRASHANTH SATYANARAYANA
UPI Transaction ID: 418088258115
Paid to Slice Small Finance Bank 8941
₹83
    """.trimIndent()
    
    val dateRegex = Regex("^\\d{2}\\s+[A-Za-z]{3},\\s+\\d{4}$")
    val timeRegex = Regex("^\\d{2}:\\d{2}\\s+[AMPM]{2}$")
    val amountRegex = Regex("^[+-]?(?:₹|Rs\\.?|INR)?\\s*([\\d,]+\\.?\\d*)$")

    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        if (dateRegex.matches(line)) {
            val dateStr = line
            if (i + 1 < lines.size && timeRegex.matches(lines[i + 1])) {
                val timeStr = lines[i + 1]
                var j = i + 2
                val txLines = mutableListOf<String>()
                
                while (j < lines.size) {
                    val nextLine = lines[j]
                    if (dateRegex.matches(nextLine)) break
                    if (nextLine.startsWith("Page ") && nextLine.contains("of")) break
                    if (nextLine.startsWith("Note: This statement")) break
                    if (nextLine.startsWith("Powered by")) break
                    
                    txLines.add(nextLine)
                    j++
                    
                    if (amountRegex.matches(nextLine)) {
                        break
                    }
                }
                
                if (txLines.isNotEmpty()) {
                    var amountStr = ""
                    val lastLine = txLines.last()
                    
                    if (amountRegex.matches(lastLine)) {
                        amountStr = lastLine
                        txLines.removeAt(txLines.size - 1)
                    } else {
                        val possibleAmount = amountRegex.find(lastLine)
                        if (possibleAmount != null) {
                            amountStr = possibleAmount.value
                            txLines[txLines.size - 1] = lastLine.replace(amountStr, "").trim()
                        }
                    }
                    
                    val upiIndex = txLines.indexOfFirst { it.startsWith("UPI Transaction ID:", ignoreCase = true) }
                    var title = ""
                    var upiId = ""
                    var bankDetails = ""
                    
                    if (upiIndex != -1) {
                        title = txLines.subList(0, upiIndex).joinToString(" ").trim()
                        upiId = txLines[upiIndex].substringAfter(":").trim()
                        if (upiIndex + 1 < txLines.size) {
                            bankDetails = txLines[upiIndex + 1]
                        }
                    } else {
                        title = txLines.joinToString(" ").trim()
                    }
                    
                    if (amountStr.isNotEmpty()) {
                        val cleanAmt = amountStr.replace("₹", "").replace(",", "").replace("Rs", "").trim()
                        val amount = cleanAmt.toDoubleOrNull() ?: 0.0
                        
                        println("Tx: Date=${'$'}dateStr, Time=${'$'}timeStr, Title=${'$'}title, UPI=${'$'}upiId, Bank=${'$'}bankDetails, Amount=${'$'}amount")
                    }
                }
                i = j
            } else {
                i++
            }
        } else {
            i++
        }
    }
}
main()
