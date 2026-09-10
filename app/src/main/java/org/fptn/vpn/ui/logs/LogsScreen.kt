package org.fptn.vpn.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.fptn.vpn.R
import org.fptn.vpn.ui.common.BottomNavBar
import org.fptn.vpn.ui.common.LegacyPillButton
import org.fptn.vpn.ui.common.ShareDialog
import org.fptn.vpn.ui.common.legacyDrawableBackground
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.White
import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Compose port of the legacy `LogsActivity` / `logs_layout.xml`: a read-only viewer for the
 * most recently modified file under `getFilesDir()/logs2`, tap-to-copy to the clipboard.
 */
@Composable
fun LogsScreen(
    onNavigateHome: () -> Unit,
    onNavigateSettings: () -> Unit,
) {
    val context = LocalContext.current
    val logsFile = remember { findLatestLogFile(context) }
    val logs = remember { readLogText(logsFile) }
    var showShareDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .legacyDrawableBackground(R.drawable.application_background),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_logo_24),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 30.dp)
                    .size(80.dp),
            )

            Text(
                text = stringResource(R.string.logs),
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp),
            )

            Text(
                text = logs,
                color = White,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { copyLogs(context, logs) }
                    .background(Color(0xFF1A1A1A))
                    .padding(start = 2.dp, top = 8.dp, end = 2.dp, bottom = 8.dp),
            )
        }

        LegacyPillButton(
            text = stringResource(R.string.send_log_file),
            backgroundDrawable = R.drawable.round_back_secondary_100,
            textColor = Primary,
            onClick = { sendLogFile(context, logsFile) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        BottomNavBar(
            isHomeScreen = false,
            isSettingsScreen = false,
            onNavigateHome = onNavigateHome,
            onNavigateSettings = onNavigateSettings,
            onShare = { showShareDialog = true },
        )
    }

    if (showShareDialog) {
        ShareDialog(onDismiss = { showShareDialog = false })
    }
}

private fun copyLogs(context: Context, text: String) {
    if (text.isEmpty()) {
        Toast.makeText(context, R.string.logs_empty, Toast.LENGTH_SHORT).show()
        return
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("logs", text))
    Toast.makeText(context, R.string.logs_copied, Toast.LENGTH_SHORT).show()
}

private fun sendLogFile(context: Context, file: File?) {
    if (file == null) {
        Toast.makeText(context, R.string.logs_empty, Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.send_log_file)))
}

private fun findLatestLogFile(context: Context): File? {
    val logDir = File(context.filesDir, "logs2")
    if (!logDir.exists() || !logDir.isDirectory) {
        return null
    }
    return logDir.listFiles()
        ?.filter { it.isFile && it.canRead() }
        ?.maxByOrNull { it.lastModified() }
}

private fun readLogText(file: File?): String {
    if (file == null) {
        return "No log files."
    }
    val sb = StringBuilder()
    try {
        BufferedReader(FileReader(file)).use { br ->
            var line: String?
            while (br.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        }
    } catch (e: Exception) {
        return "Error reading log: ${e.message}"
    }
    return if (sb.isEmpty()) "Log file is empty." else sb.toString()
}
