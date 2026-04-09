package com.quotamaster.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BarChartEntry(
    val label: String,
    val value: Float
)

@Composable
fun BarChart(
    title: String,
    entries: List<BarChartEntry>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val maxValue = entries.maxOf { it.value }.coerceAtLeast(0.1f)
    val trackColor = accentColor.copy(alpha = 0.15f)

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 12.dp)
            ) {
                val barCount = entries.size
                val spacing = 6.dp.toPx()
                val totalSpacing = spacing * (barCount - 1)
                val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(4.dp.toPx())
                val cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                entries.forEachIndexed { index, entry ->
                    val x = index * (barWidth + spacing)
                    val fraction = (entry.value / maxValue).coerceIn(0f, 1f)
                    val barHeight = size.height * fraction
                    val trackHeight = size.height

                    // Track (background)
                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, trackHeight),
                        cornerRadius = cornerRadius
                    )

                    // Bar (value)
                    if (barHeight > 0f) {
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(x, trackHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            // Labels row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                entries.forEach { entry ->
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Values row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                entries.forEach { entry ->
                    Text(
                        text = if (entry.value > 0f) "%.1f".format(entry.value) else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = accentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
