package com.filantrop.pvnclient.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.repository.FptnServerRepository;
import com.filantrop.pvnclient.utils.DataRateCalculator;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class FptnServerViewModel extends AndroidViewModel {
    private final static String TAG = FptnServerViewModel.class.getName();

    private final FptnServerRepository fptnServerRepository;

    @Getter
    private MutableLiveData<ConnectionState> connectionStateMutableLiveData = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    @Getter
    private MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    @Getter
    private MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());

    @Getter
    @Setter
    private ConnectionState connectionState;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);

        fptnServerRepository = new FptnServerRepository(getApplication());
    }

    public ListenableFuture<List<FptnServerDto>> getAllServers() {
        return fptnServerRepository.getAllServersListFuture();
    }

    public boolean parseAndSaveFptnLink(String url) {
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
                fptnServerRepository.insertAll(serverDtoList);
                return true;
            }

            Log.e(TAG, "Servers from fptnLink is empty!");
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse fptnLink!", e);
        }
        return false;
    }

}
