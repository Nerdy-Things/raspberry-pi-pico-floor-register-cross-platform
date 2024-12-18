package com.itkacher.raspberry.pico.registry

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


@Composable
fun SensorListView(sensorData: List<TemperatureData>, onOpenClicked: (TemperatureData, Boolean) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (sensorData.isEmpty()) {
            item { EmptyItem() }
        } else {
            items(sensorData) { sensor ->
                SensorItemView(sensor = sensor, onOpenClicked = onOpenClicked)
            }
        }
    }
}

@Composable
fun EmptyItem() {
    Text(
        text = "No sensors so far.", // String hardcode, this is a demo
        style = MaterialTheme.typography.body1.copy(color = Color.White),
    )
}

@Composable
fun SensorItemView(sensor: TemperatureData, onOpenClicked: (TemperatureData, Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${sensor.name}",
                style = MaterialTheme.typography.body1.copy(color = Color.White),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${sensor.temperature}°C",
                style = MaterialTheme.typography.body1.copy(color = Color.White),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = sensor.isOpened == true,
                enabled = sensor.isOpened != null,
                onCheckedChange = { onOpenClicked(sensor, sensor.isOpened?.not() == true) }
            )
            Text(text = if (sensor.isOpened == true) "OPENED" else "CLOSED")

        }
    }
}