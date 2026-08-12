package com.example.util

import com.example.data.TransactionEntity
import kotlin.math.abs

object TransactionDeduplicator {

    /**
     * Determines whether two transaction entries represent the exact same transaction.
     */
    fun isDuplicate(tx1: TransactionEntity, tx2: TransactionEntity): Boolean {
        // 1. Check if both have non-blank UPI Transaction Reference IDs
        val upi1 = tx1.upiTransactionId.trim()
        val upi2 = tx2.upiTransactionId.trim()
        if (upi1.isNotBlank() && upi2.isNotBlank() && upi1.equals(upi2, ignoreCase = true)) {
            return true
        }

        // 2. Check core transaction fields: date, amount, type, payee/title
        val dateMatch = tx1.date.trim().equals(tx2.date.trim(), ignoreCase = true)
        val amountMatch = abs(tx1.amount - tx2.amount) < 0.001
        val typeMatch = tx1.type.trim().equals(tx2.type.trim(), ignoreCase = true)

        val payee1 = tx1.payee.ifBlank { tx1.title }.trim()
        val payee2 = tx2.payee.ifBlank { tx2.title }.trim()
        val payeeMatch = payee1.equals(payee2, ignoreCase = true) ||
                         tx1.title.trim().equals(tx2.title.trim(), ignoreCase = true)

        if (dateMatch && amountMatch && typeMatch && payeeMatch) {
            // If both have explicit non-default times, verify time matching
            val time1 = tx1.time.trim()
            val time2 = tx2.time.trim()
            if (time1.isNotBlank() && time2.isNotBlank() &&
                time1 != "12:00 PM" && time2 != "12:00 PM"
            ) {
                return time1.equals(time2, ignoreCase = true)
            }
            return true
        }

        return false
    }

    /**
     * Filters out duplicates from an incoming transaction list against existing database records
     * as well as within the incoming list itself.
     * Returns Pair(uniqueNewTransactions, duplicateCountSkipped)
     */
    fun filterDuplicates(
        incoming: List<TransactionEntity>,
        existing: List<TransactionEntity>
    ): Pair<List<TransactionEntity>, Int> {
        val uniqueList = mutableListOf<TransactionEntity>()
        var duplicateCount = 0

        for (item in incoming) {
            val existsInDb = existing.any { isDuplicate(item, it) }
            val existsInUnique = uniqueList.any { isDuplicate(item, it) }

            if (existsInDb || existsInUnique) {
                duplicateCount++
            } else {
                uniqueList.add(item)
            }
        }

        return Pair(uniqueList, duplicateCount)
    }
}
