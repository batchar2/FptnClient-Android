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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LogsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `missing logs directory has no log file`() {
        val logsDir = File(tempFolder.root, "logs2")

        assertNull(LogsRepository(logsDir).findLogFile())
    }

    @Test
    fun `empty logs directory has no log file`() {
        val logsDir = tempFolder.newFolder("logs2")

        assertNull(LogsRepository(logsDir).findLogFile())
    }

    @Test
    fun `finds the most recently modified file`() {
        val logsDir = tempFolder.newFolder("logs2")
        File(logsDir, "old.log").apply {
            writeText("old entry")
            setLastModified(1_000)
        }
        val newest = File(logsDir, "new.log").apply {
            writeText("newest entry")
            setLastModified(2_000)
        }

        assertEquals(newest, LogsRepository(logsDir).findLogFile())
    }

    @Test
    fun `tail of a file smaller than the chunk size returns everything and no more before`() {
        val file = tempFolder.newFile("app.log")
        file.writeText("first\nsecond\nthird")

        val chunk = LogsRepository(tempFolder.root).readTail(file, maxBytes = 4096)

        assertEquals(listOf("first", "second", "third"), chunk.lines)
        assertFalse(chunk.hasMoreBefore)
        assertEquals(0L, chunk.startOffset)
        assertEquals(file.length(), chunk.endOffset)
    }

    @Test
    fun `tail of an empty file returns no lines`() {
        val file = tempFolder.newFile("app.log")

        val chunk = LogsRepository(tempFolder.root).readTail(file)

        assertTrue(chunk.lines.isEmpty())
        assertFalse(chunk.hasMoreBefore)
    }

    @Test
    fun `paging backwards from the tail reconstructs the whole file without dropping or duplicating lines`() {
        val file = tempFolder.newFile("app.log")
        val originalLines = (1..500).map { "line-$it padded so the file exceeds one small chunk" }
        file.writeText(originalLines.joinToString("\n"))
        val repository = LogsRepository(tempFolder.root)

        // Small chunk size forces several pages, including ones that start mid-line.
        val tail = repository.readTail(file, maxBytes = 500)
        val collected = ArrayDeque(tail.lines)
        var offset = tail.startOffset
        var hasMore = tail.hasMoreBefore
        while (hasMore) {
            val chunk = repository.readBefore(file, offset, maxBytes = 500) ?: break
            chunk.lines.asReversed().forEach { collected.addFirst(it) }
            offset = chunk.startOffset
            hasMore = chunk.hasMoreBefore
        }

        assertEquals(originalLines, collected.toList())
    }

    @Test
    fun `readBefore at offset zero returns null`() {
        val file = tempFolder.newFile("app.log")
        file.writeText("only line")

        assertNull(LogsRepository(tempFolder.root).readBefore(file, offset = 0))
    }

    @Test
    fun `readAfter returns only complete new lines and leaves a partial trailing line unconsumed`() {
        val file = tempFolder.newFile("app.log")
        file.writeText("first\n")
        val repository = LogsRepository(tempFolder.root)
        val tail = repository.readTail(file)

        file.appendText("second\nthird")
        val afterOneComplete = repository.readAfter(file, tail.endOffset)

        assertEquals(listOf("second"), afterOneComplete?.lines)
        assertEquals(tail.endOffset + "second\n".length, afterOneComplete?.endOffset)

        file.appendText(" line\n")
        val afterRemainderCompletes = repository.readAfter(file, afterOneComplete!!.endOffset)

        assertEquals(listOf("third line"), afterRemainderCompletes?.lines)
    }

    @Test
    fun `readAfter returns null when nothing new has landed`() {
        val file = tempFolder.newFile("app.log")
        file.writeText("first\n")
        val repository = LogsRepository(tempFolder.root)
        val tail = repository.readTail(file)

        assertNull(repository.readAfter(file, tail.endOffset))
    }
}
