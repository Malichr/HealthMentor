package com.example.healthmentor.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthmentor.models.MemberStepCount
import com.example.healthmentor.models.SleepData
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
import com.google.firebase.Timestamp

private fun Date.truncateToDay(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.time
}

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

    val groupedByDay = steps
        .groupBy { it.date.toDate().truncateToDay() }
        .mapValues { (_, daySteps) ->
            daySteps.maxByOrNull { it.date.toDate().time }!!
        }

    val sortedDays = groupedByDay.keys.sortedBy { it.time }
    val minPointsNeeded = 12
    val needExtraPoints = sortedDays.size < minPointsNeeded

    val allDays = if (needExtraPoints) {
        val extraPointsNeeded = minPointsNeeded - sortedDays.size
        val extraPointsBefore = extraPointsNeeded / 2
        val extraPointsAfter = extraPointsNeeded - extraPointsBefore
        
        val calendar = Calendar.getInstance()

        val daysBefore = mutableListOf<Date>()
        if (sortedDays.isNotEmpty()) {
            calendar.time = sortedDays.first()
            for (i in 1..extraPointsBefore) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                daysBefore.add(calendar.time)
            }
        }

        val daysAfter = mutableListOf<Date>()
        if (sortedDays.isNotEmpty()) {
            calendar.time = sortedDays.last()
            for (i in 1..extraPointsAfter) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                daysAfter.add(calendar.time)
            }
        }
        
        daysBefore.reversed() + sortedDays + daysAfter
    } else {
        sortedDays
    }
    
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
    
    val entries = allDays.mapIndexed { index, date ->
        val stepCount = if (groupedByDay.containsKey(date)) {
            groupedByDay[date]!!.count.toFloat()
        } else {
            0f
        }
        entryOf(index.toFloat(), stepCount)
    }
    
    val entryModel = entryModelOf(entries)
    
    val axisValueFormatter = remember {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index >= 0 && index < allDays.size) {
                dateFormat.format(allDays[index])
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
                        lineColor = primaryColor,
                        lineThickness = 2.dp
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

    val groupedByDay = sleepData
        .groupBy { it.date.toDate().truncateToDay() }
        .mapValues { (_, daySleep) ->
            daySleep.maxByOrNull { it.date.toDate().time }!!
        }

    val sortedDays = groupedByDay.keys.sortedBy { it.time }
    val minPointsNeeded = 12
    val needExtraPoints = sortedDays.size < minPointsNeeded

    val allDays = if (needExtraPoints) {
        val extraPointsNeeded = minPointsNeeded - sortedDays.size
        val extraPointsBefore = extraPointsNeeded / 2
        val extraPointsAfter = extraPointsNeeded - extraPointsBefore
        
        val calendar = Calendar.getInstance()

        val daysBefore = mutableListOf<Date>()
        if (sortedDays.isNotEmpty()) {
            calendar.time = sortedDays.first()
            for (i in 1..extraPointsBefore) {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                daysBefore.add(calendar.time)
            }
        }

        val daysAfter = mutableListOf<Date>()
        if (sortedDays.isNotEmpty()) {
            calendar.time = sortedDays.last()
            for (i in 1..extraPointsAfter) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                daysAfter.add(calendar.time)
            }
        }
        
        daysBefore.reversed() + sortedDays + daysAfter
    } else {
        sortedDays
    }
    
    val dateFormat = SimpleDateFormat("MM.dd", Locale.getDefault())
    
    val entries = allDays.mapIndexed { index, date ->
        val sleepDuration = if (groupedByDay.containsKey(date)) {
            groupedByDay[date]!!.durationMinutes.toFloat() / 60f
        } else {
            0f
        }
        entryOf(index.toFloat(), sleepDuration)
    }
    
    val entryModel = entryModelOf(entries)

    val axisValueFormatter = remember {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.toInt()
            if (index >= 0 && index < allDays.size) {
                dateFormat.format(allDays[index])
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
                        lineColor = purpleColor,
                        lineThickness = 2.dp
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