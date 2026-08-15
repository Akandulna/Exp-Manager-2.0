package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.util.Locale

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    onClick: () -> Unit,
    showTagView: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!showTagView) {
        // Standard view as it was originally for Transactions screen
        val isSelf = transaction.isSelfTransfer
        val isDebit = transaction.type == "DEBIT"
        val amountColor = when {
            isSelf -> Color(0xFF38BDF8)
            isDebit -> ExpenseRed
            else -> IncomeGreen
        }
        val amountPrefix = when {
            isSelf -> "₹"
            isDebit -> "-₹"
            else -> "+₹"
        }
        val (icon, iconBg) = getTagOrCategoryIconAndColor(transaction.tag.ifBlank { transaction.category }, isSelf)
        val (_, tagColor) = getTagOrCategoryIconAndColor(transaction.tag)

        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .testTag("transaction_item_${transaction.id}"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = transaction.category,
                        tint = iconBg,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Attached Tag badge
                        if (transaction.tag.isNotBlank() && !isSelf) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(tagColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🏷️ ${transaction.tag}",
                                    color = tagColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (isSelf) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "🔄 Transfer",
                                    color = Color(0xFF38BDF8),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = transaction.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                        Text(
                            text = transaction.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = iconBg,
                            fontSize = 11.sp
                        )
                        if (transaction.payee.isNotBlank() && transaction.payee != transaction.title) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 12.sp
                            )
                            Text(
                                text = transaction.payee,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountPrefix${String.format(Locale.US, "%,.2f", transaction.amount)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = amountColor
                    )
                    if (isSelf) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Self Transfer",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    } else if (transaction.upiTransactionId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UPI: ${transaction.upiTransactionId.takeLast(6)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
        return
    }

    // Overview Page Tag-Focused View
    val isSelf = transaction.isSelfTransfer
    val isDebit = transaction.type == "DEBIT"
    
    val amountColor = when {
        isSelf -> Color(0xFF38BDF8) // Neutral cyan for self transfer
        isDebit -> ExpenseRed
        else -> IncomeGreen
    }
    
    val amountPrefix = when {
        isSelf -> "₹"
        isDebit -> "-₹"
        else -> "+₹"
    }

    // Determine primary display title and category icon
    val displayTitle = when {
        transaction.tag.isNotBlank() -> transaction.tag
        isSelf -> "Self Transfer"
        isDebit -> if (transaction.payee.isNotBlank()) transaction.payee else transaction.title
        else -> if (transaction.payee.isNotBlank()) transaction.payee else transaction.title
    }

    val subtitle = when {
        transaction.tag.isNotBlank() -> {
            val direction = if (isDebit) "To: " else "From: "
            if (transaction.payee.isNotBlank()) "$direction${transaction.payee} • ${transaction.date}" else transaction.date
        }
        isSelf -> {
            if (transaction.payee.isNotBlank()) "${transaction.payee} • ${transaction.date}" else transaction.date
        }
        else -> {
            "${transaction.date} • ${transaction.category}"
        }
    }

    val (icon, iconBg) = getTagOrCategoryIconAndColor(transaction.tag.ifBlank { transaction.category }, isSelf)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("overview_tag_item_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category / Tag Icon Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = transaction.tag.ifBlank { transaction.category },
                    tint = iconBg,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Tag Badge if present
                    if (transaction.tag.isNotBlank() && !isSelf) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(iconBg.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🏷️ ${transaction.tag}",
                                color = iconBg,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (isSelf) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.25f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔄 Transfer",
                                color = Color(0xFF38BDF8),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
                if (isSelf) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Self Transfer",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                } else if (transaction.upiTransactionId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ref: ${transaction.upiTransactionId.takeLast(6)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun getTagOrCategoryIconAndColor(name: String, isSelfTransfer: Boolean = false): Pair<ImageVector, Color> {
    if (isSelfTransfer || name.equals("Self Transfer", ignoreCase = true)) {
        return Pair(Icons.Default.SwapHoriz, Color(0xFF38BDF8))
    }

    val lower = name.lowercase()
    return when {
        lower.contains("coffee") || lower.contains("cafe") || lower.contains("tea") || lower.contains("chai") ->
            Pair(Icons.Default.LocalCafe, Color(0xFFF59E0B))
        lower.contains("grocer") || lower.contains("blinkit") || lower.contains("zepto") || lower.contains("supermarket") ->
            Pair(Icons.Default.LocalGroceryStore, Color(0xFF34D399))
        lower.contains("food") || lower.contains("dining") || lower.contains("restaurant") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("snack") ->
            Pair(Icons.Default.Fastfood, Color(0xFFFBBF24))
        lower.contains("transport") || lower.contains("fuel") || lower.contains("petrol") || lower.contains("uber") || lower.contains("ola") || lower.contains("cab") ->
            Pair(Icons.Default.DirectionsCar, Color(0xFF38BDF8))
        lower.contains("bill") || lower.contains("recharge") || lower.contains("utility") || lower.contains("electricity") ->
            Pair(Icons.Default.Receipt, Color(0xFFF472B6))
        lower.contains("shopping") || lower.contains("cloth") || lower.contains("health") || lower.contains("amazon") || lower.contains("flipkart") ->
            Pair(Icons.Default.ShoppingBag, Color(0xFFA78BFA))
        lower.contains("finance") || lower.contains("bank") || lower.contains("emi") || lower.contains("loan") ->
            Pair(Icons.Default.AccountBalance, Color(0xFF818CF8))
        lower.contains("income") || lower.contains("salary") || lower.contains("cashback") || lower.contains("interest") ->
            Pair(Icons.Default.MonetizationOn, Color(0xFF10B981))
        lower.contains("transfer") || lower.contains("saving") || lower.contains("investment") ->
            Pair(Icons.Default.SwapHoriz, Color(0xFF38BDF8))
        else ->
            Pair(Icons.Default.Tag, Color(0xFF10B981))
    }
}

@Composable
fun getCategoryIconAndColor(category: String): Pair<ImageVector, Color> {
    return getTagOrCategoryIconAndColor(category)
}

