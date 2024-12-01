//package com.filantrop.pvnclient.wtmp.model;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.VpnService;
//import android.os.Bundle;
//import android.widget.EditText;
//
//import com.filantrop.pvnclient.R;
//import com.filantrop.pvnclient.services.CustomVpnService;
//
//public class MainActivityOld extends Activity {
//    public interface Prefs {
//        String NAME = "connection";
//        String SERVER_ADDRESS = "server.address";
//        String SERVER_PORT = "server.port";
//        String USERNAME = "username";
//        String PASSWORD = "password";
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.form);
//
//        // Обращение к UI - наверное можно заменить на DI
//        final EditText serverAddress = findViewById(R.id.serverAddressEditText);
//        final EditText serverPort = findViewById(R.id.serverPortEditText);
//        final EditText username = findViewById(R.id.usernameEditText);
//        final EditText password = findViewById(R.id.passwordEditText);
//
//        // Разделяемые настройки - доступны по идее из любого места приложения
//        final SharedPreferences prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);
//
//        serverAddress.setText(prefs.getString(Prefs.SERVER_ADDRESS, "185.215.187.165"));
//        username.setText(prefs.getString(Prefs.USERNAME, ""));
//        password.setText(prefs.getString(Prefs.PASSWORD, ""));
//
//        int serverPortPrefValue = prefs.getInt(Prefs.SERVER_PORT, 443);
//        serverPort.setText(String.valueOf(serverPortPrefValue == 0 ? "" : serverPortPrefValue));
//
//        findViewById(R.id.connect).setOnClickListener(v -> {
//            int serverPortNum;
//            try {
//                serverPortNum = Integer.parseInt(serverPort.getText().toString());
//            } catch (NumberFormatException e) {
//                serverPortNum = 0;
//            }
//
//            prefs.edit()
//                    .putString(Prefs.SERVER_ADDRESS, serverAddress.getText().toString())
//                    .putInt(Prefs.SERVER_PORT, serverPortNum)
//                    .putString(Prefs.USERNAME, username.getText().toString())
//                    .putString(Prefs.PASSWORD, password.getText().toString())
//                    .apply();
//            // Запрос на предоставление приложению возможности запускать впн Intent != null, если приложению еще не предоставлялся доступ
//            Intent intent = VpnService.prepare(MainActivityOld.this);
//            if (intent != null) {
//                // Запрос на предоставление приложению возможности запускать впн
//                startActivityForResult(intent, 0);
//            } else {
//                // Запрос был ранее предоставлен - сразу вызов onActivityResult с RESULT_OK
//                onActivityResult(0, RESULT_OK, null);
//            }
//        });
//        findViewById(R.id.disconnect).setOnClickListener(v -> {
//            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
//        });
//    }
//
//    @Override
//    protected void onActivityResult(int request, int result, Intent data) {
//        if (result == RESULT_OK) {
//            startService(getServiceIntent().setAction(CustomVpnService.ACTION_CONNECT));
//        }
//    }
//
//    private Intent getServiceIntent() {
//        return new Intent(this, CustomVpnService.class);
//    }
//}
