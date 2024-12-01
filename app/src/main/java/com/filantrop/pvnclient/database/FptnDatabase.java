package com.filantrop.pvnclient.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.filantrop.pvnclient.database.dao.FptnServerDAO;
import com.filantrop.pvnclient.database.model.FptnServer;

@Database(entities = {FptnServer.class}, version = 6, exportSchema = false)
public abstract class FptnDatabase extends RoomDatabase {
    public abstract FptnServerDAO fptnServerDAO();

    private static FptnDatabase instance;

    public static synchronized FptnDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), FptnDatabase.class, "FptnDatabase")
                    .fallbackToDestructiveMigration()
                    .addCallback(fptnCallBack)
                    .build();
        }
        return instance;
    }

    private static RoomDatabase.Callback fptnCallBack = new RoomDatabase.Callback(){
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            // new PopulateDb(instance).execute();
        }
    };
}
