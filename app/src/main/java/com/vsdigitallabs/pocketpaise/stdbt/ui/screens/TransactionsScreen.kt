package com.vsdigitallabs.pocketpaise.stdbt.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.ExpenseRed
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.MarigoldTertiary
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.RupeeGreen
import com.vsdigitallabs.pocketpaise.stdbt.utils.PdfExporter
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(navController: NavController, viewModel: PocketPaiseViewModel) {
    val context = LocalContext.current
    val transactions by viewModel.filteredTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val selectedType by viewModel.selectedTypeFilter.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()

    val customExpenseCategories by viewModel.customExpenseCategories.collectAsState()
    val customIncomeCategories by viewModel.customIncomeCategories.collectAsState()

    var showExportDialog by remember { mutableStateOf<String?>(null) } // "PDF" or "Excel" or null
    val coroutineScope = rememberCoroutineScope()
    var showCategoryFilterMenu by remember { mutableStateOf(false) }

    val allCategories = remember(customExpenseCategories, customIncomeCategories) {
        val base = listOf(
            "All Categories", "Food", "Travel", "Books", "College Fees", "Recharge", 
            "Shopping", "Medical", "Entertainment", "Rent", "Savings", "Pocket Money", 
            "Parents", "Scholarship", "Part-Time Job", "Gift"
        )
        (base + customExpenseCategories + customIncomeCategories + listOf("Other")).distinct()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Pocket Passbook", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                actions = {
                    IconButton(onClick = { showExportDialog = "PDF" }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { showExportDialog = "Excel" }) {
                        Icon(Icons.Default.TableChart, contentDescription = "Export Excel", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                label = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_bar"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filtering chips
            val chipsScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(chipsScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type selector: All, Income, Expense
                FilterChip(
                    selected = selectedType == null,
                    onClick = { viewModel.setTypeFilter(null) },
                    label = { Text("All Tx") }
                )

                FilterChip(
                    selected = selectedType == "INCOME",
                    onClick = { viewModel.setTypeFilter("INCOME") },
                    label = { Text("Income") }
                )

                FilterChip(
                    selected = selectedType == "EXPENSE",
                    onClick = { viewModel.setTypeFilter("EXPENSE") },
                    label = { Text("Expense") }
                )

                // Category dropdown filter
                Box {
                    AssistChip(
                        onClick = { showCategoryFilterMenu = true },
                        label = { Text(selectedCategory ?: "Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") }
                    )

                    DropdownMenu(
                        expanded = showCategoryFilterMenu,
                        onDismissRequest = { showCategoryFilterMenu = false }
                    ) {
                        allCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    if (cat == "All Categories") {
                                        viewModel.setCategoryFilter(null)
                                    } else {
                                        viewModel.setCategoryFilter(cat)
                                    }
                                    showCategoryFilterMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transactions list
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterListOff,
                            contentDescription = "No matches",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions found.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Try modifying your search or filter settings.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            currencySymbol = currencySymbol,
                            onShare = {
                                val text = "PocketPaise Transaction Info:\n" +
                                        "Title: ${tx.title}\n" +
                                        "Amount: $currencySymbol${tx.amount}\n" +
                                        "Type: ${tx.type}\n" +
                                        "Category: ${tx.category}\n" +
                                        "Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(tx.timestamp))}\n" +
                                        "Shared via Student PocketPaise Tracker."
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, text)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Transaction via"))
                            },
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog != null) {
            AlertDialog(
                onDismissRequest = { showExportDialog = null },
                icon = {
                    Icon(
                        imageVector = if (showExportDialog == "PDF") Icons.Default.PictureAsPdf else Icons.Default.TableChart,
                        contentDescription = "Export icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = { Text(text = "Export as $showExportDialog") },
                text = {
                    Text(
                        text = "Your passbook statements have been aggregated! PocketPaise is packaging your statement history. Click download to save the student financial report file."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showExportDialog = null
                            coroutineScope.launch {
                                PdfExporter.exportTransactionsToPdf(context, transactions)
                            }
                        }
                    ) {
                        Text(text = "Download File")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = null }) {
                        Text(text = "Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionRowItem(
    transaction: Transaction,
    currencySymbol: String,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val isExpense = transaction.type == "EXPENSE"
    val dateString = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
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
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${transaction.category} • $dateString",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Text(
                    text = "${if (isExpense) "-" else "+"}$currencySymbol${String.format("%.2f", transaction.amount)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isExpense) ExpenseRed else RupeeGreen
                )
            }

            if (transaction.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${transaction.notes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ExpenseRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
