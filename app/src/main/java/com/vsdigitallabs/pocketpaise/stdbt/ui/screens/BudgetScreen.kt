package com.vsdigitallabs.pocketpaise.stdbt.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Budget
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.ExpenseRed
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.RupeeGreen
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.WarnOrange
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(navController: NavController, viewModel: PocketPaiseViewModel) {
    val budgets by viewModel.currentBudgets.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()

    var showAddBudgetDialog by remember { mutableStateOf(false) }

    // Aggregate monthly expenses by category to cross reference limits
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val monthlyExpenses = transactions.filter {
        val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
        it.type == "EXPENSE" && cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
    }

    val spentByCategory = monthlyExpenses.groupBy { it.category }
        .mapValues { entry -> entry.value.sumOf { it.amount } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Student Pocket Budgets", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBudgetDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_budget_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Set Budget Limit")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // General Advice Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Tips",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Smart Student Limits 🎯",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Set category limits like Food, Shopping or Travel. We will trigger system push alerts before you hit overruns!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddModerator,
                            contentDescription = "No budgets",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No pocket budget limits set yet.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tap the '+' button below to configure category budgets.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        val spent = spentByCategory[budget.category] ?: 0.0
                        BudgetRowItem(
                            budget = budget,
                            spent = spent,
                            currencySymbol = currencySymbol,
                            onDelete = { viewModel.deleteBudget(budget.id) }
                        )
                    }
                }
            }
        }

        // Add/Edit Budget Dialog
        if (showAddBudgetDialog) {
            AddBudgetDialog(
                onDismiss = { showAddBudgetDialog = false },
                onConfirm = { category, limit ->
                    viewModel.addBudget(category, limit)
                    showAddBudgetDialog = false
                }
            )
        }
    }
}

@Composable
fun BudgetRowItem(
    budget: Budget,
    spent: Double,
    currencySymbol: String,
    onDelete: () -> Unit
) {
    val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
    val isOverrun = spent > budget.limitAmount
    val isWarning = spent > budget.limitAmount * 0.8

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isOverrun -> ExpenseRed.copy(alpha = 0.1f)
                                    isWarning -> WarnOrange.copy(alpha = 0.1f)
                                    else -> RupeeGreen.copy(alpha = 0.1f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (budget.category) {
                                "Food" -> Icons.Default.Restaurant
                                "Travel" -> Icons.Default.DirectionsBus
                                "Books" -> Icons.Default.Book
                                "College Fees" -> Icons.Default.School
                                "Recharge" -> Icons.Default.PhonelinkRing
                                "Shopping" -> Icons.Default.ShoppingBag
                                "Medical" -> Icons.Default.LocalHospital
                                "Entertainment" -> Icons.Default.LocalPlay
                                "Rent" -> Icons.Default.Home
                                else -> Icons.AutoMirrored.Filled.ReceiptLong
                            },
                            contentDescription = budget.category,
                            tint = when {
                                isOverrun -> ExpenseRed
                                isWarning -> WarnOrange
                                else -> RupeeGreen
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = budget.category,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Budget",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                color = when {
                    isOverrun -> ExpenseRed
                    isWarning -> WarnOrange
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spent: $currencySymbol${String.format("%.2f", spent)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Text(
                    text = "Limit: $currencySymbol${String.format("%.2f", budget.limitAmount)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            if (isOverrun) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ExpenseRed.copy(alpha = 0.08f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = "Warning", tint = ExpenseRed, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Budget exceeded! Minimize expenses in ${budget.category} immediately.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                }
            } else if (isWarning) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarnOrange.copy(alpha = 0.08f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Warning", tint = WarnOrange, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Spent 80%+ of category budget limit. Spend wisely!",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarnOrange
                    )
                }
            }
        }
    }
}

@Composable
fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double) -> Unit
) {
    val categories = listOf("Food", "Travel", "Books", "College Fees", "Recharge", "Shopping", "Medical", "Entertainment", "Rent", "Other")
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var limitStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Define Category Budget", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Category: $selectedCategory", color = MaterialTheme.colorScheme.onSurface)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = limitStr,
                    onValueChange = { limitStr = it },
                    label = { Text("Monthly Limit Amount (INR)") },
                    placeholder = { Text("e.g. 3000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("budget_limit_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = limitStr.toDoubleOrNull()
                    if (limit == null || limit <= 0) {
                        error = "Please enter a valid limit amount greater than 0."
                    } else {
                        onConfirm(selectedCategory, limit)
                    }
                }
            ) {
                Text("Save Limit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
