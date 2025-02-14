package com.filantrop.pvnclient.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.filantrop.pvnclient.R;
import com.filantrop.pvnclient.database.model.FptnServerDto;
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.filantrop.pvnclient.views.callback.DBFutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public class LoginActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private FptnServerViewModel fptnViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);

        initializeVariable();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // miss back button
            }
        });
    }

    @SuppressLint({"InlinedApi", "ClickableViewAccessibility"})
    private void initializeVariable() {
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);

        ListenableFuture<List<FptnServerDto>> allServersListFuture = fptnViewModel.getAllServers();
        Futures.addCallback(allServersListFuture, (DBFutureCallback<List<FptnServerDto>>) result -> {
            if (result != null && !result.isEmpty()) { // miss login
                Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(intent);
            } else {
                // show
            }
        }, this.getMainExecutor());

        // Show HTML
        final String telegramBot = getString(R.string.telegram_bot);
        final String textTemplate = getString(R.string.telegram_text_template);
        final String telegramBotLink = getString(R.string.telegram_bot_link);

        final String replacedText = textTemplate.replace("{}", "<a href=\"" + telegramBotLink + "\">" + telegramBot + "</a> ");
        String html = "<div style=\"text-align:center;\">" + replacedText + "</div>";

        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        // HIDE KEYBOARD
        EditText editText = findViewById(R.id.fptn_login_link_input);
        editText.setTextIsSelectable(true);
        editText.setShowSoftInputOnFocus(false);
        editText.setOnTouchListener((view, motionEvent) -> {
            if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                if (motionEvent.getX() > (view.getWidth() - view.getPaddingRight() - 50)) {
                    ((EditText) view).setText("");
                }
            }
            return false;
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);  // This just hide keyboard when activity starts
    }

    public void onLogin(View v) {
        final EditText linkInput = findViewById(R.id.fptn_login_link_input);
        final String fptnLink = linkInput.getText().toString();
        if (fptnViewModel.parseAndSaveFptnLink(fptnLink)) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
