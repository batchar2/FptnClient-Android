package com.filantrop.pvnclient.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerRepo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Base64;
import java.util.List;

public class FptnServerViewModel extends AndroidViewModel {
    private final String TAG = FptnServerViewModel.class.getName();
    private final FptnServerRepo fptnServerRepo;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);
        fptnServerRepo = new FptnServerRepo(application);
    }

    public LiveData<List<FptnServer>> getAllServersLiveData() {
        return fptnServerRepo.getAllServersLiveData();
    }

    public boolean parseAndSaveFptnLink(String url) {
        if (url.startsWith("fptn://")) {
            String preparedUrl = url.substring(7);  // Remove first 7 characters
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(preparedUrl);
                String jsonString = new String(decodedBytes);

                FptnServerRepo fptnServerRepo = new FptnServerRepo(getApplication());
                JSONObject jsonObject = new JSONObject(jsonString);
                String username = jsonObject.getString("username");
                String password = jsonObject.getString("password");
                JSONArray serversArray = jsonObject.getJSONArray("servers");
                for (int i = 0; i < serversArray.length(); i++) {
                    JSONObject serverObject = serversArray.getJSONObject(i);
                    String name = serverObject.getString("name");
                    String host = serverObject.getString("host");
                    int port = serverObject.getInt("port");
                    fptnServerRepo.insert(new FptnServer(name, username, password, host, port));
                    Log.i(TAG, "=== SERVER: " + username + " " + password + " " + host + ":" + port);
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void deleteServers() {
        fptnServerRepo.deleteAll();
    }
}
