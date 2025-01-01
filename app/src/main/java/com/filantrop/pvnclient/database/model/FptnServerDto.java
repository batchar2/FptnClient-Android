package com.filantrop.pvnclient.database.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity(tableName = "server_table")
@Data
public class FptnServerDto implements Serializable {

    public static final FptnServerDto AUTO = new FptnServerDto("Auto", "Auto", "Auto", "", 0);

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;

    private String username;
    private String password;
    private String host;
    private Integer port;

    public FptnServerDto(String name, String username, String password, String host, Integer port) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

}
