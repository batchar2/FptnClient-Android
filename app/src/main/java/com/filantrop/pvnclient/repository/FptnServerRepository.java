package com.filantrop.pvnclient.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.filantrop.pvnclient.database.FptnDatabase;
import com.filantrop.pvnclient.database.model.FptnServerDto;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FptnServerRepository {
    private final FptnDatabase database;
    private final ExecutorService executorService;

    public FptnServerRepository(Context context) {
        this.database = FptnDatabase.getInstance(context.getApplicationContext());
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<FptnServerDto>> getAllServersLiveData() {
        return database.fptnServerDAO().getAllServers();
    }

    public void deleteAll() {
        executorService.execute(() -> database.fptnServerDAO().deleteAll());
    }

    public void insertAll(List<FptnServerDto> serverDtoList) {
        executorService.execute(() -> serverDtoList.forEach(fptnServerDto -> database.fptnServerDAO().insert(fptnServerDto)));
    }
}
