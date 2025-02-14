package com.filantrop.pvnclient.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
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
import com.filantrop.pvnclient.viewmodel.FptnServerViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import lombok.Getter;


public class SettingsActivityUpdateToken extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    @Getter
    private FptnServerViewModel fptnViewModel;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout_update_token);
        initializeVariable();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeVariable() {
        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);

        // FIXME
        bottomNavigationView = findViewById(R.id.bottomNavBar);
        bottomNavigationView.setSelectedItemId(R.id.menuSettings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menuHome) {
                Intent intent = new Intent(this, HomeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.menuSettings) {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
            } else if (itemId == R.id.menuShare) {
                bottomNavigationView.setSelectedItemId(R.id.menuSettings); // don't change

                final String shareTitle = getString(R.string.share_title);
                final String shareMessage = getString(R.string.share_message);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, shareTitle));
                return true;
            }
            return false;
        });
        // Show HTML
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(getString(R.string.telegram_bot_html), Html.FROM_HTML_MODE_LEGACY));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        // HIDE KEYBOARD
        EditText editText = findViewById(R.id.fptn_login_link_input);
        editText.setTextIsSelectable(true);
        editText.setShowSoftInputOnFocus(false);
        editText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    if (motionEvent.getX() > (view.getWidth() - view.getPaddingRight() - 50)){
                        ((EditText)view).setText("");
                    }
                }
                return false;
            }
        });
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);  // This just hide keyboard when activity starts
    }

    public void onCancel(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onSave(View v) {
        final EditText linkInput = findViewById(R.id.fptn_login_link_input);
        final String fptnLink = linkInput.getText().toString();
        if (fptnViewModel.parseAndSaveFptnLink(fptnLink)) {
            Toast.makeText(getApplicationContext(), R.string.token_was_updated, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
