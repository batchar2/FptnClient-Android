package com.filantrop.pvnclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.EditText;

/*

1) Авторизация
Перед началом надо авторизоваться, вернется jwt токен
https://github.com/batchar2/fptn/blob/android/src/android/app/src/main/java/org/fptn/client/service/websocket/WebSocketClient.java#L58

этот токен нужно прикрепить к http заголовку для вебсокета и фейковый IP адрес клиента (этот же IP-адрес надо указать для TUN интерфейса), просто захардкодь этот адрес и не заморачивайся
https://github.com/batchar2/fptn/blob/android/src/android/app/src/main/java/org/fptn/client/service/websocket/WebSocketClient.java#L104

2) Пакеты в протобафе выпустить на устройство
Сервер будет отправлять IP пакеты в протобафе через вебсокет. Их нужно достать и "выпустить" через тун в сеть устройства

От клиента на сервер IP пакеты нужно запаковать в протобаф и через вебсокет отправить

*/

public class MainActivity extends Activity {
    public interface Prefs {
        String NAME = "connection";
        String SERVER_ADDRESS = "server.address";
        String SERVER_PORT = "server.port";
        String USERNAME = "username";
        String PASSWORD = "password";

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.form);

        // Обращение к UI - наверное можно заменить на DI
        final EditText serverAddress = findViewById(R.id.serverAddressEditText);
        final EditText serverPort = findViewById(R.id.serverPortEditText);
        final EditText username = findViewById(R.id.usernameEditText);
        final EditText password = findViewById(R.id.passwordEditText);

        // Разделяемые настройки - доступны по идее из любого места приложения
        final SharedPreferences prefs = getSharedPreferences(Prefs.NAME, MODE_PRIVATE);

        serverAddress.setText(prefs.getString(Prefs.SERVER_ADDRESS, ""));
        username.setText(prefs.getString(Prefs.USERNAME, ""));
        password.setText(prefs.getString(Prefs.PASSWORD, ""));

        int serverPortPrefValue = prefs.getInt(Prefs.SERVER_PORT, 0);
        serverPort.setText(String.valueOf(serverPortPrefValue == 0 ? "" : serverPortPrefValue));

        findViewById(R.id.connect).setOnClickListener(v -> {
            int serverPortNum;
            try {
                serverPortNum = Integer.parseInt(serverPort.getText().toString());
            } catch (NumberFormatException e) {
                serverPortNum = 0;
            }

            prefs.edit()
                    .putString(Prefs.SERVER_ADDRESS, serverAddress.getText().toString())
                    .putInt(Prefs.SERVER_PORT, serverPortNum)
                    .putString(Prefs.USERNAME, username.getText().toString())
                    .putString(Prefs.PASSWORD, password.getText().toString())
                    .apply();
            // Запрос на предоставление приложению возможности запускать впн Intent != null, если приложению еще не предоставлялся доступ
            Intent intent = VpnService.prepare(MainActivity.this);
            if (intent != null) {
                // Запрос на предоставление приложению возможности запускать впн
                startActivityForResult(intent, 0);
            } else {
                // Запрос был ранее предоставлен - сразу вызов onActivityResult с RESULT_OK
                onActivityResult(0, RESULT_OK, null);
            }
        });
        findViewById(R.id.disconnect).setOnClickListener(v -> {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_DISCONNECT));
        });
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            startService(getServiceIntent().setAction(CustomVpnService.ACTION_CONNECT));
        }
    }

    private Intent getServiceIntent() {
        return new Intent(this, CustomVpnService.class);
    }
}
