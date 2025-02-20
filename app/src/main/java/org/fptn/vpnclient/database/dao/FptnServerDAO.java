package org.fptn.vpnclient.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import org.fptn.vpnclient.database.model.FptnServerDto;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

@Dao
public interface FptnServerDAO {
    @Insert
    void insert(FptnServerDto server);

    @Query("SELECT * FROM server_table")
    LiveData<List<FptnServerDto>> getAllServersLiveData();

    @Query("SELECT * FROM server_table")
    ListenableFuture<List<FptnServerDto>> getAllServersListFuture();

    @Query("UPDATE server_table SET isSelected = 1 WHERE id = :id")
    ListenableFuture<Integer> setIsSelected(int id);

    @Query("SELECT * FROM server_table WHERE isSelected = 1")
    ListenableFuture<FptnServerDto> getSelected();

    @Query("UPDATE server_table SET isSelected = 0 WHERE 1")
    ListenableFuture<Integer> resetSelected();

    @Query("DELETE FROM server_table")
    void deleteAll();
}
