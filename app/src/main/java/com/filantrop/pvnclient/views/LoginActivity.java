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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServer;
import com.filantrop.pvnclient.repository.FptnServerRepo;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

//
public class LoginActivity extends AppCompatActivity {

    private final String TAG = "LoginActivity";

    private FptnServerViewModel fptnViewModel;

    private FptnServerRepo fptnServerRepo = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
    }

    public void onLogin(View v) {
        final EditText linkInput = findViewById(R.id.fptn_link_input);
        final String fptnLink = linkInput.getText().toString();
        fptnViewModel.parseAndSaveFptnLink(fptnLink);
    }

    private void intializeVariable() {
//        recyclerView = findViewById(R.id.rec)
    }
}
