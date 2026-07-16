package com.vsdigitallabs.pocketpaise.stdbt.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.vsdigitallabs.pocketpaise.stdbt.data.model.Transaction
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.ExpenseRed
import com.vsdigitallabs.pocketpaise.stdbt.ui.theme.RupeeGreen
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(navController: NavController, viewModel: PocketPaiseViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()

    var currentCalendarMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Aggregate transactions by date
    val txsGroupedByDate = remember(transactions) {
        transactions.groupBy {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            "${cal.get(Calendar.DAY_OF_MONTH)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.YEAR)}"
        }
    }

    val daysInMonth = currentCalendarMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = currentCalendarMonth.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)

    val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentCalendarMonth.time)

    // Select date key
    val selectedDateKey = "${selectedDate.get(Calendar.DAY_OF_MONTH)}-${selectedDate.get(Calendar.MONTH)}-${selectedDate.get(Calendar.YEAR)}"
    val selectedDateTransactions = txsGroupedByDate[selectedDateKey] ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Pocket Calendar Tracker", 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
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
            // Month Switcher Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        time = currentCalendarMonth.time
                        add(Calendar.MONTH, -1)
                    }
                    currentCalendarMonth = newCal
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month")
                }

                Text(
                    text = monthName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(onClick = {
                    val newCal = Calendar.getInstance().apply {
                        time = currentCalendarMonth.time
                        add(Calendar.MONTH, 1)
                    }
                    currentCalendarMonth = newCal
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Days of Week labels
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid Days
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var currentDay = 1
                val offset = firstDayOfWeek - 1 // Calendar SUN starts at 1, offset starting point

                for (row in 0..5) {
                    if (currentDay > daysInMonth) break
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..6) {
                            val isOffset = row == 0 && col < offset
                            if (isOffset || currentDay > daysInMonth) {
                                Box(modifier = Modifier.weight(1f))
                            } else {
                                val dayNum = currentDay
                                val dayKey = "$dayNum-${currentCalendarMonth.get(Calendar.MONTH)}-${currentCalendarMonth.get(Calendar.YEAR)}"
                                val dayTxs = txsGroupedByDate[dayKey] ?: emptyList()
                                val hasExpense = dayTxs.any { it.type == "EXPENSE" }
                                val hasIncome = dayTxs.any { it.type == "INCOME" }

                                val isSelected = selectedDate.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                        selectedDate.get(Calendar.MONTH) == currentCalendarMonth.get(Calendar.MONTH) &&
                                        selectedDate.get(Calendar.YEAR) == currentCalendarMonth.get(Calendar.YEAR)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedDate = Calendar.getInstance().apply {
                                                set(Calendar.DAY_OF_MONTH, dayNum)
                                                set(Calendar.MONTH, currentCalendarMonth.get(Calendar.MONTH))
                                                set(Calendar.YEAR, currentCalendarMonth.get(Calendar.YEAR))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Indicators
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            if (hasIncome) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(RupeeGreen)
                                                )
                                            }
                                            if (hasExpense) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(ExpenseRed)
                                                )
                                            }
                                        }
                                    }
                                }
                                currentDay++
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Date transaction results header
            val selectedDateString = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(selectedDate.time)
            Text(
                text = "Statement for $selectedDateString",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            if (selectedDateTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EventNote,
                            contentDescription = "Empty Date",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No pocket transactions on this date.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedDateTransactions, key = { it.id }) { tx ->
                        TransactionRowItem(
                            transaction = tx,
                            currencySymbol = currencySymbol,
                            onShare = {},
                            onDelete = { viewModel.deleteTransaction(tx) }
                        )
                    }
                }
            }
        }
    }
}
