/*
 * FPTN Android Client
 * Copyright (C) 2026  Skokov Stanislav, Enin Sergey
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Website: https://fptn.org
 */

package org.fptn.vpn.ui.logs

import java.io.File
import java.io.RandomAccessFile

/**
 * A page of whole log lines, plus the byte offsets it spans so the next page (older or newer)
 * can be requested without re-scanning from either end of the file.
 */
data class LogsChunk(
    val lines: List<String>,
    val startOffset: Long,
    val endOffset: Long,
    val hasMoreBefore: Boolean,
)

/**
 * Reads a single log file in line-oriented pages: the most recent page first (like
 * `kubectl logs --tail`), with further pages fetched backwards as the user scrolls up and new
 * lines fetched forward as they scroll back down to the end. Takes a plain [File] rather than a
 * `Context`, so it can be exercised in a JVM unit test against a temp directory instead of
 * needing an instrumented/Robolectric environment.
 */
class LogsRepository(private val logsDir: File) {

    fun findLogFile(): File? {
        if (!logsDir.exists() || !logsDir.isDirectory) return null
        return logsDir.listFiles()
            ?.filter { it.isFile && it.canRead() }
            ?.maxByOrNull { it.lastModified() }
    }

    /** The most recent [maxBytes] worth of lines — the initial view when the screen opens. */
    fun readTail(file: File, maxBytes: Long = DEFAULT_CHUNK_BYTES): LogsChunk =
        readChunkEndingAt(file, file.length(), maxBytes)

    /** Up to [maxBytes] worth of lines immediately before [offset], or null once [offset] is 0. */
    fun readBefore(file: File, offset: Long, maxBytes: Long = DEFAULT_CHUNK_BYTES): LogsChunk? {
        if (offset <= 0) return null
        return readChunkEndingAt(file, offset, maxBytes)
    }

    /**
     * Complete lines appended after [offset], or null if nothing new has landed yet. A trailing
     * line still being written (no terminating `\n`) is deliberately left unconsumed so it isn't
     * shown half-written and then duplicated once it's finished.
     */
    fun readAfter(file: File, offset: Long): LogsChunk? {
        val length = file.length()
        if (length <= offset) return null
        val text = readRange(file, offset, length)
        val lastNewline = text.lastIndexOf('\n')
        if (lastNewline < 0) return null
        return LogsChunk(
            lines = text.substring(0, lastNewline).split("\n"),
            startOffset = offset,
            endOffset = offset + lastNewline + 1,
            hasMoreBefore = offset > 0,
        )
    }

    /**
     * Reads the [maxBytes] ending at [end], then drops a leading partial line (when the read
     * didn't start at byte 0) so every returned page starts on a line boundary and no line is
     * ever split across two pages.
     */
    private fun readChunkEndingAt(file: File, end: Long, maxBytes: Long): LogsChunk {
        val rawStart = maxOf(0, end - maxBytes)
        var text = readRange(file, rawStart, end)
        var actualStart = rawStart
        if (rawStart > 0) {
            val firstNewline = text.indexOf('\n')
            if (firstNewline >= 0) {
                actualStart = rawStart + firstNewline + 1
                text = text.substring(firstNewline + 1)
            }
        }
        val lines = if (text.isEmpty()) emptyList() else text.removeSuffix("\n").split("\n")
        return LogsChunk(lines, actualStart, end, hasMoreBefore = actualStart > 0)
    }

    private fun readRange(file: File, start: Long, end: Long): String {
        RandomAccessFile(file, "r").use { raf ->
            raf.seek(start)
            val bytes = ByteArray((end - start).toInt())
            raf.readFully(bytes)
            return String(bytes, Charsets.UTF_8)
        }
    }

    companion object {
        const val DEFAULT_CHUNK_BYTES = 64 * 1024L
    }
}
