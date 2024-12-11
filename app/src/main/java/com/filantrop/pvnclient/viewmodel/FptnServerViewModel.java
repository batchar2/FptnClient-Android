package com.filantrop.pvnclient.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.repository.FptnServerRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class FptnServerViewModel extends AndroidViewModel {
    private final String TAG = FptnServerViewModel.class.getName();

    @Getter
    @Setter
    private ConnectionState connectionState;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<List<FptnServerDto>> getAllServersLiveData() {
        return new FptnServerRepository(getApplication()).getAllServersLiveData();
    }

    public boolean parseAndSaveFptnLink(String url) {
        if (url.startsWith("fptn://")) {
            String preparedUrl = url.substring(7);  // Remove first 7 characters
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(preparedUrl);
                String jsonString = new String(decodedBytes);
                JSONObject jsonObject = new JSONObject(jsonString);
                String username = jsonObject.getString("username");
                String password = jsonObject.getString("password");

                List<FptnServerDto> serverDtoList = new ArrayList<>();
                JSONArray serversArray = jsonObject.getJSONArray("servers");
                for (int i = 0; i < serversArray.length(); i++) {
                    JSONObject serverObject = serversArray.getJSONObject(i);
                    String name = serverObject.getString("name");
                    String host = serverObject.getString("host");
                    int port = serverObject.getInt("port");

                    serverDtoList.add(new FptnServerDto(name, username, password, host, port));
                    Log.i(TAG, "=== SERVER: " + username + " " + password + " " + host + ":" + port);
                }

                if (!serverDtoList.isEmpty()) {
                    FptnServerRepository fptnServerRepository = new FptnServerRepository(getApplication());
                    fptnServerRepository.insertAll(serverDtoList);
                }

                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

}
