package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.ExpenseViewModel
import com.example.ui.components.SummaryHeaderCard
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.getTagOrCategoryIconAndColor
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToPdfImport: () -> Unit,
    onAddTransaction: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summaryStats.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    
    val groupedTransactions = androidx.compose.runtime.remember(transactions, selectedType) {
        transactions
            .groupBy { if (it.tag.isNotBlank()) it.tag else it.category }
            .mapValues { entry ->
                val txs = entry.value
                val isAllTransfers = txs.all { it.isTransferOrSaving }
                if (isAllTransfers) {
                    val total = txs.sumOf { it.amount }
                    Pair(total, txs)
                } else {
                    val totalDebit = txs.filter { it.type == "DEBIT" && !it.isTransferOrSaving }.sumOf { it.amount }
                    val totalCredit = txs.filter { it.type == "CREDIT" && !it.isTransferOrSaving }.sumOf { it.amount }
                    Pair(totalCredit - totalDebit, txs)
                }
            }
            .toList()
            .sortedByDescending { Math.abs(it.second.first) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Expense Overview",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Clean recipient tracker & statement analyzer",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Summary Header Card with Clickable Filter
        item {
            SummaryHeaderCard(
                summary = summary,
                selectedType = selectedType,
                onTypeFilterClick = { type ->
                    viewModel.toggleTypeFilter(type)
                }
            )
        }

        // Active Filter Banner (if filter is selected via Total Spent / Total Received / Transfer & Savings)
        if (selectedType != "ALL") {
            item {
                val bannerColor = when (selectedType) {
                    "DEBIT" -> ExpenseRed
                    "CREDIT" -> IncomeGreen
                    else -> Color(0xFF38BDF8)
                }
                val bannerText = when (selectedType) {
                    "DEBIT" -> "Showing: Only Expenses (Total Spent)"
                    "CREDIT" -> "Showing: Only Income (Total Received)"
                    else -> "Showing: Transfer & Savings"
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = bannerColor.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = "Filtered",
                                tint = bannerColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = bannerText,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { viewModel.setType("ALL") }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Show All",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Filter",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Overview Tagged Transactions Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedType) {
                            "DEBIT" -> "Expenses by Tag"
                            "CREDIT" -> "Income by Tag"
                            "TRANSFER" -> "Transfer & Savings by Tag"
                            else -> "View by Tags & Payees"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Tap any tag to view transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
                TextButton(onClick = onNavigateToTransactions) {
                    Text(
                        text = "All Transactions",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Empty state or transaction list
        if (groupedTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = "No transactions",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedType != "ALL") "No matching transactions found" else "No Transactions Yet",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (selectedType != "ALL") "Try clearing your spend/receive filter." else "Import a statement PDF or add an expense manually to see your recipient list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else {
            items(groupedTransactions, key = { it.first }) { (tagName, data) ->
                val (netAmount, txs) = data
                TagGroupCard(
                    tagName = tagName,
                    netAmount = netAmount,
                    transactions = txs,
                    onSelectTransaction = onSelectTransaction
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TagGroupCard(
    tagName: String,
    netAmount: Double,
    transactions: List<com.example.data.TransactionEntity>,
    onSelectTransaction: (com.example.data.TransactionEntity) -> Unit
) {
    val expanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val isTransferGroup = transactions.all { it.isTransferOrSaving }
    val isIncome = !isTransferGroup && netAmount >= 0 && transactions.any { it.type == "CREDIT" }
    val isDebit = !isTransferGroup && (netAmount < 0 || transactions.all { it.type == "DEBIT" })
    
    val amountColor = when {
        isTransferGroup -> Color(0xFF38BDF8)
        isDebit -> com.example.ui.theme.ExpenseRed
        else -> com.example.ui.theme.IncomeGreen
    }
    val amountPrefix = when {
        isTransferGroup -> "₹"
        isDebit -> "-₹"
        else -> "+₹"
    }
    val displayAmount = Math.abs(netAmount)
    
    val (icon, iconBg) = getTagOrCategoryIconAndColor(tagName)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded.value = !expanded.value },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = tagName,
                        tint = iconBg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tagName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "${transactions.size} transaction${if (transactions.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Amount & Chevron
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix${String.format(java.util.Locale.US, "%,.2f", displayAmount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (expanded.value) "Hide" else "Show",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Expanded List
            androidx.compose.animation.AnimatedVisibility(visible = expanded.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    transactions.forEachIndexed { index, tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectTransaction(tx) }
                                .padding(vertical = 8.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (tx.payee.isNotBlank()) tx.payee else tx.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tx.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                            
                            val txIsTransfer = tx.isTransferOrSaving
                            val txIsDebit = tx.type == "DEBIT"
                            val txColor = when {
                                txIsTransfer -> Color(0xFF38BDF8)
                                txIsDebit -> ExpenseRed
                                else -> IncomeGreen
                            }
                            val txPrefix = when {
                                txIsTransfer -> "₹"
                                txIsDebit -> "-₹"
                                else -> "+₹"
                            }
                            Text(
                                text = "$txPrefix${String.format(java.util.Locale.US, "%,.2f", tx.amount)}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = txColor
                            )
                        }
                        if (index < transactions.size - 1) {
                            androidx.compose.material3.HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

