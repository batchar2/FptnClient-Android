package com.filantrop.pvnclient.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.filantrop.pvnclient.database.FptnDatabase;
import com.filantrop.pvnclient.database.dao.FptnServerDAO;
import com.filantrop.pvnclient.database.model.FptnServer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class FptnServerRepo {
    private FptnServerDAO fptnServerDAO;
    private LiveData<List<FptnServer>> allServersLiveData;
    private Executor executor = Executors.newSingleThreadExecutor();

    public FptnServerRepo(Application application) {
        fptnServerDAO = FptnDatabase.getInstance(application).fptnServerDAO();
    }

    public LiveData<List<FptnServer>> getAllServersLiveData() {
        return allServersLiveData;
    }

    public void insert(FptnServer fptnServer) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                fptnServerDAO.insert(fptnServer);
            }
        });
    }

    public void deleteAll() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                fptnServerDAO.deleteAll();
            }
        });
    }

}
