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
import com.vsdigitallabs.pocketpaise.stdbt.data.model.SavingsGoal
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.MarigoldTertiary
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.RupeeGreen
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(navController: NavController, viewModel: PocketPaiseViewModel) {
    val goals by viewModel.savingsGoals.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalForContribution by remember { mutableStateOf<SavingsGoal?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Student Savings Goals", 
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
                onClick = { showAddGoalDialog = true },
                containerColor = MarigoldTertiary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_savings_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
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
            // General Savings Balance Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOTAL SAVINGS VALUE 🐷",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencySymbol${String.format("%,.2f", currentUser?.currentSavings ?: 0.0)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Transferred from pocket money to goals.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = "No goals",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No savings goals established yet.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tap '+' to setup a target like buying college books or travel.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        SavingsGoalRowItem(
                            goal = goal,
                            currencySymbol = currencySymbol,
                            onAddFunds = { selectedGoalForContribution = goal },
                            onDelete = { viewModel.deleteGoal(goal.id) }
                        )
                    }
                }
            }
        }

        // Add Savings Goal Dialog
        if (showAddGoalDialog) {
            AddSavingsGoalDialog(
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { name, amount, days ->
                    val targetDate = System.currentTimeMillis() + (86400000L * days)
                    viewModel.addGoal(name, amount, targetDate)
                    showAddGoalDialog = false
                }
            )
        }

        // Add Contribution Dialog
        if (selectedGoalForContribution != null) {
            var contributionAmount by remember { mutableStateOf("") }
            var alertError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { selectedGoalForContribution = null },
                title = { Text(text = "Add Funds to ${selectedGoalForContribution!!.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (alertError != null) {
                            Text(text = alertError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Contribute directly from your current Pocket Balance ($currencySymbol${String.format("%.2f", currentUser?.pocketMoneyLimit ?: 0.0)}):",
                            fontSize = 13.sp
                        )
                        OutlinedTextField(
                            value = contributionAmount,
                            onValueChange = { contributionAmount = it },
                            label = { Text("Contribution Amount (INR)") },
                            placeholder = { Text("e.g. 500") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("savings_contribution_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = contributionAmount.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                alertError = "Please enter a valid contribution amount."
                            } else {
                                viewModel.addFundsToGoal(
                                    goal = selectedGoalForContribution!!,
                                    amount = amt,
                                    onError = { err -> alertError = err }
                                )
                                if (alertError == null) {
                                    selectedGoalForContribution = null
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RupeeGreen)
                    ) {
                        Text("Add Funds")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedGoalForContribution = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SavingsGoalRowItem(
    goal: SavingsGoal,
    currencySymbol: String,
    onAddFunds: () -> Unit,
    onDelete: () -> Unit
) {
    val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat() else 0f
    val dateString = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))
    val isCompleted = goal.isCompleted || pct >= 1f

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
                                if (isCompleted) RupeeGreen.copy(alpha = 0.12f)
                                else MarigoldTertiary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.WorkspacePremium,
                            contentDescription = goal.name,
                            tint = if (isCompleted) RupeeGreen else MarigoldTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = goal.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Goal",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Completion percentage progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$currencySymbol${String.format("%.0f", goal.currentAmount)} Saved",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${(pct * 100).toInt()}% Complete",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCompleted) RupeeGreen else MarigoldTertiary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { pct.coerceAtMost(1f) },
                color = if (isCompleted) RupeeGreen else MarigoldTertiary,
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
                    text = "Target Date: $dateString",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Text(
                    text = "Target: $currencySymbol${String.format("%.0f", goal.targetAmount)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (!isCompleted) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAddFunds,
                    colors = ButtonDefaults.buttonColors(containerColor = MarigoldTertiary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = "Savings", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Savings Contribution")
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(RupeeGreen.copy(alpha = 0.08f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PartyMode, contentDescription = "Celebration", tint = RupeeGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Goal Complete! Excellent money management!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RupeeGreen
                    )
                }
            }
        }
    }
}

@Composable
fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, target: Double, days: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var daysStr by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Establish Savings Goal", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (error != null) {
                    Text(text = error!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("What are you saving for?") },
                    placeholder = { Text("e.g. New Laptop or College Trip") },
                    modifier = Modifier.fillMaxWidth().testTag("goal_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it },
                    label = { Text("Target Amount (INR)") },
                    placeholder = { Text("e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = daysStr,
                    onValueChange = { daysStr = it },
                    label = { Text("Target Days to Achieve") },
                    placeholder = { Text("e.g. 60") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("goal_days_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toDoubleOrNull()
                    val days = daysStr.toIntOrNull()
                    if (name.isEmpty()) {
                        error = "Please enter a savings goal name."
                    } else if (target == null || target <= 0) {
                        error = "Please enter a valid target amount."
                    } else if (days == null || days <= 0) {
                        error = "Please enter target achievement days."
                    } else {
                        onConfirm(name, target, days)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MarigoldTertiary)
            ) {
                Text("Establish Target")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
