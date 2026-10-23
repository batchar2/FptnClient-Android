package org.fptn.vpn.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import org.fptn.vpn.R
import org.fptn.vpn.database.entity.ServerEntity
import androidx.compose.foundation.layout.height

@Composable
fun ServerDropdown(
    servers: List<ServerEntity>,
    selected: ServerEntity,
    onSelect: (ServerEntity) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Close menu instantly (no animation) when enabled becomes false
    LaunchedEffect(enabled) {
        if (!enabled) {
            expanded = false
        }
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { anchorWidthPx = it.size.width }
                .legacyDrawableBackground(
                    if (expanded) R.drawable.spinner_background_up else R.drawable.spinner_background_down,
                )
                .clickable(enabled = enabled) { expanded = true },
        ) {
            val selectedPing = servers.firstOrNull { it.id == selected.id }?.pingMs
                ?: selected.pingMs
            ServerRow(server = selected, pingValue = selectedPing)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Remove the closing animation so the menu disappears instantly.
            // Approximately 60dp per server (flag + name + ping row + padding), 6 servers + 16dp vertical padding.
            modifier = Modifier
                .width(with(density) { anchorWidthPx.toDp() })
                .heightIn(max = 400.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
        ) {
            // The popup opens in a separate window with its own composition.
            // LocalDensity from MainActivity (with our custom fontScale override) is NOT passed
            // to the popup automatically. Therefore, we explicitly provide the density captured
            // above (which contains our custom fontScale) to the popup content.
            CompositionLocalProvider(LocalDensity provides density) {
                servers.forEach { server ->
                    key(server.id, server.pingMs) {
                        DropdownMenuItem(
                            text = { ServerRow(server = server, pingValue = server.pingMs) },
                            onClick = {
                                onSelect(server)
                                expanded = false
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 2.dp, horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}