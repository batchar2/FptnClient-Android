package com.filantrop.pvnclient.wtmp.model

import androidx.room.Entity

@Entity(tableName = "service_table")
data class Service(
    val version: Int,
    val username: String,
    val password: String,
    val servers: List<Server>
)
