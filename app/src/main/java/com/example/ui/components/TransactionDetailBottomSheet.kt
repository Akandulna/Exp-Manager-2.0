package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionDetailDialog(
    transaction: TransactionEntity,
    availableTags: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onSaveTag: (tag: String, applyToAllForPayee: Boolean) -> Unit
) {
    val isSelf = transaction.isSelfTransfer
    val isDebit = transaction.type == "DEBIT"
    val (icon, iconColor) = getTagOrCategoryIconAndColor(transaction.tag.ifBlank { transaction.category }, isSelf)

    var currentTag by remember { mutableStateOf(transaction.tag) }
    var customTagInput by remember { mutableStateOf(transaction.tag) }
    var applyToAllForPayee by remember { mutableStateOf(true) }
    var localCustomTags by remember { mutableStateOf(emptySet<String>()) }

    val defaultBase = if (availableTags.isNotEmpty()) availableTags else listOf(
        "Coffee",
        "Food & Dining",
        "Groceries",
        "Transport & Fuel",
        "Bills & Utilities",
        "Self Transfer",
        "Transfers & Savings",
        "Shopping & Health",
        "Income & Salary"
    )

    val allDisplayTags = remember(defaultBase, localCustomTags, transaction.tag) {
        val combined = (defaultBase + localCustomTags + listOfNotNull(transaction.tag.takeIf { it.isNotBlank() }))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val seen = mutableSetOf<String>()
        val list = mutableListOf<String>()
        for (t in combined) {
            if (seen.add(t.lowercase())) {
                list.add(t)
            }
        }
        list
    }

    fun addNewTag(newTag: String) {
        val trimmed = newTag.trim()
        if (trimmed.isNotEmpty()) {
            localCustomTags = localCustomTags + trimmed
            currentTag = trimmed
            customTagInput = trimmed
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("transaction_detail_dialog"),
        containerColor = Color(0xFF1E293B),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transaction.category,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = if (transaction.tag.isNotBlank()) transaction.tag else if (transaction.payee.isNotBlank()) transaction.payee else transaction.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = if (isSelf) "Self Transfer (Excluded from Net Totals)" else if (isDebit) "Sent to: ${transaction.payee}" else "Received from: ${transaction.payee}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelf) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Amount Banner
                val bannerBg = when {
                    isSelf -> Color(0xFF0284C7).copy(alpha = 0.18f)
                    isDebit -> ExpenseRed.copy(alpha = 0.15f)
                    else -> IncomeGreen.copy(alpha = 0.15f)
                }
                val bannerColor = when {
                    isSelf -> Color(0xFF38BDF8)
                    isDebit -> ExpenseRed
                    else -> IncomeGreen
                }
                val bannerLabel = when {
                    isSelf -> "Self Transfer (Not Counted in Spent/Received)"
                    isDebit -> "Expense (Debit)"
                    else -> "Income (Credit)"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = bannerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = bannerColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (isSelf) "₹" else if (isDebit) "-₹" else "+₹"}${String.format(Locale.US, "%,.2f", transaction.amount)}",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = bannerColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tagging System Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tag,
                                contentDescription = "Tag",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tag this Transaction & Recipient",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Available tag chips (Preset + Custom Created)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allDisplayTags.forEach { tag ->
                                val isSelected = currentTag.equals(tag, ignoreCase = true)
                                val tagColor = when {
                                    tag.contains("Transfer", ignoreCase = true) || tag.contains("Saving", ignoreCase = true) -> Color(0xFF38BDF8)
                                    tag.equals("Coffee", ignoreCase = true) -> Color(0xFFF59E0B)
                                    tag.contains("Shopping", ignoreCase = true) -> Color(0xFFEC4899)
                                    tag.contains("Food", ignoreCase = true) -> Color(0xFFF97316)
                                    tag.contains("Bills", ignoreCase = true) -> Color(0xFFA855F7)
                                    else -> Color(0xFFEF4444)
                                }
                                
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) tagColor else Color(0xFF1E293B),
                                    border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                                    modifier = Modifier.clickable {
                                        currentTag = if (isSelected) "" else tag
                                        customTagInput = currentTag
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) Color.White else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Tag Input with Instant Add
                        OutlinedTextField(
                            value = customTagInput,
                            onValueChange = {
                                customTagInput = it
                                currentTag = it
                            },
                            label = { Text("Custom Tag (e.g. Coffee, Kaushal, Rent)", fontSize = 12.sp) },
                            placeholder = { Text("Type new tag name...", fontSize = 12.sp, color = Color(0xFF64748B)) },
                            singleLine = true,
                            trailingIcon = {
                                if (customTagInput.trim().isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            addNewTag(customTagInput)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Tag to List",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFEF4444),
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedLabelColor = Color(0xFFEF4444),
                                unfocusedLabelColor = Color(0xFF94A3B8),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (transaction.payee.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { applyToAllForPayee = !applyToAllForPayee }
                            ) {
                                Checkbox(
                                    checked = applyToAllForPayee,
                                    onCheckedChange = { applyToAllForPayee = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFFEF4444),
                                        checkmarkColor = Color.White
                                    )
                                )
                                Text(
                                    text = "Apply tag to all transactions with \"${transaction.payee}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val finalTag = currentTag.trim()
                                if (finalTag.isNotEmpty()) {
                                    addNewTag(finalTag)
                                }
                                onSaveTag(finalTag, applyToAllForPayee)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentTag.isBlank()) "Clear Tag" else "Save Tag: $currentTag",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DetailRow("Date & Time", "${transaction.date} • ${transaction.time}")
                DetailRow("Payee / Recipient", transaction.payee)
                if (transaction.upiTransactionId.isNotBlank()) {
                    DetailRow("UPI Ref ID", transaction.upiTransactionId)
                }
                if (transaction.paymentMethod.isNotBlank()) {
                    DetailRow("Payment Method", transaction.paymentMethod)
                }
                DetailRow("Statement Source", transaction.statementSource)
                if (transaction.notes.isNotBlank()) {
                    DetailRow("Notes", transaction.notes)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { onDelete(transaction) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(ExpenseRed.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier.testTag("delete_transaction_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }

                Button(
                    onClick = { onEdit(transaction) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier.testTag("edit_transaction_button")
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit All Fields", color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        Spacer(modifier = Modifier.height(4.dp))
    }
}

