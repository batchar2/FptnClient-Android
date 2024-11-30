package com.filantrop.pvnclient.database.model;


import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "fptn_server_table")
public class FptnServer implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;

    private String username;
    private String password;
    private String host;
    private Integer port;

    public FptnServer(String name, String username, String password, String host, Integer port) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.host = host;
        this.port = port;
    }

    protected FptnServer(Parcel in) {
        id = in.readInt();
        username = in.readString();
        password = in.readString();
        host = in.readString();
        if (in.readByte() == 0) {
            port = null;
        } else {
            port = in.readInt();
        }
    }

    public static final Creator<FptnServer> CREATOR = new Creator<FptnServer>() {
        @Override
        public FptnServer createFromParcel(Parcel in) {
            return new FptnServer(in);
        }

        @Override
        public FptnServer[] newArray(int size) {
            return new FptnServer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int flags) {
        parcel.writeString(name);
        parcel.writeString(username);
        parcel.writeString(password);
        parcel.writeString(host);
        parcel.writeInt(port);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }
}
