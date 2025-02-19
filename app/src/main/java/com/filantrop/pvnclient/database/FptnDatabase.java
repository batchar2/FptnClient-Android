package com.filantrop.pvnclient.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.filantrop.pvnclient.database.dao.FptnServerDAO;
import com.filantrop.pvnclient.database.model.FptnServerDto;

@Database(entities = {FptnServerDto.class}, version = 8, exportSchema = false)
public abstract class FptnDatabase extends RoomDatabase {
    public abstract FptnServerDAO fptnServerDAO();

    private static FptnDatabase instance;

    public static synchronized FptnDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), FptnDatabase.class, "FptnDatabase")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

}
