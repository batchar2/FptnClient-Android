package com.filantrop.pvnclient.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.filantrop.pvnclient.database.model.FptnServerDto;

import java.util.List;

@Dao
public interface FptnServerDAO {
    @Insert
    void insert(FptnServerDto server);

    @Query("SELECT * FROM server_table")
    LiveData<List<FptnServerDto>> getAllServers();

    @Query("DELETE FROM server_table")
    void deleteAll();
}
