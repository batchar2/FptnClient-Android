package com.filantrop.pvnclient.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.enums.ConnectionState;
import com.filantrop.pvnclient.repository.FptnServerRepository;
import com.filantrop.pvnclient.utils.CountUpTimer;
import com.filantrop.pvnclient.utils.DataRateCalculator;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import lombok.Getter;

public class FptnServerViewModel extends AndroidViewModel {
    private final static String TAG = FptnServerViewModel.class.getName();

    private final FptnServerRepository fptnServerRepository;

    @Getter
    private final MutableLiveData<ConnectionState> connectionStateMutableLiveData = new MutableLiveData<>(ConnectionState.DISCONNECTED);
    @Getter
    private final MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    @Getter
    private final MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    @Getter
    private final MutableLiveData<String> timerTextLiveData = new MutableLiveData<>("00:00:00");
    @Getter
    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");

    private CountUpTimer timer;

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

    public void clearErrorTextMessage(){
        errorTextLiveData.setValue("");
    }


    public void startTimer() {
        if (timer == null) {
            timer = new CountUpTimer() {
                @Override
                public void onTick(int second) {
                    int hours = second / 3600;
                    int minutes = (second % 3600) / 60;
                    int seconds = second % 60;

                    // Format and display the time as HH:MM:SS
                    String time = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
                    timerTextLiveData.postValue(time);
                }

                @Override
                public void onFinish() {
                    timerTextLiveData.postValue("00:00:00");
                }
            };
            timer.start();
        }
    }

    public void stopTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = null;
    }

}
