package org.fptn.vpn.viewmodel;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;
import org.fptn.vpn.database.model.FptnServerDto;
import org.fptn.vpn.enums.ConnectionState;
import org.fptn.vpn.repository.FptnServerRepository;
import org.fptn.vpn.utils.CountryFlags;
import org.fptn.vpn.utils.DataRateCalculator;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.utils.TimeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

public class FptnServerViewModel extends AndroidViewModel {
    private final static String TAG = FptnServerViewModel.class.getName();

    private final FptnServerRepository fptnServerRepository;

    @Getter
    private final MutableLiveData<Pair<ConnectionState, Instant>> connectionStateMutableLiveData = new MutableLiveData<>(Pair.create(ConnectionState.DISCONNECTED, Instant.now()));
    @Getter
    private final MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    @Getter
    private final MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(new DataRateCalculator(1000).getFormatString());
    @Getter
    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");
    @Getter
    private final MutableLiveData<String> timerTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_time));
    @Getter
    private final MutableLiveData<String> currentSNI = new MutableLiveData<>(getApplication().getString(R.string.default_sni));
    @Getter
    private final LiveData<List<FptnServerDto>> serverDtoListLiveData;

    private final Observer<Pair<ConnectionState, Instant>> connectionStateObserver;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);

        fptnServerRepository = new FptnServerRepository(getApplication());
        serverDtoListLiveData = fptnServerRepository.getAllServersLiveData();

        connectionStateObserver = connectionStateInstantPair -> {
            if (connectionStateInstantPair.first == ConnectionState.CONNECTED) {
                startTimer(connectionStateInstantPair.second);
            } else if (connectionStateInstantPair.first == ConnectionState.DISCONNECTED) {
                stopTimer();
            }
        };
        connectionStateMutableLiveData.observeForever(connectionStateObserver);

        currentSNI.postValue(getSavedSNI());
    }

    @NonNull
    private String getSavedSNI() {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.CURRENT_SNI_SHARED_PREF_KEY, getApplication().getString(R.string.default_sni));
    }

    public ListenableFuture<List<FptnServerDto>> getAllServers() {
        return fptnServerRepository.getAllServersListFuture();
    }

    public void deleteAll() {
        fptnServerRepository.deleteAll();
    }

    public boolean parseAndSaveFptnLink(String url) {
        // removes all whitespaces and non-visible characters (e.g., tab, \n) and prefixes fptn://  fptn:
        final String preparedUrl = url.replaceAll("\\s+", "")
                .replace("fptn://", "")
                .replace("fptn:", "");
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
                String hostname = serverObject.getString("name");
                String host = serverObject.getString("host");
                int port = serverObject.getInt("port");

                String countryCode = getCountryCodeFromJson(serverObject, hostname);
                serverDtoList.add(new FptnServerDto(0, false, hostname, username, password, host, port, countryCode));
            }
            if (!serverDtoList.isEmpty()) {
                deleteAll(); // delete old
                fptnServerRepository.insertAll(serverDtoList);
                return true;
            }
            Log.e(TAG, "Servers from fptnLink is empty!");
        } catch (JSONException e) {
            Log.e(TAG, "Can't parse fptnLink!", e);
        } catch (Exception e) {
            Log.e(TAG, "Undefined error:", e);
        }
        return false;
    }

    private String getCountryCodeFromJson(JSONObject serverObject, String hostname) {
        try {
            return serverObject.getString("country_code");
        } catch (JSONException ignored) {
            Log.w(TAG, "CountryCode not found!");
        }
        return CountryFlags.getCountryCodeFromHostName(hostname);
    }

    public void clearErrorTextMessage() {
        errorTextLiveData.setValue("");
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        connectionStateMutableLiveData.removeObserver(connectionStateObserver);
        stopTimer();
    }

    private void startTimer(Instant connectedFrom) {
        Log.d(TAG, "FptnServerViewModel.startTimer: " + connectedFrom);
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        scheduledFuture = scheduler.scheduleWithFixedDelay(() -> {
            Instant now = Instant.now();
            long durationInSeconds = Duration.between(connectedFrom, now).getSeconds();

            String time = TimeUtils.getTime(durationInSeconds);
            Log.d(TAG, "FptnServerViewModel.time: " + time);
            timerTextLiveData.postValue(time);
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopTimer() {
        Log.d(TAG, "FptnServerViewModel.stopTimer()");
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        timerTextLiveData.postValue(getApplication().getString(R.string.zero_time));
    }

    public void updateSNI(String newSni) {
        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(Constants.CURRENT_SNI_SHARED_PREF_KEY, newSni).apply();
        currentSNI.postValue(newSni);
    }
}
