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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

sealed interface LogsUiState {
    data object Loading : LogsUiState
    data object NoLogsDirectory : LogsUiState
    data object NoLogFiles : LogsUiState
    data class ReadError(val message: String?) : LogsUiState
    data class Content(
        val file: File,
        val lines: List<String>,
        val hasMoreBefore: Boolean,
        val loadingOlder: Boolean = false,
    ) : LogsUiState
}

/**
 * Pages the log file the way `kubectl logs` does: the newest lines load first, scrolling up
 * fetches further history (`readBefore`), and scrolling back down to the end picks up whatever
 * was appended since (`readAfter`). [LogsRepository] does the actual file I/O; this class only
 * tracks the byte offsets of what's currently loaded and re-publishes the growing line list.
 */
class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LogsRepository(File(application.filesDir, "logs2"))

    val uiStateLiveData = MutableLiveData<LogsUiState>(LogsUiState.Loading)

    private var topOffset = 0L
    private var bottomOffset = 0L
    private var loadingNewer = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val file = repository.findLogFile()
            if (file == null) {
                val hasDirectory = File(application.filesDir, "logs2").isDirectory
                uiStateLiveData.postValue(if (hasDirectory) LogsUiState.NoLogFiles else LogsUiState.NoLogsDirectory)
                return@launch
            }
            try {
                val chunk = repository.readTail(file)
                topOffset = chunk.startOffset
                bottomOffset = chunk.endOffset
                uiStateLiveData.postValue(
                    LogsUiState.Content(file = file, lines = chunk.lines, hasMoreBefore = chunk.hasMoreBefore),
                )
            } catch (e: IOException) {
                uiStateLiveData.postValue(LogsUiState.ReadError(e.message))
            }
        }
    }

    /** Safe to call on every "scrolled near the top" tick — no-ops while a fetch is in flight or nothing older remains. */
    fun loadOlder() {
        val current = uiStateLiveData.value as? LogsUiState.Content ?: return
        if (current.loadingOlder || !current.hasMoreBefore) return
        uiStateLiveData.value = current.copy(loadingOlder = true)
        viewModelScope.launch(Dispatchers.IO) {
            val chunk = try {
                repository.readBefore(current.file, topOffset)
            } catch (e: IOException) {
                null
            }
            val latest = uiStateLiveData.value as? LogsUiState.Content ?: return@launch
            if (chunk != null) {
                topOffset = chunk.startOffset
                uiStateLiveData.postValue(
                    latest.copy(lines = chunk.lines + latest.lines, hasMoreBefore = chunk.hasMoreBefore, loadingOlder = false),
                )
            } else {
                uiStateLiveData.postValue(latest.copy(loadingOlder = false))
            }
        }
    }

    /** Safe to call on every "scrolled to the bottom" tick — no-ops while a fetch is in flight or nothing new has landed. */
    fun loadNewer() {
        if (loadingNewer) return
        val current = uiStateLiveData.value as? LogsUiState.Content ?: return
        loadingNewer = true
        viewModelScope.launch(Dispatchers.IO) {
            val chunk = try {
                repository.readAfter(current.file, bottomOffset)
            } catch (e: IOException) {
                null
            }
            loadingNewer = false
            if (chunk == null) return@launch
            bottomOffset = chunk.endOffset
            val latest = uiStateLiveData.value as? LogsUiState.Content ?: return@launch
            uiStateLiveData.postValue(latest.copy(lines = latest.lines + chunk.lines))
        }
    }
}
