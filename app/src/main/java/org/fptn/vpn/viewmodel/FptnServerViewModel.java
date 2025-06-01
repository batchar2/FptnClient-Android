package org.fptn.vpn.viewmodel;

import static org.fptn.vpn.utils.ResourcesUtils.getStringResourceByName;

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

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.utils.TimeUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;
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
    private final FptnServerRepository fptnServerRepository;

    @Getter
    private final MutableLiveData<Pair<ConnectionState, Instant>> connectionStateMutableLiveData = new MutableLiveData<>(Pair.create(ConnectionState.DISCONNECTED, Instant.now()));
    private final Observer<Pair<ConnectionState, Instant>> connectionStateObserver;

    @Getter
    private final MutableLiveData<PVNClientException> lastExceptionLiveData = new MutableLiveData<>();
    private final Observer<PVNClientException> lastExceptionObserver;

    @Getter
    private final MutableLiveData<Boolean> activeStateLiveData = new MutableLiveData<>(false);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledFuture;

    /*Only for show on views*/
    @Getter
    private final MutableLiveData<String> currentSNI = new MutableLiveData<>(getApplication().getString(R.string.default_sni));
    @Getter
    private final MutableLiveData<String> timerTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_time));
    @Getter
    private final MutableLiveData<String> downloadSpeedAsStringLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_speed));
    @Getter
    private final MutableLiveData<String> uploadSpeedAsStringLiveData = new MutableLiveData<>(getApplication().getString(R.string.zero_speed));
    @Getter
    private final MutableLiveData<String> errorTextLiveData = new MutableLiveData<>("");
    @Getter
    private final MutableLiveData<String> statusTextLiveData = new MutableLiveData<>(getApplication().getString(R.string.disconnected));
    @Getter
    private final LiveData<List<FptnServerDto>> serverDtoListLiveData;

    public FptnServerViewModel(@NonNull Application application) {
        super(application);

        fptnServerRepository = new FptnServerRepository(getApplication());
        serverDtoListLiveData = fptnServerRepository.getAllServersLiveData();

        connectionStateObserver = connectionStateInstantPair -> {
            ConnectionState connectionState = connectionStateInstantPair.first;
            switch (connectionState) {
                case DISCONNECTED -> {
                    stopTimer();
                    activeStateLiveData.postValue(false);
                    statusTextLiveData.postValue(getApplication().getString(R.string.disconnected));
                }
                case CONNECTING -> {
                    activeStateLiveData.postValue(true);
                    statusTextLiveData.postValue(getApplication().getString(R.string.connecting));
                    resetErrorMessage();
                }
                case CONNECTED -> {
                    startTimer(connectionStateInstantPair.second);
                    activeStateLiveData.postValue(true);
                    resetErrorMessage();
                }
                case RECONNECTING -> {
                    activeStateLiveData.postValue(true);
                }
            }
        };
        connectionStateMutableLiveData.observeForever(connectionStateObserver);

        currentSNI.postValue(getSavedSNI());


        lastExceptionObserver = exception -> {
            if (exception != null) {
                ErrorCode errorCode = exception.errorCode;
                if (errorCode != ErrorCode.UNKNOWN_ERROR) {
                    String stringResourceByName = getStringResourceByName(getApplication(), errorCode.getValue());
                    Log.e(getTag(), "Error as text: " + stringResourceByName);

                    setErrorMessage(stringResourceByName);
                    if (ErrorCode.Companion.isNeedToOfferRefreshToken(errorCode)) {
                        // todo: need to add snackbar
                        /*
                        Snackbar snackbar = Snackbar.make(findViewById(R.id.layout), stringResourceByName, 8000);
                    if (ErrorCode.Companion.isNeedToOfferRefreshToken(errorCode)) {
                        snackbar.setAction(getString(R.string.refresh_token), v -> {
                            Intent browserIntent = new
                                    Intent(Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.telegram_bot_link)));
                            startActivity(browserIntent);
                        });
                    }
                    snackbar.show();
                    */
                    }
                } else {
                    setErrorMessage(exception.errorMessage);
                }
            }

        };
        lastExceptionLiveData.observeForever(lastExceptionObserver);
    }

    public void setErrorMessage(String errorMessage) {
        errorTextLiveData.postValue(errorMessage);
    }

    private void resetErrorMessage(){
        setErrorMessage("");
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

    public boolean parseAndSaveFptnLink(String fptnToken) {
        // removes all whitespaces and non-visible characters (e.g., tab, \n) and prefixes fptn://  fptn:
        final String preparedUrl = fptnToken.replaceAll("\\s+", "")
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
                String md5ServerFingerprint = serverObject.getString("md5_fingerprint");
                String countryCode = getCountryCodeFromJson(serverObject, hostname);

                serverDtoList.add(new FptnServerDto(0, false, hostname, username, password, host, port, countryCode, md5ServerFingerprint, false));
            }

            JSONArray censoredZoneServerArray = jsonObject.getJSONArray("censored_zone_servers");
            for (int i = 0; i < censoredZoneServerArray.length(); i++) {
                JSONObject serverObject = censoredZoneServerArray.getJSONObject(i);
                String hostname = serverObject.getString("name");
                String host = serverObject.getString("host");
                int port = serverObject.getInt("port");
                String md5ServerFingerprint = serverObject.getString("md5_fingerprint");
                String countryCode = getCountryCodeFromJson(serverObject, hostname);

                serverDtoList.add(new FptnServerDto(0, false, hostname, username, password, host, port, countryCode, md5ServerFingerprint, true));
            }

            if (!serverDtoList.isEmpty()) {
                deleteAll(); // delete old
                fptnServerRepository.insertAll(serverDtoList);
                return true;
            }
            Log.e(getTag(), "Servers from fptnLink is empty!");
        } catch (JSONException e) {
            Log.e(getTag(), "Can't parse fptnLink!", e);
        } catch (Exception e) {
            Log.e(getTag(), "Undefined error:", e);
        }
        return false;
    }

    private String getCountryCodeFromJson(JSONObject serverObject, String hostname) {
        try {
            return serverObject.getString("country_code");
        } catch (JSONException ignored) {
            Log.w(getTag(), "CountryCode not found!");
        }
        return CountryFlags.getCountryCodeFromHostName(hostname);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        connectionStateMutableLiveData.removeObserver(connectionStateObserver);
        lastExceptionLiveData.removeObserver(lastExceptionObserver);

        stopTimer();
    }

    private void startTimer(Instant connectedFrom) {
        Log.d(getTag(), "FptnServerViewModel.startTimer: " + connectedFrom);
        if (scheduledFuture != null && !scheduledFuture.isCancelled()) {
            scheduledFuture.cancel(true);
            scheduledFuture = null;
        }
        scheduledFuture = scheduler.scheduleWithFixedDelay(() -> {
            Instant now = Instant.now();
            long durationInSeconds = Duration.between(connectedFrom, now).getSeconds();

            String time = TimeUtils.getTime(durationInSeconds);
            Log.d(getTag(), "FptnServerViewModel.time: " + time);
            timerTextLiveData.postValue(time);
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void stopTimer() {
        Log.d(getTag(), "FptnServerViewModel.stopTimer()");
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

    public void updateStatusText(String text) {
        statusTextLiveData.postValue(text);
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    public void updateSpeed(String downloadSpeed, String uploadSpeed) {
        downloadSpeedAsStringLiveData.postValue(downloadSpeed);
        uploadSpeedAsStringLiveData.postValue(uploadSpeed);
    }
}
