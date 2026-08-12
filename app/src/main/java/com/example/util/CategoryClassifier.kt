package com.example.util

object CategoryClassifier {
    fun classify(title: String, payee: String): String {
        val text = "$title $payee".lowercase()
        return when {
            text.contains("blinkit") || text.contains("zepto") || text.contains("marketplace") || text.contains("mart") || text.contains("grocery") -> "Groceries"
            text.contains("tea") || text.contains("coffee") || text.contains("hotel") || text.contains("food") || text.contains("dominos") || text.contains("pizza") || text.contains("restaurant") || text.contains("eatclub") || text.contains("snacks") || text.contains("tadka") || text.contains("treats") -> "Food & Dining"
            text.contains("petrol") || text.contains("fuel") || text.contains("rapido") || text.contains("roppen") || text.contains("uber") || text.contains("ola") || text.contains("travel") || text.contains("automobiles") || text.contains("gear") -> "Transport & Fuel"
            text.contains("airtel") || text.contains("recharge") || text.contains("bill") || text.contains("electricity") || text.contains("hardware") || text.contains("electric") -> "Bills & Utilities"
            text.contains("saree") || text.contains("store") || text.contains("chemist") || text.contains("health") || text.contains("wine") || text.contains("google play") -> "Shopping & Health"
            text.contains("cred") || text.contains("payu") || text.contains("lazy pay") || text.contains("repayment") || text.contains("loan") -> "Finance & Bills"
            text.contains("self transfer") || text.contains("auto save") || text.contains("deposit") -> "Transfers & Savings"
            text.contains("received from") || text.contains("rewards") || text.contains("interest cr") || text.contains("credit") -> "Income & Cashbacks"
            else -> "Personal & Others"
        }
    }
}
