package com.itkacher.raspberry.pico.registry

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemperatureData(
    @SerialName("name")
    val name: String? = null,
    @SerialName("temperature")
    val temperature: Double,
    @SerialName("sender_ip")
    val senderIp: String? = null,
    @SerialName("is_opened")
    val isOpened: Boolean? = null,
)
