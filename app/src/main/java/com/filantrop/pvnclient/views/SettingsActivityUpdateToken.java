package com.filantrop.pvnclient.views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

    private void initializeVariable() {
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

        String html = "<div style=\"text-align:center;\">Use the Telegram <a href=\"https://t.me/fptn_bot\">bot</a> to get your key.</div>";
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
        label.setMovementMethod(LinkMovementMethod.getInstance());

        // HIDE KEYBOARD
        EditText editText = findViewById(R.id.fptn_login_link_input);
        editText.setShowSoftInputOnFocus(false);
    }

    public void onCancel(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onSave(View v) {
        final EditText linkInput = findViewById(R.id.fptn_login_link_input);
        final String fptnLink = linkInput.getText().toString();
        if (fptnLink.startsWith("fptn://") && fptnViewModel.parseAndSaveFptnLink(fptnLink)) {
            Toast.makeText(getApplicationContext(), "Token was updated!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), "Invalid link format or saving failed", Toast.LENGTH_SHORT).show();
        }
    }
}
