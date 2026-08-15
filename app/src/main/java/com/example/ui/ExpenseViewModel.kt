package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TransactionEntity
import com.example.data.TransactionRepository
import com.example.util.DateUtils
import com.example.util.PdfTransactionParser
import com.example.util.TransactionDeduplicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption {
    DATE_DESC,
    DATE_ASC,
    AMOUNT_DESC,
    AMOUNT_ASC
}

sealed class ImportUiState {
    object Idle : ImportUiState()
    object Loading : ImportUiState()
    data class ParsedPreview(
        val transactions: List<TransactionEntity>,
        val fileName: String,
        val pageCount: Int,
        val sourceName: String,
        val duplicateCount: Int = 0,
        val newCount: Int = transactions.size
    ) : ImportUiState()
    data class Success(
        val count: Int,
        val skippedDuplicates: Int = 0,
        val sourceName: String
    ) : ImportUiState()
    data class Error(val message: String) : ImportUiState()
}

data class ExpenseSummary(
    val totalSpent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val totalTransfersAndSavings: Double = 0.0,
    val netBalance: Double = 0.0,
    val transactionCount: Int = 0,
    val categoryTotals: Map<String, Double> = emptyMap()
)

class ExpenseViewModel(private val repository: TransactionRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    private val _selectedType = MutableStateFlow("ALL") // "ALL", "DEBIT", "CREDIT"
    val selectedType = _selectedType.asStateFlow()

    private val _selectedSort = MutableStateFlow(SortOption.DATE_DESC)
    val selectedSort = _selectedSort.asStateFlow()

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState = _importState.asStateFlow()

    private val _rawTransactions = repository.allTransactions

    // Filtered & Sorted Transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        _rawTransactions,
        _searchQuery,
        _selectedCategory,
        _selectedType,
        _selectedSort
    ) { transactions, query, category, type, sort ->
        var list = transactions

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                        it.payee.lowercase().contains(q) ||
                        it.tag.lowercase().contains(q) ||
                        it.category.lowercase().contains(q) ||
                        it.upiTransactionId.lowercase().contains(q) ||
                        it.paymentMethod.lowercase().contains(q)
            }
        }

        if (category != "All") {
            list = list.filter {
                it.category.equals(category, ignoreCase = true) ||
                        it.tag.equals(category, ignoreCase = true)
            }
        }

        if (type == "DEBIT") {
            // Show only expenses, excluding transfers and savings
            list = list.filter { it.type == "DEBIT" && !it.isTransferOrSaving }
        } else if (type == "CREDIT") {
            // Show only income, excluding transfers and savings
            list = list.filter { it.type == "CREDIT" && !it.isTransferOrSaving }
        } else if (type == "TRANSFER") {
            // Show only transfers and savings
            list = list.filter { it.isTransferOrSaving }
        }

        when (sort) {
            SortOption.DATE_DESC -> list.sortedByDescending { it.rawTimestamp }
            SortOption.DATE_ASC -> list.sortedBy { it.rawTimestamp }
            SortOption.AMOUNT_DESC -> list.sortedByDescending { it.amount }
            SortOption.AMOUNT_ASC -> list.sortedBy { it.amount }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Expense Summary Stats (Transfer & Savings is separated and excluded from total spent & total received)
    val summaryStats: StateFlow<ExpenseSummary> = _rawTransactions
        .map { transactions ->
            var spent = 0.0
            var received = 0.0
            var transfersAndSavings = 0.0
            val catMap = mutableMapOf<String, Double>()

            transactions.forEach { tx ->
                if (tx.isTransferOrSaving) {
                    transfersAndSavings += tx.amount
                } else {
                    if (tx.type == "DEBIT") {
                        spent += tx.amount
                        val displayCat = if (tx.tag.isNotBlank()) tx.tag else tx.category
                        catMap[displayCat] = (catMap[displayCat] ?: 0.0) + tx.amount
                    } else {
                        received += tx.amount
                    }
                }
            }

            ExpenseSummary(
                totalSpent = spent,
                totalReceived = received,
                totalTransfersAndSavings = transfersAndSavings,
                netBalance = received - spent,
                transactionCount = transactions.count { !it.isTransferOrSaving },
                categoryTotals = catMap
            )
        }.flowOn(Dispatchers.Default).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseSummary()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setType(type: String) {
        _selectedType.value = type
    }

    fun toggleTypeFilter(type: String) {
        if (_selectedType.value == type) {
            _selectedType.value = "ALL"
        } else {
            _selectedType.value = type
        }
    }

    fun setSort(sort: SortOption) {
        _selectedSort.value = sort
    }

    fun tagRecipient(payee: String, tag: String) {
        viewModelScope.launch {
            if (payee.isNotBlank()) {
                repository.updateTagForPayee(payee, tag)
            }
        }
    }

    fun tagTransaction(transactionId: Long, tag: String) {
        viewModelScope.launch {
            repository.updateTagForTransaction(transactionId, tag)
        }
    }

    fun tagTransaction(transactionId: Long, tag: String, applyToAllForPayee: Boolean = false, payee: String = "") {
        viewModelScope.launch {
            if (applyToAllForPayee && payee.isNotBlank()) {
                repository.updateTagForPayee(payee, tag)
            } else {
                repository.updateTagForTransaction(transactionId, tag)
            }
        }
    }

    fun parsePdfFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _importState.value = ImportUiState.Loading
            when (val result = PdfTransactionParser.parsePdfUri(context, uri)) {
                is PdfTransactionParser.ParseResult.Success -> {
                    val existing = _rawTransactions.first()
                    val (uniqueList, duplicateCount) = TransactionDeduplicator.filterDuplicates(result.transactions, existing)
                    _importState.value = ImportUiState.ParsedPreview(
                        transactions = result.transactions,
                        fileName = result.fileName,
                        pageCount = result.pageCount,
                        sourceName = result.sourceName,
                        duplicateCount = duplicateCount,
                        newCount = uniqueList.size
                    )
                }
                is PdfTransactionParser.ParseResult.Error -> {
                    _importState.value = ImportUiState.Error(result.message)
                }
            }
        }
    }


    fun confirmImport(transactions: List<TransactionEntity>, sourceName: String) {
        viewModelScope.launch {
            _importState.value = ImportUiState.Loading
            val existing = _rawTransactions.first()
            val (uniqueTransactions, skippedDuplicates) = TransactionDeduplicator.filterDuplicates(transactions, existing)

            if (uniqueTransactions.isNotEmpty()) {
                repository.insertTransactions(uniqueTransactions)
            }

            _importState.value = ImportUiState.Success(
                count = uniqueTransactions.size,
                skippedDuplicates = skippedDuplicates,
                sourceName = sourceName
            )
        }
    }

    fun addManualTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    fun resetImportState() {
        _importState.value = ImportUiState.Idle
    }
}

class ExpenseViewModelFactory(private val repository: TransactionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
