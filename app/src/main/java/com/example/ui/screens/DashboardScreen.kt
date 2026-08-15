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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.SystemUpdate
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
    onCheckUpdate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val summary by viewModel.summaryStats.collectAsState()
    val transactions by viewModel.filteredTransactions.collectAsState()
    val selectedType by viewModel.selectedType.collectAsState()
    
    val tagGroups = androidx.compose.runtime.remember(transactions, selectedType) {
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

    val payeeGroups = androidx.compose.runtime.remember(transactions, selectedType) {
        transactions
            .groupBy {
                if (it.payee.isNotBlank()) it.payee else if (it.title.isNotBlank()) it.title else "Unknown"
            }
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
                Column(modifier = Modifier.weight(1f)) {
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

                // Update App Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF1E293B))
                        .clickable { onCheckUpdate() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("btn_check_update")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update App",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Update",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
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

        // Overview Breakdown Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedType) {
                            "DEBIT" -> "Expenses Breakdown"
                            "CREDIT" -> "Income Breakdown"
                            "TRANSFER" -> "Transfer & Savings"
                            else -> "View by Tags & Payees"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Side-by-side breakdown by tags and payees",
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

        // Empty state or 2-column breakdown list
        if (tagGroups.isEmpty() && payeeGroups.isEmpty()) {
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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Column 1: By Tags
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Column 1 Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🏷️ Tags",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF38BDF8)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${tagGroups.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        if (tagGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No tags",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            tagGroups.forEach { (tagName, data) ->
                                val (netAmount, txs) = data
                                BreakdownColumnCard(
                                    title = tagName,
                                    isTag = true,
                                    netAmount = netAmount,
                                    transactions = txs,
                                    onSelectTransaction = onSelectTransaction
                                )
                            }
                        }
                    }

                    // Column 2: By Payees
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Column 2 Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E293B))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👤 Payees",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF10B981)
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF059669).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${payeeGroups.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        if (payeeGroups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No payees",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        } else {
                            payeeGroups.forEach { (payeeName, data) ->
                                val (netAmount, txs) = data
                                BreakdownColumnCard(
                                    title = payeeName,
                                    isTag = false,
                                    netAmount = netAmount,
                                    transactions = txs,
                                    onSelectTransaction = onSelectTransaction
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun BreakdownColumnCard(
    title: String,
    isTag: Boolean,
    netAmount: Double,
    transactions: List<com.example.data.TransactionEntity>,
    onSelectTransaction: (com.example.data.TransactionEntity) -> Unit
) {
    val expanded = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val isTransferGroup = transactions.all { it.isTransferOrSaving }
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
    val (icon, iconBg) = getTagOrCategoryIconAndColor(title)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded.value = !expanded.value },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconBg.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconBg,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount
            Text(
                text = "$amountPrefix${String.format(Locale.US, "%,.2f", displayAmount)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Transaction count & toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transactions.size} txn${if (transactions.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Text(
                    text = if (expanded.value) "▲ Hide" else "▼ Details",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expanded.value) Color(0xFF38BDF8) else Color(0xFF64748B),
                    fontSize = 10.sp
                )
            }

            // Expanded Transaction Items
            androidx.compose.animation.AnimatedVisibility(visible = expanded.value) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    transactions.forEachIndexed { index, tx ->
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

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onSelectTransaction(tx) }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = if (isTag) (if (tx.payee.isNotBlank()) tx.payee else tx.title) else (if (tx.tag.isNotBlank()) "🏷️ ${tx.tag}" else tx.category),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.date,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "$txPrefix${String.format(Locale.US, "%,.2f", tx.amount)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = txColor,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        if (index < transactions.size - 1) {
                            androidx.compose.material3.HorizontalDivider(
                                color = Color(0xFF334155).copy(alpha = 0.4f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

