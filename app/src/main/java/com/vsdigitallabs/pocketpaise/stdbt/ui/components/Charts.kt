package com.vsdigitallabs.pocketpaise.stdbt.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.atan2

data class ChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun InteractiveBarChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹"
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(800))
    }

    val maxValue = data.maxOfOrNull { it.value } ?: 1f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val width = size.width
                    val barWidth = width / (data.size * 2f)
                    val spaceWidth = width / (data.size * 2f)
                    
                    var tappedIndex: Int? = null
                    for (i in data.indices) {
                        val xOffset = spaceWidth / 2 + i * (barWidth + spaceWidth)
                        if (tapOffset.x in xOffset..(xOffset + barWidth)) {
                            tappedIndex = i
                            break
                        }
                    }
                    selectedIndex = if (selectedIndex == tappedIndex) null else tappedIndex
                }
            }
        ) {
            val width = size.width
            val height = size.height
            val barWidth = width / (data.size * 2f)
            val spaceWidth = width / (data.size * 2f)

            // Draw Base line
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 2f
            )

            data.forEachIndexed { i, chartData ->
                val barHeight = (chartData.value / maxValue) * height * animationProgress.value
                val xOffset = spaceWidth / 2 + i * (barWidth + spaceWidth)
                val yOffset = height - barHeight

                val isSelected = selectedIndex == i

                drawRoundRect(
                    color = if (isSelected) chartData.color else chartData.color.copy(alpha = 0.6f),
                    topLeft = Offset(xOffset, yOffset),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }

        // Tooltip
        selectedIndex?.let { index ->
            val chartData = data[index]
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "${chartData.label}: $currencySymbol${chartData.value.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InteractivePieChart(
    data: List<ChartData>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹",
    holeColor: Color = MaterialTheme.colorScheme.surface
) {
    if (data.isEmpty() || data.sumOf { it.value.toDouble() } == 0.0) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(800))
    }

    val total = data.sumOf { it.value.toDouble() }.toFloat()
    
    Box(modifier = modifier) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2, size.height / 2)
                    val dx = tapOffset.x - center.x
                    val dy = tapOffset.y - center.y
                    
                    var angle = (atan2(dy, dx) * 180 / Math.PI).toFloat()
                    // Adjust angle to match -90 start
                    angle = (angle + 90) % 360
                    if (angle < 0) angle += 360f

                    var currentAngle = 0f
                    var tappedIndex: Int? = null
                    
                    for (i in data.indices) {
                        val sweepAngle = (data[i].value / total) * 360f
                        if (angle in currentAngle..(currentAngle + sweepAngle)) {
                            tappedIndex = i
                            break
                        }
                        currentAngle += sweepAngle
                    }
                    selectedIndex = if (selectedIndex == tappedIndex) null else tappedIndex
                }
            }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2
            
            var startAngle = -90f // Start from top
            
            data.forEachIndexed { i, chartData ->
                val sweepAngle = (chartData.value / total) * 360f * animationProgress.value
                val isSelected = selectedIndex == i
                val sliceRadius = if (isSelected) radius * 1.05f else radius

                drawArc(
                    color = chartData.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - sliceRadius, center.y - sliceRadius),
                    size = Size(sliceRadius * 2, sliceRadius * 2)
                )
                startAngle += sweepAngle
            }
            
            // Draw inner hole for donut effect
            drawCircle(
                color = holeColor,
                radius = radius * 0.55f,
                center = center
            )
        }

        // Center Text
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedIndex != null) {
                val chartData = data[selectedIndex!!]
                val percent = ((chartData.value / total) * 100).toInt()
                Text(
                    text = chartData.label,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$currencySymbol${chartData.value.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = chartData.color
                )
                Text(
                    text = "$percent%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Text(
                    text = "Total",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "$currencySymbol${total.toInt()}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
