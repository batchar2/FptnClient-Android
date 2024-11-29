package com.filantrop.pvnclient.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerRepo;

import java.util.List;

public class FptnServerViewModel extends AndroidViewModel {
    private FptnServerRepo fptnServerRepo;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);
        fptnServerRepo = new FptnServerRepo(application);
    }

    public LiveData<List<FptnServer>> getAllServersLiveData() {
        return fptnServerRepo.getAllServersLiveData();
    }
    public void setServers(String urlBase64) {
//        fptnServerRepo.insert(FptnServer());
    }

    public void deleteServers() {
        fptnServerRepo.deleteAll();
    }
}
