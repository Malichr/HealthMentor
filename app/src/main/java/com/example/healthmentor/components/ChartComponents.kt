package com.example.healthmentor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.healthmentor.models.MemberStepCount
import com.example.healthmentor.models.SleepData
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StepsChart(steps: List<MemberStepCount>) {
    if (steps.isEmpty()) {
        Text(
            text = "Nincs elegendő adat a grafikon megjelenítéséhez",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    val sortedSteps = steps.sortedBy { it.date.toDate().time }
    val maxSteps = sortedSteps.maxOfOrNull { it.count } ?: 0
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
    val primaryColor = MaterialTheme.colors.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val xStep = canvasWidth / (sortedSteps.size - 1).coerceAtLeast(1)
            val yScale = (canvasHeight * 0.8f) / maxSteps.coerceAtLeast(1)

            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 0..5) {
                val y = canvasHeight - ((canvasHeight / 5) * i)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    pathEffect = dashPathEffect,
                    strokeWidth = 1.dp.toPx()
                )
            }

            for (i in 0 until sortedSteps.size - 1) {
                val startX = i * xStep
                val startY = canvasHeight - (sortedSteps[i].count * yScale)
                val endX = (i + 1) * xStep
                val endY = canvasHeight - (sortedSteps[i + 1].count * yScale)

                drawLine(
                    color = primaryColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            sortedSteps.forEachIndexed { index, step ->
                val x = index * xStep
                val y = canvasHeight - (step.count * yScale)

                drawCircle(
                    color = primaryColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        DateLabels(sortedSteps = sortedSteps, dateFormat = dateFormat)
    }
}

@Composable
private fun DateLabels(
    sortedSteps: List<MemberStepCount>,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        sortedSteps.forEach { step ->
            Text(
                text = dateFormat.format(step.date.toDate()),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp)
            )
        }
    }
}

@Composable
fun SleepChart(sleepData: List<SleepData>) {
    if (sleepData.isEmpty()) {
        Text(
            text = "Nincs elegendő adat a grafikon megjelenítéséhez",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        return
    }

    val sortedSleepData = sleepData.sortedBy { it.date.toDate().time }
    val maxSleepMinutes = sortedSleepData.maxOfOrNull { it.durationMinutes } ?: 0
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val xStep = canvasWidth / (sortedSleepData.size - 1).coerceAtLeast(1)
            val yScale = (canvasHeight * 0.8f) / maxSleepMinutes.coerceAtLeast(1)

            val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (i in 0..5) {
                val y = canvasHeight - ((canvasHeight / 5) * i)
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    pathEffect = dashPathEffect,
                    strokeWidth = 1.dp.toPx()
                )
            }

            for (i in 0 until sortedSleepData.size - 1) {
                val startX = i * xStep
                val startY = canvasHeight - (sortedSleepData[i].durationMinutes * yScale)
                val endX = (i + 1) * xStep
                val endY = canvasHeight - (sortedSleepData[i + 1].durationMinutes * yScale)

                drawLine(
                    color = Color(0xFF6200EE),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )
            }

            sortedSleepData.forEachIndexed { index, sleepEntry ->
                val x = index * xStep
                val y = canvasHeight - (sleepEntry.durationMinutes * yScale)

                drawCircle(
                    color = Color(0xFF6200EE),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        SleepDateLabels(sortedSleepData = sortedSleepData, dateFormat = dateFormat)
    }
}

@Composable
private fun SleepDateLabels(
    sortedSleepData: List<SleepData>,
    dateFormat: SimpleDateFormat
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        sortedSleepData.forEach { sleepEntry ->
            Text(
                text = dateFormat.format(sleepEntry.date.toDate()),
                style = MaterialTheme.typography.caption,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp)
            )
        }
    }
} 