package com.itkacher.raspberry.pico.registry

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

const val OPEN_MESSAGE = "Knock-Knock, Open Up!"
const val CLOSE_MESSAGE = "Shut the Door!"
const val UDP_PORT = 65432

@OptIn(DelicateCoroutinesApi::class)
@Composable
@Preview
fun App() {
    // No architecture. Keep It Simple, Stupid. It's a demo
    val coroutineScope = rememberCoroutineScope()
    val sensorMap = mutableMapOf<String, TemperatureData>()
    val state = mutableStateOf<List<TemperatureData>>(emptyList())
    val messageFlow = UdpServer.listenUdpMessages(coroutineScope, UDP_PORT)
    val messages = remember { state }

    fun sendMessage(data: TemperatureData, shouldOpen: Boolean) = coroutineScope.launch {
        data.toOutMessage(shouldOpen)?.apply {
            UdpServer.sendMessage(this)
        }
    }

    coroutineScope.launch {
        messageFlow.collect {
            // Update or add new data by MAC address
            sensorMap[it.name ?: ""] = it
            state.value = sensorMap.values.sortedBy { it.name }
        }
    }

    ThermoSensorAppTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Temperature Sensor Data") },
                    backgroundColor = MaterialTheme.colors.primary,
                    contentColor = MaterialTheme.colors.onPrimary
                )
            },
            content = {
                SensorListView(sensorData = messages.value, ::sendMessage)
            }
        )
    }
}

private fun TemperatureData.toOutMessage(shouldOpen: Boolean): UdpServer.OutMessage? {
    return UdpServer.OutMessage(
        ip = this.senderIp ?: return null,
        port = UDP_PORT,
        message = if (shouldOpen) {
            OPEN_MESSAGE
        } else {
            CLOSE_MESSAGE
        },
    )
}
