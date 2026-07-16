package com.vsdigitallabs.pocketpaise.stdbt.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MultilineChart
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.vsdigitallabs.pocketpaise.stdbt.viewmodel.PocketPaiseViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(navController: NavController, viewModel: PocketPaiseViewModel) {
    val transactions by viewModel.allTransactions.collectAsState()
    val currencySymbol by viewModel.selectedCurrency.collectAsState()

    var selectedReportTab by remember { mutableStateOf("Monthly") } // "Daily", "Weekly", "Monthly", "Yearly"
    val scrollState = rememberScrollState()

    // Filtering Transactions based on Selected Time Scope
    val calendar = Calendar.getInstance()
    val now = System.currentTimeMillis()

    val filteredTxs = transactions.filter { tx ->
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
        when (selectedReportTab) {
            "Daily" -> {
                txCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR) &&
                        txCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }
            "Weekly" -> {
                txCal.get(Calendar.WEEK_OF_YEAR) == calendar.get(Calendar.WEEK_OF_YEAR) &&
                        txCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }
            "Monthly" -> {
                txCal.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                        txCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }
            "Yearly" -> {
                txCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
            }
            else -> true
        }
    }

    val totalIncome = filteredTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpense = filteredTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Pocket Paise Reports", 
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
        ) {
            // Segmented Controls for Time Ranges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Daily", "Weekly", "Monthly", "Yearly").forEach { tab ->
                    val isSelected = selectedReportTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { selectedReportTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredTxs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No metrics for $selectedReportTab yet.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Complete student transactions to view analytics.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Comparative Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Income",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencySymbol${String.format("%,.0f", totalIncome)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RupeeGreen
                                )
                            }

                            // Vertical divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Expense",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currencySymbol${String.format("%,.0f", totalExpense)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Chart 1: Bar Chart (Income vs Expense comparison)
                    Row(
                        modifier = Modifier.padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BarChart, contentDescription = "Bar Chart", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income vs Expense Bar Chart",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val maxVal = maxOf(totalIncome, totalExpense, 1000.0).toFloat()
                                val barWidth = 60.dp.toPx()
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                // Draw Income Bar
                                val incomeHeight = (totalIncome.toFloat() / maxVal) * canvasHeight
                                drawRect(
                                    color = RupeeGreen,
                                    topLeft = Offset(x = (canvasWidth / 2) - barWidth - 20f, y = canvasHeight - incomeHeight),
                                    size = Size(width = barWidth, height = incomeHeight)
                                )

                                // Draw Expense Bar
                                val expenseHeight = (totalExpense.toFloat() / maxVal) * canvasHeight
                                drawRect(
                                    color = ExpenseRed,
                                    topLeft = Offset(x = (canvasWidth / 2) + 20f, y = canvasHeight - expenseHeight),
                                    size = Size(width = barWidth, height = expenseHeight)
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(text = "Income", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RupeeGreen)
                            Text(text = "Expense", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                        }
                    }

                    // Chart 2: Spending Category breakdowns
                    val expenseTransactions = filteredTxs.filter { it.type == "EXPENSE" }
                    if (expenseTransactions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PieChart, contentDescription = "Pie Chart", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Category Spending Breakdown",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            val categoryGroups = expenseTransactions.groupBy { it.category }
                                .mapValues { entry -> entry.value.sumOf { it.amount } }

                            val totalSpent = categoryGroups.values.sum()

                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                                                val sweep = ((entry.value / totalSpent) * 360f).toFloat()
                                                drawArc(
                                                    color = colors[index % colors.size],
                                                    startAngle = startAngle,
                                                    sweepAngle = sweep,
                                                    useCenter = true
                                                )
                                                startAngle += sweep
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // Display category labels inside reports
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val colors = listOf(
                                            Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFF81C784),
                                            Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
                                            Color(0xFFFF8A65), Color(0xFFAED581), Color(0xFF90A4AE)
                                        )

                                        categoryGroups.entries.forEachIndexed { index, entry ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(colors[index % colors.size])
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = entry.key,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                Text(
                                                    text = "$currencySymbol${String.format("%.0f", entry.value)} (${((entry.value / totalSpent) * 100).toInt()}%)",
                                                    fontSize = 11.sp,
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

                    // Chart 3: Spending Trend Line Chart
                    if (expenseTransactions.size >= 2) {
                        Row(
                            modifier = Modifier.padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MultilineChart, contentDescription = "Trends", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Spending Progression Trend",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(24.dp)
                            ) {
                                val primaryColor = MaterialTheme.colorScheme.primary
                                val sortedExpenses = expenseTransactions.sortedBy { it.timestamp }
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val maxAmt = sortedExpenses.maxOf { it.amount }.toFloat()
                                    val canvasWidth = size.width
                                    val canvasHeight = size.height

                                    val points = sortedExpenses.mapIndexed { idx, tx ->
                                        val x = (idx.toFloat() / (sortedExpenses.size - 1)) * canvasWidth
                                        val y = canvasHeight - ((tx.amount.toFloat() / maxAmt) * canvasHeight)
                                        Offset(x, y)
                                    }

                                    // Draw connecting trend lines
                                    for (i in 0 until points.size - 1) {
                                        drawLine(
                                            color = MarigoldTertiary,
                                            start = points[i],
                                            end = points[i + 1],
                                            strokeWidth = 6f
                                        )
                                        // Draw point dots
                                        drawCircle(
                                            color = primaryColor,
                                            radius = 8f,
                                            center = points[i]
                                        )
                                    }
                                    drawCircle(
                                        color = primaryColor,
                                        radius = 8f,
                                        center = points.last()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
