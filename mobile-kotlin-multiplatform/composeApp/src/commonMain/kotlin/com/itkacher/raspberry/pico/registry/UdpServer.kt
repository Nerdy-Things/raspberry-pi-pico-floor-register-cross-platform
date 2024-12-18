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

    private suspend fun sendBroadcastDiscovery(broadcastIp: String, sendToPort: Int) {
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
                    address = InetSocketAddress(broadcastIp, sendToPort) // Broadcast address
                )
            )
            println("UDP Broadcast message sent.")
        } catch (e: Exception) {
            e.printStackTrace()
            println("UDP Error sending broadcast: ${e.message}")
        }
    }

    private suspend fun proceedMessageQueue(sendToPort: Int) {
        messageLock.withLock {
            var message = messageQueue.removeFirstOrNull() ?: return
            serverSocket?.apply {
                try {
                    message = message.copy(port = sendToPort)
                    println("UDP Trying to send ${message.toDatagram()}")
                    outgoing.send(message.toDatagram())
                    println("UDP message is sent")
                } catch (e: Exception) {
                    println("UDP Error during  sending: $e")
                }
            }
        }
    }

    private suspend fun readMessages() {
        try {
            val datagram = withTimeout(500) {
                serverSocket?.receive()
            } ?: return
            val address = datagram.address.toString()
            val message = datagram.packet.readText()
            if (message.startsWith("{").not()) return
            println("UDP Received message $message")
            val data = Json.decodeFromString<TemperatureData>(message)
            try {
                // /192.168.0.232:56085 ===> 192.168.0.232
                var ip = address.toString().split(":").first().replace("/", "")
                temperatureData.emit(data.copy(senderIp = ip))
                println("UDP Received message: $message from ${datagram.address}")
            } catch (e: Exception) {
                println("UDP Error emitting temperature data: ${e.message}")
            }
        } catch (_: TimeoutCancellationException) {

        } catch (e: Exception) {
            println("UDP readMessages error: ${e.message}")
        }
    }

    fun listenUdpMessages(scope: CoroutineScope, broadcastIp: String, port: Int = 65432):
            Flow<TemperatureData> {
        scope.launch(Dispatchers.IO + CoroutineName("BackgroundCoroutine")) {
            udpLock.withLock {
                try {
                    val selectorManager = SelectorManager(Dispatchers.IO)
                    serverSocket?.dispose()
                    val newSocket = aSocket(selectorManager)
                        .udp()
                        .bind(InetSocketAddress("0.0.0.0", port)) {
                            broadcast = true
                        }

                    serverSocket = newSocket
                    println("UDP server is listening on ${serverSocket?.localAddress}")
                    while (isActive) {
                        proceedMessageQueue(port)
                        sendBroadcastDiscovery(broadcastIp, port)
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
