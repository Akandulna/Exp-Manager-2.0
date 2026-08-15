package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TransactionEntity
import com.example.ui.components.AddEditTransactionDialog
import com.example.ui.components.TransactionDetailDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PdfImportScreen
import com.example.ui.screens.TransactionsListScreen

@Composable
fun ExpenseTrackerApp(viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedDetailTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val allTags by viewModel.allTags.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard", modifier = Modifier.size(20.dp)) },
                    label = { Text("Overview") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(imageVector = Icons.Default.List, contentDescription = "Transactions", modifier = Modifier.size(20.dp)) },
                    label = { Text("Transactions") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_transactions")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "PDF Reader", modifier = Modifier.size(20.dp)) },
                    label = { Text("Read PDF") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = Color(0xFF10B981),
                        indicatorColor = Color(0xFF10B981),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_tab_pdf_reader")
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        editingTransaction = null
                        showAddDialog = true
                    },
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTransactions = { selectedTab = 1 },
                    onNavigateToPdfImport = { selectedTab = 2 },
                    onAddTransaction = {
                        editingTransaction = null
                        showAddDialog = true
                    },
                    onSelectTransaction = { item ->
                        selectedDetailTransaction = item
                    }
                )
                1 -> TransactionsListScreen(
                    viewModel = viewModel,
                    onSelectTransaction = { item ->
                        selectedDetailTransaction = item
                    }
                )
                2 -> PdfImportScreen(viewModel = viewModel)
            }
        }

        // Add or Edit Transaction Dialog
        if (showAddDialog || editingTransaction != null) {
            AddEditTransactionDialog(
                transaction = editingTransaction,
                availableTags = allTags,
                onDismiss = {
                    showAddDialog = false
                    editingTransaction = null
                },
                onSave = { item ->
                    if (item.tag.isNotBlank()) {
                        viewModel.addCustomTag(item.tag)
                    }
                    if (editingTransaction != null) {
                        viewModel.updateTransaction(item)
                    } else {
                        viewModel.addManualTransaction(item)
                    }
                    showAddDialog = false
                    editingTransaction = null
                }
            )
        }

        // Transaction Detail Modal Dialog
        selectedDetailTransaction?.let { transaction ->
            TransactionDetailDialog(
                transaction = transaction,
                availableTags = allTags,
                onDismiss = { selectedDetailTransaction = null },
                onEdit = { item ->
                    selectedDetailTransaction = null
                    editingTransaction = item
                },
                onDelete = { item ->
                    viewModel.deleteTransaction(item)
                    selectedDetailTransaction = null
                },
                onSaveTag = { tag, applyToAll ->
                    if (tag.isNotBlank()) {
                        viewModel.addCustomTag(tag)
                    }
                    if (applyToAll && transaction.payee.isNotBlank()) {
                        viewModel.tagRecipient(transaction.payee, tag)
                    } else {
                        viewModel.tagTransaction(transaction.id, tag)
                    }
                    selectedDetailTransaction = null
                }
            )
        }
    }
}
