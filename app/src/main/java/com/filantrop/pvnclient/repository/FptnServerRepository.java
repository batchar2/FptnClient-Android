package com.filantrop.pvnclient.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.filantrop.pvnclient.database.FptnDatabase;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.google.common.util.concurrent.ListenableFuture;

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

    public ListenableFuture<List<FptnServerDto>> getAllServersListFuture() {
        return database.fptnServerDAO().getAllServersListFuture();
    }

    public LiveData<List<FptnServerDto>> getAllServersLiveData() {
        return database.fptnServerDAO().getAllServersLiveData();
    }

    public void deleteAll() {
        executorService.execute(() -> database.fptnServerDAO().deleteAll());
    }

    public void insertAll(List<FptnServerDto> serverDtoList) {
        executorService.execute(() -> serverDtoList.forEach(fptnServerDto -> database.fptnServerDAO().insert(fptnServerDto)));
    }

    public ListenableFuture<Integer>  resetSelected() {
        return database.fptnServerDAO().resetSelected();
    }

    public ListenableFuture<Integer> setIsSelected(int id) {
        return database.fptnServerDAO().setIsSelected(id);
    }

    public ListenableFuture<FptnServerDto> getSelected() {
        return database.fptnServerDAO().getSelected();
    }
}
