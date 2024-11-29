package com.filantrop.pvnclient.views;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerRepo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

//
public class LoginActivity extends AppCompatActivity {

    private final String TAG = "LoginActivity";

//    private FptnServerRepo fptnServerRepo = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

//        fptnServerRepo = new FptnServerRepo(getApplication());
    }

    public void onLogin(View v) {
        final EditText linkInput = findViewById(R.id.fptn_link_input);
//        String fptnUrlBase64 = linkInput.getText().toString();
        String fptnLinkBase64 = linkInput.getText().toString();

        if (!fptnLinkBase64.isEmpty() && fptnLinkBase64.startsWith("fptn://")) {
            fptnLinkBase64 = fptnLinkBase64.substring(7);  // Remove first 7 characters

            byte[] decodedBytes = Base64.getDecoder().decode(fptnLinkBase64);
            String jsonString = new String(decodedBytes);

//            List<FptnServer> serverList = new ArrayList<>();

            FptnServerRepo fptnServerRepo = new FptnServerRepo(getApplication());

            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                String username = jsonObject.getString("username");
                String password = jsonObject.getString("password");
                JSONArray serversArray = jsonObject.getJSONArray("servers");
                for (int i = 0; i < serversArray.length(); i++) {
                    JSONObject serverObject = serversArray.getJSONObject(i);
                    String name = serverObject.getString("name");
                    String host = serverObject.getString("host");
                    int port = serverObject.getInt("port");

                    FptnServer server = new FptnServer(name, username, password, host, port);
//
                    Log.i(TAG, "=== SERVER: " + username + " " + password + " " + host + ":" + port);
                    fptnServerRepo.insert(server);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
