package com.example.healthmentor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthmentor.models.SleepData
import com.example.healthmentor.models.MemberStepCount
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
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
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())

    val entries = sortedSteps.mapIndexed { index, stepData ->
        entryOf(index.toFloat(), stepData.count.toFloat())
    }
    val entryModel = entryModelOf(entries)

    val axisValueFormatter = remember {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index >= 0 && index < sortedSteps.size) {
                dateFormat.format(sortedSteps[index].date.toDate())
            } else ""
        }
    }

    val primaryColor = MaterialTheme.colors.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Chart(
            chart = lineChart(
                lines = listOf(
                    lineSpec(
                        lineColor = primaryColor
                    )
                )
            ),
            model = entryModel,
            startAxis = startAxis(),
            bottomAxis = bottomAxis(valueFormatter = axisValueFormatter),
            modifier = Modifier.fillMaxSize()
        )
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
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())

    val entries = sortedSleepData.mapIndexed { index, sleep ->
        entryOf(index.toFloat(), sleep.durationMinutes.toFloat() / 60f)
    }
    val entryModel = entryModelOf(entries)

    val axisValueFormatter = remember {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index >= 0 && index < sortedSleepData.size) {
                dateFormat.format(sortedSleepData[index].date.toDate())
            } else ""
        }
    }

    val verticalAxisFormatter = remember {
        AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
            "${value.toInt()} óra"
        }
    }

    val purpleColor = MaterialTheme.colors.secondary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Chart(
            chart = lineChart(
                lines = listOf(
                    lineSpec(
                        lineColor = purpleColor
                    )
                )
            ),
            model = entryModel,
            startAxis = startAxis(valueFormatter = verticalAxisFormatter),
            bottomAxis = bottomAxis(valueFormatter = axisValueFormatter),
            modifier = Modifier.fillMaxSize()
        )
    }
} 