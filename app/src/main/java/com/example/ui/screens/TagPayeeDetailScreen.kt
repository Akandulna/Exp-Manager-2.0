package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.getTagOrCategoryIconAndColor
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPayeeDetailScreen(
    groupTitle: String,
    isTag: Boolean,
    viewModel: ExpenseViewModel,
    onBack: () -> Unit,
    onSelectTransaction: (TransactionEntity) -> Unit,
    onAddTransactionForGroup: (title: String, isTag: Boolean) -> Unit
) {
    BackHandler { onBack() }

    val allTransactions by viewModel.filteredTransactions.collectAsState()
    
    // Filter transactions specifically for this group (tag or payee)
    val groupTransactions = remember(allTransactions, groupTitle, isTag) {
        allTransactions.filter { tx ->
            if (isTag) {
                val txTag = if (tx.tag.isNotBlank()) tx.tag else tx.category
                txTag.equals(groupTitle, ignoreCase = true)
            } else {
                val txPayee = if (tx.payee.isNotBlank()) tx.payee else tx.title
                txPayee.equals(groupTitle, ignoreCase = true)
            }
        }
    }

    var localSearchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, DEBIT, CREDIT, TRANSFER

    // Search and sub-filter
    val displayedTransactions by remember(groupTransactions, localSearchQuery, filterType) {
        derivedStateOf {
            var list = groupTransactions

            if (localSearchQuery.isNotBlank()) {
                val q = localSearchQuery.trim().lowercase()
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                    it.payee.lowercase().contains(q) ||
                    it.tag.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.upiTransactionId.lowercase().contains(q) ||
                    it.notes.lowercase().contains(q) ||
                    it.paymentMethod.lowercase().contains(q)
                }
            }

            if (filterType == "DEBIT") {
                list = list.filter { it.type == "DEBIT" && !it.isTransferOrSaving }
            } else if (filterType == "CREDIT") {
                list = list.filter { it.type == "CREDIT" && !it.isTransferOrSaving }
            } else if (filterType == "TRANSFER") {
                list = list.filter { it.isTransferOrSaving }
            }

            list.sortedByDescending { it.rawTimestamp }
        }
    }

    // Compute stats for this group
    val totalDebit = remember(groupTransactions) {
        groupTransactions.filter { it.type == "DEBIT" && !it.isTransferOrSaving }.sumOf { it.amount }
    }
    val totalCredit = remember(groupTransactions) {
        groupTransactions.filter { it.type == "CREDIT" && !it.isTransferOrSaving }.sumOf { it.amount }
    }
    val totalTransfers = remember(groupTransactions) {
        groupTransactions.filter { it.isTransferOrSaving }.sumOf { it.amount }
    }
    val netAmount = remember(totalCredit, totalDebit, totalTransfers, groupTransactions) {
        val isAllTransfers = groupTransactions.isNotEmpty() && groupTransactions.all { it.isTransferOrSaving }
        if (isAllTransfers) totalTransfers else (totalCredit - totalDebit)
    }

    val (icon, iconColor) = getTagOrCategoryIconAndColor(groupTitle)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = groupTitle,
                                tint = iconColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = groupTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = if (isTag) "Tag Breakdown · ${groupTransactions.size} transactions" else "Payee Breakdown · ${groupTransactions.size} transactions",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_group_detail")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Overview",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddTransactionForGroup(groupTitle, isTag) },
                containerColor = Color(0xFFEF4444),
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_group_tx")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = Color(0xFF0F172A)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Group Financial Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTag) "TAG NET BALANCE" else "PAYEE NET BALANCE",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(iconColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isTag) "🏷️ Tag" else "👤 Payee",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = iconColor,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val netPrefix = if (netAmount < 0) "-₹" else "+₹"
                        val netDisplayColor = if (netAmount < 0) ExpenseRed else IncomeGreen
                        Text(
                            text = "$netPrefix${String.format(Locale.US, "%,.2f", Math.abs(netAmount))}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = netDisplayColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3-Metric Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Total Spent
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Spent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", totalDebit)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = ExpenseRed,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Total Received
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Received",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${String.format(Locale.US, "%,.2f", totalCredit)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = IncomeGreen,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Total Transfers if any
                            if (totalTransfers > 0) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapHoriz,
                                                    contentDescription = null,
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Transfers",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "₹${String.format(Locale.US, "%,.2f", totalTransfers)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF38BDF8),
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }
            }

            // Search in group transactions
            item {
                OutlinedTextField(
                    value = localSearchQuery,
                    onValueChange = { localSearchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("group_search_field"),
                    placeholder = { Text("Search transactions in $groupTitle...", color = Color(0xFF64748B)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFF94A3B8)
                        )
                    },
                    trailingIcon = {
                        if (localSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { localSearchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Type Filter Chips (All / Spent / Received / Transfer)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All (${groupTransactions.size})",
                        "DEBIT" to "Spent",
                        "CREDIT" to "Received"
                    ) + if (totalTransfers > 0) listOf("TRANSFER" to "Transfers") else emptyList()

                    filters.forEach { (typeKey, label) ->
                        val isSelected = filterType == typeKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterType = typeKey },
                            label = {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626),
                                containerColor = Color(0xFF1E293B)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFF334155),
                                selectedBorderColor = Color(0xFFEF4444)
                            )
                        )
                    }
                }
            }

            // Transactions Header & Count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${displayedTransactions.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Tap to view & edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // List of Transactions
            if (displayedTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (localSearchQuery.isNotEmpty()) "No matching transactions found" else "No transactions for $groupTitle",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try adjusting your search or filter",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = displayedTransactions,
                    key = { it.id }
                ) { transaction ->
                    TransactionItemCard(
                        transaction = transaction,
                        onClick = { onSelectTransaction(transaction) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
