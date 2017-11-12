package org.abimon.presence4k

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import jnr.unixsocket.UnixSocketAddress
import jnr.unixsocket.UnixSocketChannel
import org.abimon.presence4k.objects.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Rich Presence works using an IPC connection
 */
class IPCWrapper(val socketPath: String, val clientID: String) {
    companion object {
        val TEMP_PATH = System.getenv("XDG_RUNTIME_DIR") ?: System.getenv("TMPDIR") ?: System.getenv("TMP") ?: System.getenv("TEMP") ?: "/tmp"
        val VERSION = 1
        val SLEEP_TIME = 500L

        val MAPPER: ObjectMapper = ObjectMapper()
                .registerKotlinModule()
                .registerModules(Jdk8Module(), JavaTimeModule(), ParameterNamesModule())
                .registerModule(SimpleModule().addSerializer(RichPresence::class.java, RichPresenceSerializer()))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                .setSerializationInclusion(JsonInclude.Include.NON_ABSENT)

        fun obtain(clientID: String): IPCWrapper? {
            for(i in 0 until 10) {
                val socket = File("$TEMP_PATH/discord-ipc-$i")
                if(socket.exists())
                    return IPCWrapper(socket.absolutePath, clientID)
            }

            return null
        }

        fun obtainSpecific(clientID: String, ipcNum: Int): IPCWrapper? {
            val socket = File("$TEMP_PATH/discord-ipc-$ipcNum")
            if (socket.exists())
                return IPCWrapper(socket.absolutePath, clientID)

            return null
        }
    }

    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    val queue: Queue<IPCRequest> = LinkedList()
    val headerBuffer = ByteBuffer.allocate(8)
    val dataBuffer = ByteBuffer.allocate(16 * 1024)

    val address = UnixSocketAddress(socketPath)
    val channel = UnixSocketChannel.open(address).apply { configureBlocking(false) }

    init {
        queue.add(IPCRequest(Opcode.HANDSHAKE, mapOf("v" to VERSION, "client_id" to clientID)))
        executor.scheduleAtFixedRate(this::poll, 0, SLEEP_TIME, TimeUnit.MILLISECONDS)
    }

    fun poll() {
        try {
            for (i in queue.indices) {
                writeFrame(queue.remove())
                Thread.sleep(100)
            }

            var frame: IPCResponse? = readFrame()
            while(frame != null) {
                println(frame)
                Thread.sleep(100)
                frame = readFrame()
            }

        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    fun readFrame(): IPCResponse? {
        headerBuffer.clear()
        val headerRead = channel.read(headerBuffer)
        if(headerRead != headerBuffer.capacity())
            return null

        val op = headerBuffer.getLittleInt(0)
        val size = headerBuffer.getLittleInt(4)

        dataBuffer.clear()
        dataBuffer.limit(size)
        val read = channel.read(dataBuffer)
        dataBuffer.flip()

        val rawData = ByteArray(read)
        dataBuffer.get(rawData, 0, read)

        val payload = try { MAPPER.readValue(rawData, IPCPayload::class.java) } catch(json: JsonMappingException) { null }
        val error = try { MAPPER.readValue(rawData, IPCError::class.java) } catch(json: JsonMappingException) { null }

        return IPCResponse(Opcode.values()[op], payload, error)
    }

//    fun readFrameInputStream(): IPCResponse? {
//        val op = inputStream.readInt()
//        val size = inputStream.readInt()
//
//        val data = ByteArray(size)
//        val read = inputStream.read(data)
//        val rawData = data.copyOfRange(0, read)
//
//        val payload = try { MAPPER.readValue(rawData, IPCPayload::class.java) } catch(json: JsonMappingException) { null }
//        val error = try { MAPPER.readValue(rawData, IPCError::class.java) } catch(json: JsonMappingException) { null }
//
//        return IPCResponse(Opcode.values()[op], payload, error)
//    }

    fun writeFrame(request: IPCRequest) {
        val requestData = MAPPER.writeValueAsBytes(request.data)

        headerBuffer.clear()
        headerBuffer.putLittleInt(request.op.ordinal)
        headerBuffer.putLittleInt(requestData.size)
        headerBuffer.flip()

        channel.write(headerBuffer)
        channel.write(ByteBuffer.wrap(requestData))
    }

//    fun writeFrameOutputStream(request: IPCRequest): IPCResponse? {
//        val requestData = MAPPER.writeValueAsBytes(request.data)
//        outputStream.writeInt(request.op.ordinal)
//        outputStream.writeInt(requestData.size)
//
//        outputStream.write(requestData)
//        outputStream.flush()
//
//        return readFrame()
//    }

    fun OutputStream.writeInt(int: Int) {
        write(int.ushr(0) and 0xFF)
        write(int.ushr(8) and 0xFF)
        write(int.ushr(16) and 0xFF)
        write(int.ushr(24) and 0xFF)
    }

    fun InputStream.readInt(): Int {
        val ch1 = read()
        val ch2 = read()
        val ch3 = read()
        val ch4 = read()
        return (ch1 shl 0) + (ch2 shl 8) + (ch3 shl 16) + (ch4 shl 24)
    }

    fun ByteBuffer.getLittleInt(index: Int): Int
            = (this[index + 0].toInt() and 0xFF) + ((this[index + 1].toInt() and 0xFF) shl 8) + ((this[index + 2].toInt() and 0xFF) shl 16) + ((this[index + 3].toInt() and 0xFF) shl 24)

    fun ByteBuffer.putLittleInt(int: Int): ByteBuffer
            = put(int.ushr(0).toByte()).put(int.ushr(8).toByte()).put(int.ushr(16).toByte()).put(int.ushr(24).toByte())
}