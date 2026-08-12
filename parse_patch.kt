import java.io.File

fun main() {
    val file = File("app/src/main/java/com/example/util/PdfTransactionParser.kt")
    var content = file.readText()
    
    val oldCode = """
        var type = "DEBIT"
        if (amount < 0 || amtRaw.startsWith("-")) {
            type = "DEBIT"
            amount = abs(amount)
        } else if (desc.contains("Interest Cr.", ignoreCase = true) ||
            desc.contains("UPI Credit", ignoreCase = true) ||
            desc.contains("Deposit", ignoreCase = true) ||
            amtRaw.startsWith("+")
        ) {
            type = "CREDIT"
            amount = abs(amount)
        } else if (desc.contains("Debit", ignoreCase = true)) {
            type = "DEBIT"
        }
    """.trimIndent()
    
    val newCode = """
        var type = "CREDIT"
        if (amount < 0 || amtRaw.startsWith("-")) {
            type = "DEBIT"
            amount = abs(amount)
        } else if (desc.contains("Debit", ignoreCase = true) && !desc.contains("Reversal", ignoreCase = true)) {
            // Fallback just in case a debit doesn't have a minus sign
            type = "DEBIT"
        }
    """.trimIndent()
    
    content = content.replace(oldCode, newCode)
    file.writeText(content)
}
