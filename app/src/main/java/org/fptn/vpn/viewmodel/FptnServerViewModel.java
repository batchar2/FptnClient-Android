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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.utils.TimeUtils;
import org.fptn.vpn.viewmodel.model.FptnToken;
import org.fptn.vpn.viewmodel.model.FptnTokenServer;
import org.fptn.vpn.viewmodel.model.FptnTokenValidationUtils;
import org.fptn.vpn.vpnclient.exception.ErrorCode;
import org.fptn.vpn.vpnclient.exception.PVNClientException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

public class FptnServerViewModel extends AndroidViewModel {
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

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

    private void resetErrorMessage() {
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

    public void parseAndSaveFptnLink(String fptnTokenString) throws IOException, PVNClientException {
        List<FptnServerDto> serverDtoList = new ArrayList<>();

        // removes all whitespaces and non-visible characters (e.g., tab, \n) and prefixes fptn://  fptn:
        final String clearedtoken = fptnTokenString.replaceAll("\\s+", "")
                .replace("fptn://", "")
                .replace("fptn:", "");
        String decodedToken = new String(Base64.getDecoder().decode(clearedtoken));
        try {
            FptnToken fptnToken = OBJECT_MAPPER.readValue(decodedToken, FptnToken.class);
            FptnTokenValidationUtils.validate(fptnToken);

            String username = fptnToken.getUsername();
            String password = fptnToken.getPassword();
            // add normal servers
            List<FptnTokenServer> servers = fptnToken.getServers();
            if (servers != null && !servers.isEmpty()) {
                for (FptnTokenServer server : servers) {
                    FptnServerDto fptnServerDto = createFptnServerDto(server, username, password, false);
                    serverDtoList.add(fptnServerDto);
                    Log.d(getTag(), "Server from token: " + fptnServerDto.getServerInfo());
                }
            }
            // add censured servers
            List<FptnTokenServer> censoredServers = fptnToken.getCensoredServers();
            if (censoredServers != null && !censoredServers.isEmpty()) {
                for (FptnTokenServer server : censoredServers) {
                    FptnServerDto fptnServerDto = createFptnServerDto(server, username, password, true);
                    serverDtoList.add(fptnServerDto);
                    Log.d(getTag(), "Censured server from token: " + fptnServerDto.getServerInfo());
                }
            }
        } catch (IOException e) {
            Log.e(getTag(), "Can't parse FPTNLink!", e);
            throw e;
        }

        if (serverDtoList.isEmpty()) {
            ErrorCode errorCode = ErrorCode.SERVER_LIST_NULL_OR_EMPTY;
            String errorMessage = Optional.ofNullable(getStringResourceByName(getApplication(), errorCode.getValue()))
                    .orElse(errorCode.getValue());
            throw new PVNClientException(errorCode, errorMessage);
        }

        fptnServerRepository.deleteAll(); // delete all old servers
        fptnServerRepository.insertAll(serverDtoList); // add new servers
    }

    @NonNull
    private static FptnServerDto createFptnServerDto(FptnTokenServer server, String username, String password, boolean censured) {
        String countryCode = Optional.ofNullable(server.getCountryCode())
                .orElse(CountryFlags.getCountryCodeFromHostName(server.getName()));
        return new FptnServerDto(
                0,
                false,
                server.getName(),
                username,
                password,
                server.getHost(),
                server.getPort(),
                countryCode,
                server.getMd5Fingerprint(),
                censured);
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

    private String getTag() {
        return this.getClass().getCanonicalName();
    }

    public void updateSpeed(String downloadSpeed, String uploadSpeed) {
        downloadSpeedAsStringLiveData.postValue(downloadSpeed);
        uploadSpeedAsStringLiveData.postValue(uploadSpeed);
    }
}
