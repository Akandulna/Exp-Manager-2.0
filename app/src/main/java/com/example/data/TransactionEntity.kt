package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,            // e.g. "01 Jul, 2026"
    val time: String,            // e.g. "10:15 AM"
    val rawTimestamp: Long,      // timestamp for sorting
    val title: String,           // e.g. "Paid to KAUSHAL KUMAR"
    val payee: String,           // e.g. "KAUSHAL KUMAR"
    val amount: Double,          // e.g. 10.0
    val type: String,            // "DEBIT" or "CREDIT"
    val category: String,        // "Food & Dining", "Shopping", "Transport", "Bills", "Self Transfer", "Income", "Others"
    val upiTransactionId: String = "",
    val paymentMethod: String = "",
    val statementSource: String = "Manual Entry", // e.g. "Google Pay Statement", "Slice Statement"
    val notes: String = ""
)
