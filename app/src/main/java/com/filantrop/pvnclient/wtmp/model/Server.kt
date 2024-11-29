package com.filantrop.pvnclient.wtmp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_table")
data class Server(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val Host: String,
    val Password: String,
    val port: Int
)
