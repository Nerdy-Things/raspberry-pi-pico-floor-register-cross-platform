package com.itkacher.raspberry.pico.registry

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

const val DISCOVERY_MESSAGE = "Who is there?"

object UdpServer {
    private var serverSocket: BoundDatagramSocket? = null
    private val temperatureData = MutableSharedFlow<TemperatureData>()
    private val messageQueue = mutableListOf<OutMessage>()

    private val udpLock = Mutex()
    private val messageLock = Mutex()

    private var lastBroadcastTime: Instant? = null  // Track the last broadcast time

    suspend fun sendMessage(message: OutMessage) {
        messageLock.withLock {
            messageQueue.add(message)
        }
    }

    private suspend fun sendBroadcastDiscovery(port: Int) {
        val currentTime = Clock.System.now()
        val timeElapsed = lastBroadcastTime?.let { currentTime - it } ?: Duration.INFINITE

        if (timeElapsed < 10.seconds) {
            println("UDP Broadcast message postponed.")
            return
        }
        lastBroadcastTime = currentTime

        try {
            serverSocket?.outgoing?.send(
                Datagram(
                    packet = ByteReadPacket(DISCOVERY_MESSAGE.toByteArray()),
                    address = InetSocketAddress("192.168.1.255", port) // Broadcast address
                )
            )
            println("UDP Broadcast message sent.")
        } catch (e: Exception) {
            println("UDP Error sending broadcast: ${e.message}")
        }
    }

    private suspend fun proceedMessageQueue() {
        messageLock.withLock {
            val message = messageQueue.removeFirstOrNull() ?: return
            serverSocket?.apply {
                try {
                    outgoing.send(message.toDatagram())
                    println("UDP Message was sent: $message")
                } catch (e: Exception) {
                    println("UDP Error during  sending: ${e.message}")
                }
            }
        }
    }

    private suspend fun readMessages() {
        try {
            val datagram = withTimeout(5000) {
                serverSocket?.receive()
            } ?: return
            val address = datagram.address.toString()
            val message = datagram.packet.readText()
            val data = Json.decodeFromString<TemperatureData>(message)
            try {
                temperatureData.emit(data.copy(senderIp = address.toString()))
                println("UDP Received message: $message from ${datagram.address}")
            } catch (e: Exception) {
                println("UDP Error emitting temperature data: ${e.message}")
            }
        }  catch (_: TimeoutCancellationException) {
            println("UDP Timeout: No message received.")
        }
    }

    fun listenUdpMessages(scope: CoroutineScope, port: Int = 65432): Flow<TemperatureData> {
        scope.launch(Dispatchers.IO + CoroutineName("BackgroundCoroutine")) {
            udpLock.withLock {
                try {
                    val selectorManager = SelectorManager(Dispatchers.IO)
                    serverSocket?.dispose()
                    val newSocket = aSocket(selectorManager)
                        .udp()
                        .bind(InetSocketAddress("0.0.0.0", port))
                    serverSocket = newSocket
                    println("UDP server is listening on ${serverSocket?.localAddress}")
                    while (isActive) {
                        proceedMessageQueue()
                        sendBroadcastDiscovery(port)
                        readMessages()
                    }
                } catch (e: Exception) {
                    println("UDP Error during listening: ${e.message}")
                } finally {
                    serverSocket?.dispose()
                    serverSocket = null
                    println("UDP Socket disposed.")
                }
            }
        }
        return temperatureData
    }

    data class OutMessage(
        val ip: String,
        val port: Int,
        val message: String,
    )

    private fun OutMessage.toDatagram(): Datagram {
        val packet = ByteReadPacket(message.toByteArray())
        val address = InetSocketAddress(ip, port)
        return Datagram(packet, address)
    }
}
