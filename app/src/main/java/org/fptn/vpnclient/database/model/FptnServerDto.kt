package org.fptn.vpnclient.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.fptn.vpnclient.core.common.Constants.SELECTED_SERVER_ID_AUTO

@Entity(tableName = "server_table")
data class FptnServerDto(
    @JvmField
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @JvmField
    val isSelected: Boolean,
    @JvmField val name: String,
    @JvmField val username: String,
    @JvmField val password: String,
    @JvmField val host: String,
    @JvmField val port: Int,
) {
    val serverInfo: String
        get() = "$name ($host)"

    companion object {
        @JvmField
        val AUTO: FptnServerDto =
            FptnServerDto(SELECTED_SERVER_ID_AUTO, false, "Auto", "Auto", "Auto", "", 0)
    }
}
