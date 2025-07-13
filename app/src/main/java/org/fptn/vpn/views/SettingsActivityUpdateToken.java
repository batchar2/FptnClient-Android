package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.viewmodel.FptnServerViewModel;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import lombok.Getter;

public class SettingsActivityUpdateToken extends AppCompatActivity {
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
        bottomNavigationView.setOnItemSelectedListener(new CustomBottomNavigationListener(this, bottomNavigationView, R.id.menuSettings));

        // Show HTML
        TextView label = findViewById(R.id.fptn_login_html_label);
        label.setText(Html.fromHtml(getString(R.string.telegram_bot_html), Html.FROM_HTML_MODE_LEGACY));
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

    public void onCancel(View v) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void onSave(View v) {
        final EditText linkInput = findViewById(R.id.fptn_login_link_input);
        final String fptnLink = linkInput.getText().toString();
        final TextView errorTextView = findViewById(R.id.errorTextView);
        fptnViewModel.getErrorTextLiveData().observe(this, errorTextView::setText);

        try {
            fptnViewModel.parseAndSaveFptnLink(fptnLink);

            Toast.makeText(getApplicationContext(), R.string.token_was_updated, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(getTag(), "Token invalid: ", e);
            Toast.makeText(getApplicationContext(), R.string.token_saving_failed, Toast.LENGTH_SHORT).show();
            errorTextView.setVisibility(View.VISIBLE);
        }
    }

    private String getTag() {
        return this.getClass().getCanonicalName();
    }
}
