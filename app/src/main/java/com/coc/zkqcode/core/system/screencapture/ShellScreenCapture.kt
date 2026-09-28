package com.coc.zkqcode.core.system.screencapture

import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Persistent root shell screen capture.
 * Maintains a long-lived `su` process and uses `screencap -p` to read PNG data
 * directly from stdout by parsing PNG chunks, avoiding all file I/O.
 */
object ShellScreenCapture {

    private var process: Process? = null
    private var shellStdin: OutputStream? = null
    private var shellStdout: BufferedInputStream? = null

    private val connectionLock = Any()
    private val captureMutex = Mutex()

    private const val INIT_MARKER = "__SHELL_READY__"
    private const val STREAM_BUFFER_SIZE = 1024 * 1024 // 1MB read buffer

    /**
     * Ensure the persistent su shell is alive. Recreate if the process is dead.
     */
    private fun ensureConnection() {
        synchronized(connectionLock) {
            if (process != null && isProcessAlive(process!!)) return
            closeConnectionLocked()

            try {
                val pb = ProcessBuilder("su")
                pb.redirectErrorStream(false)
                val proc = pb.start()
                process = proc
                shellStdin = proc.outputStream
                shellStdout = BufferedInputStream(proc.inputStream, STREAM_BUFFER_SIZE)

                // Drain any initial output (e.g. Magisk greeting) by sending a marker
                drainUntilMarker()

            } catch (e: Exception) {
                closeConnectionLocked()
                throw e
            }
        }
    }

    /**
     * Send a known echo marker and discard all bytes until the marker line appears.
     * This ensures the shell is ready and any startup messages are consumed.
     */
    private fun drainUntilMarker() {
        val stdin = shellStdin ?: return
        val stdout = shellStdout ?: return

        stdin.write("echo $INIT_MARKER\n".toByteArray())
        stdin.flush()

        val lineBuffer = StringBuilder()
        while (true) {
            val b = stdout.read()
            if (b == -1) throw IOException("Shell closed during initialization")
            if (b == '\n'.code) {
                if (lineBuffer.toString().trim() == INIT_MARKER) return
                lineBuffer.clear()
            } else {
                lineBuffer.append(b.toChar())
            }
        }
    }

    private fun closeConnectionLocked() {
        runCatching { shellStdin?.close() }
        runCatching { shellStdout?.close() }
        runCatching { process?.destroy() }
        process = null
        shellStdin = null
        shellStdout = null
    }

    /**
     * Release the persistent shell connection and all resources.
     */
    fun release() {
        synchronized(connectionLock) {
            closeConnectionLocked()
        }
    }

    /**
     * Capture the screen via persistent root shell.
     * Thread-safe; only one capture runs at a time. Retries once on connection failure.
     */
    suspend fun capture(asBitmap: Boolean): Any? = captureMutex.withLock {
        withContext(Dispatchers.IO) {
            captureInternal(asBitmap)
        }
    }

    private fun captureInternal(asBitmap: Boolean): Any? {
        // Retry once: first attempt may fail if the connection died between captures
        repeat(2) { attempt ->
            try {
                ensureConnection()
                val stdin = shellStdin ?: return null
                val stdout = shellStdout ?: return null

                val result = captureViaPng(stdin, stdout, asBitmap)

                if (result != null) {
                    return result
                }

                return null
            } catch (e: Exception) {
                synchronized(connectionLock) { closeConnectionLocked() }
                if (attempt > 0) return null
            }
        }
        return null
    }

    /** 捕获命令序号：每条命令唯一，用于识别输出流里的陈旧帧。 */
    private var commandSeq = 0

    // ======================== PNG screencap ========================

    /**
     * Capture using `screencap -p` and parse PNG chunks from the stream.
     * PNG structure: 8-byte signature, then chunks until IEND.
     * Each chunk: 4-byte length (big-endian) + 4-byte type + length data + 4-byte CRC.
     */
    private fun captureViaPng(
        stdin: OutputStream,
        stdout: InputStream,
        asBitmap: Boolean
    ): Any? {
        // 每条命令带唯一序号标记：读完 PNG 后必须读到**本序号**的标记行才算拿到本次实时画面。
        // 持久 shell 的输出流若残留上一次未消费的 [PNG+标记]，序号不匹配就跳过继续读，
        // 避免返回陈旧帧；同时保持持久连接（不反复起 su 进程，否则 Magisk 每次都弹授权提示）。
        commandSeq++
        val marker = "__ZKQ_CAP_${commandSeq}__"
        stdin.write("screencap -p; echo $marker\n".toByteArray())
        stdin.flush()

        var pngBytes: ByteArray? = null
        var matched = false
        // 注意必须用 for+break：repeat 里的 return@repeat 是 continue 而不是 break，
        // 匹配成功后继续循环会阻塞在下一个 readPngFromStream 上（等永远不来的输出）
        for (attempt in 0 until 3) {
            pngBytes = readPngFromStream(stdout)
            val line = readLineUntilNewline(stdout)
            if (line.trim() == marker) {
                matched = true
                break
            }
            // 标记序号不匹配 → 刚读到的 PNG 是流里残留的陈旧输出，丢弃并继续读下一个
        }
        val data = pngBytes
        if (!matched || data == null) {
            throw IOException("screencap marker not matched (stale stream?)")
        }

        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
            ?: return null

        if (asBitmap) return bitmap

        // Convert Bitmap to CaptureResult (raw RGBA buffer)
        val width = bitmap.width
        val height = bitmap.height
        val pixelStride = 4
        val rowStride = width * pixelStride
        val buffer = ByteBuffer.allocateDirect(height * rowStride)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.flip()
        bitmap.recycle()

        return ScreenCaptureManager.CaptureResult(buffer, width, height, pixelStride, rowStride)
    }

    /** 逐字节读一行（到 '\n' 为止），用于消费命令结束标记。 */
    private fun readLineUntilNewline(stream: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = stream.read()
            if (b == -1) throw IOException("Stream closed while reading capture marker")
            if (b == '\n'.code) break
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    /**
     * Read a complete PNG from the stream by parsing its chunk structure.
     * Stops after the IEND chunk, leaving the stream positioned for the next command.
     */
    private fun readPngFromStream(stream: InputStream): ByteArray {
        val output = ByteArrayOutputStream()

        // 8-byte PNG signature
        val signature = readExactly(stream, 8)
        output.write(signature)

        // Validate PNG magic bytes
        val expectedSig = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        )
        if (!signature.contentEquals(expectedSig)) {
            throw IOException(
                "Invalid PNG signature: ${signature.joinToString(" ") { "%02X".format(it) }}"
            )
        }

        // Read chunks until IEND
        while (true) {
            // Chunk header: 4-byte data length + 4-byte type
            val chunkHeader = readExactly(stream, 8)
            output.write(chunkHeader)

            val length = ByteBuffer.wrap(chunkHeader, 0, 4).order(ByteOrder.BIG_ENDIAN).getInt()
            val type = String(chunkHeader, 4, 4, Charsets.US_ASCII)

            if (length !in 0..50_000_000) {
                throw IOException("PNG chunk '$type' has unreasonable length: $length")
            }

            // Read chunk data
            if (length > 0) {
                val data = readExactly(stream, length)
                output.write(data)
            }

            // Read 4-byte CRC
            val crc = readExactly(stream, 4)
            output.write(crc)

            if (type == "IEND") break
        }

        return output.toByteArray()
    }

    // ======================== Utility ========================

    /**
     * API 24-compatible check for whether a process is still running.
     * Uses exitValue() which throws IllegalThreadStateException if the process has not yet terminated.
     */
    private fun isProcessAlive(proc: Process): Boolean {
        return try {
            proc.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }

    /**
     * Read exactly [size] bytes from [stream], blocking until all bytes are received.
     * Throws IOException if the stream closes before all bytes are read.
     */
    private fun readExactly(stream: InputStream, size: Int): ByteArray {
        val buf = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = stream.read(buf, offset, size - offset)
            if (read == -1) throw IOException("Stream closed prematurely, read $offset/$size bytes")
            offset += read
        }
        return buf
    }
}
