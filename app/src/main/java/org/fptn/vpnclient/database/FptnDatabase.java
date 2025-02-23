package org.fptn.vpnclient.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import org.fptn.vpnclient.database.dao.FptnServerDAO;
import org.fptn.vpnclient.database.model.FptnServerDto;

@Database(entities = {FptnServerDto.class}, version = 9, exportSchema = false)
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
