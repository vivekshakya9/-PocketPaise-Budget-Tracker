@file:Suppress("DEPRECATION")
package com.vsdigitallabs.pocketpaise.stdbt.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import com.vsdigitallabs.pocketpaise.stdbt.ui.navigation.Screen
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.*
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import com.vsdigitallabs.pocketpaise.stdbt.ui.components.AdMobBanner
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(navController: NavController, viewModel: PocketPaiseViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val budgets by viewModel.currentBudgets.collectAsState()
    val goals by viewModel.savingsGoals.collectAsState()
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()

    // Dialog trigger states
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    // Calculations
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayTransactions = transactions.filter { it.timestamp >= todayStart }
    val todayExpense = todayTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val todayIncome = todayTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }

    // Monthly values
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val monthlyTransactions = transactions.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    }
    val monthlyExpense = monthlyTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalBudgetLimits = budgets.sumOf { it.limitAmount }
    val remainingBudget = if (totalBudgetLimits > 0) totalBudgetLimits - monthlyExpense else 0.0

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NAMASTE,",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentUser?.name ?: "Student",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { navController.navigate(Screen.Calendar.route) }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pocket Calendar",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { navController.navigate(Screen.Notification.route) }) {
                            Box {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (unreadNotificationsCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unreadNotificationsCount.toString(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FloatingActionButton(
                    onClick = { showAddIncomeDialog = true },
                    containerColor = MarigoldTertiary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_income_fab")
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = "Add Income")
                }

                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Expense")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp)
        ) {
            // Balance Card
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("balance_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Balance",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol${String.format("%,.2f", currentUser?.pocketMoneyLimit ?: 0.0)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Monthly",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Income Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "INCOME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "+ $currencySymbol${String.format("%,.0f", todayIncome)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        // Expense Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.1f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "EXPENSES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "- $currencySymbol${String.format("%,.0f", todayExpense)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Income Action
                QuickActionItem(
                    label = "Income",
                    icon = "+",
                    containerColor = Color(0xFFD1FAE5),
                    contentColor = Color(0xFF047857),
                    onClick = { showAddIncomeDialog = true }
                )
                // Expense Action
                QuickActionItem(
                    label = "Expense",
                    icon = "−",
                    containerColor = Color(0xFFFFE4E6),
                    contentColor = Color(0xFFBE123C),
                    onClick = { showAddExpenseDialog = true }
                )
                // Budget Action
                QuickActionItem(
                    label = "Budget",
                    icon = "↗",
                    containerColor = Color(0xFFE0F2FE),
                    contentColor = Color(0xFF0369A1),
                    onClick = { navController.navigate(Screen.Budget.route) }
                )
                // Goals Action
                QuickActionItem(
                    label = "Goals",
                    icon = "☆",
                    containerColor = Color(0xFFFEF3C7),
                    contentColor = Color(0xFFB45309),
                    onClick = { navController.navigate(Screen.Savings.route) }
                )
            }

            // Budget progress card
            if (totalBudgetLimits > 0) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Budget Status",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "$currencySymbol${String.format("%.0f", remainingBudget)} left",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBudget < totalBudgetLimits * 0.15) ExpenseRed else RupeeGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val progress = if (totalBudgetLimits > 0) (monthlyExpense / totalBudgetLimits).toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape),
                            color = when {
                                progress >= 1f -> ExpenseRed
                                progress >= 0.8f -> WarnOrange
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Spent: $currencySymbol${String.format("%.0f", monthlyExpense)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Limit: $currencySymbol${String.format("%.0f", totalBudgetLimits)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Quick insights / Custom Category spending Chart
            Text(
                text = "Expense Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val expenseTransactions = monthlyTransactions.filter { it.type == "EXPENSE" }
                    if (expenseTransactions.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Empty insights",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No expenses recorded this month yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Start logging expenses to generate insights!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    } else {
                        val categoryGroups = expenseTransactions.groupBy { it.category }
                            .mapValues { entry -> entry.value.sumOf { it.amount } }

                        val totalMonthlyExpense = categoryGroups.values.sum()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Draw Custom Donut Chart on a Canvas
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    var startAngle = 270f
                                    val colors = listOf(
                                        Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784),
                                        Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
                                        Color(0xFFFF8A65), Color(0xFFAED581), Color(0xFF90A4AE)
                                    )
                                    
                                    categoryGroups.entries.forEachIndexed { index, entry ->
                                        val sweep = ((entry.value / totalMonthlyExpense) * 360f).toFloat()
                                        drawArc(
                                            color = colors[index % colors.size],
                                            startAngle = startAngle,
                                            sweepAngle = sweep,
                                            useCenter = false,
                                            style = Stroke(width = 30f)
                                        )
                                        startAngle += sweep
                                    }
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Spent",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "$currencySymbol${String.format("%.0f", totalMonthlyExpense)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Legend breakdown
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val colors = listOf(
                                    Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784),
                                    Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
                                    Color(0xFFFF8A65), Color(0xFFAED581), Color(0xFF90A4AE)
                                )
                                categoryGroups.entries.take(4).forEachIndexed { index, entry ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(colors[index % colors.size])
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.key,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 75.dp)
                                            )
                                        }
                                        Text(
                                            text = "${((entry.value / totalMonthlyExpense) * 100).toInt()}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Savings summary list
            if (goals.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Savings Goals Progress",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { navController.navigate(Screen.Savings.route) }) {
                        Text("View All")
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        goals.take(2).forEach { goal ->
                            val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = goal.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${(pct * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { pct.coerceAtMost(1f) },
                                    color = MarigoldTertiary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$currencySymbol${String.format("%.0f", goal.currentAmount)} saved",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "Target: $currencySymbol${String.format("%.0f", goal.targetAmount)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Transactions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = { navController.navigate(Screen.Transactions.route) }) {
                    Text("See History")
                }
            }

            if (transactions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "No transactions",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No pocket transactions yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        transactions.take(4).forEach { tx ->
                            TransactionItem(
                                transaction = tx,
                                currencySymbol = currencySymbol,
                                onDelete = { viewModel.deleteTransaction(tx) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Extra space for FAB and bottom navigation
        }

        // Add Expense Dialog
        if (showAddExpenseDialog) {
            AddTransactionDialog(
                type = "EXPENSE",
                categories = listOf("Food", "Travel", "Books", "College Fees", "Recharge", "Shopping", "Medical", "Entertainment", "Rent", "Other"),
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { title, amount, category, notes ->
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = "EXPENSE",
                        category = category,
                        notes = notes
                    )
                    showAddExpenseDialog = false
                }
            )
        }

        // Add Income Dialog
        if (showAddIncomeDialog) {
            AddTransactionDialog(
                type = "INCOME",
                categories = listOf("Pocket Money", "Parents", "Scholarship", "Part-Time Job", "Gift", "Other"),
                onDismiss = { showAddIncomeDialog = false },
                onConfirm = { title, amount, category, notes ->
                    viewModel.addTransaction(
                        title = title,
                        amount = amount,
                        type = "INCOME",
                        category = category,
                        notes = notes
                    )
                    showAddIncomeDialog = false
                }
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val isExpense = transaction.type == "EXPENSE"
    val dateString = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Category Icon background rounded-xl box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isExpense) ExpenseRed.copy(alpha = 0.1f)
                        else RupeeGreen.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.category) {
                        "Food" -> Icons.Default.Restaurant
                        "Travel" -> Icons.Default.DirectionsBus
                        "Books" -> Icons.Default.Book
                        "College Fees" -> Icons.Default.School
                        "Recharge" -> Icons.Default.PhonelinkRing
                        "Shopping" -> Icons.Default.ShoppingBag
                        "Medical" -> Icons.Default.LocalHospital
                        "Entertainment" -> Icons.Default.LocalPlay
                        "Rent" -> Icons.Default.Home
                        "Pocket Money", "Parents" -> Icons.Default.CardGiftcard
                        "Scholarship" -> Icons.Default.WorkspacePremium
                        "Part-Time Job" -> Icons.Default.Work
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = transaction.category,
                    tint = if (isExpense) ExpenseRed else RupeeGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = transaction.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.category} • $dateString",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format("%.2f", transaction.amount)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isExpense) ExpenseRed else RupeeGreen
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        AdMobBanner(context = context)
    }
}

@Composable
fun AddTransactionDialog(
    type: String,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, notes: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    var showCategoryDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Daily $type",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (type == "EXPENSE") ExpenseRed else RupeeGreen
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Purpose") },
                    placeholder = { Text("e.g. Chai and Samosa") },
                    modifier = Modifier.fillMaxWidth().testTag("tx_title_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount (INR)") },
                    placeholder = { Text("e.g. 50") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("tx_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showCategoryDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Category: $selectedCategory", color = MaterialTheme.colorScheme.onSurface)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(text = cat) },
                                onClick = {
                                    selectedCategory = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Add Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull()
                    if (title.isEmpty()) {
                        error = "Please enter a transaction title."
                    } else if (amount == null || amount <= 0) {
                        error = "Please enter a valid amount greater than 0."
                    } else {
                        onConfirm(title, amount, selectedCategory, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "EXPENSE") ExpenseRed else RupeeGreen
                )
            ) {
                Text("Confirm Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun QuickActionItem(
    label: String,
    icon: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
        )
    }
}

