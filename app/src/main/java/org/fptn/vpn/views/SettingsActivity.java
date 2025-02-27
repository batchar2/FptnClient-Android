package org.fptn.vpn.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import org.fptn.vpn.R;
import org.fptn.vpn.viewmodel.FptnServerViewModel;
import org.fptn.vpn.views.adapter.FptnServerAdapter;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import lombok.Getter;

public class SettingsActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getName();

    private ListView serverListView;
    @Getter
    private FptnServerViewModel fptnViewModel;

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);
        initializeVariable();
    }

    @SuppressLint("InlinedApi")
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
                return true;
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

        fptnViewModel = new ViewModelProvider(this).get(FptnServerViewModel.class);
        fptnViewModel.getServerDtoListLiveData().observe(this, fptnServerDtos -> {
            if (fptnServerDtos != null && !fptnServerDtos.isEmpty()) {
                serverListView.setAdapter(new FptnServerAdapter(fptnServerDtos, R.layout.settings_server_list_item)); // NEED TO CHANGE THE ITEM LAYOUT
                setListViewHeightBasedOnChildren(serverListView);
            } else {
                // goto Login activity
                Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                startActivity(intent);
                finish();
            }
        });
        serverListView = findViewById(R.id.settings_servers_list);

        TextView versionTextView = findViewById(R.id.settings_fptn_version);
        try {
            Context context = getApplicationContext();
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            final String version = pInfo.versionName;
            versionTextView.setText(version);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // about
        TextView about = findViewById(R.id.settings_about);
        about.setText(Html.fromHtml(getString(R.string.info_message_html), Html.FROM_HTML_MODE_LEGACY));
        about.setMovementMethod(LinkMovementMethod.getInstance());
        about.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void onLogout(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_logout_title)
                .setMessage(R.string.dialog_logout_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    dialog.dismiss();
                    fptnViewModel.deleteAll();
                    // goto Login activity
                    Intent intent = new Intent(SettingsActivity.this, SplashActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    public void onUpdateToken(View v) {
        // Goto update token
        Intent intent = new Intent(SettingsActivity.this, SettingsActivityUpdateToken.class);
        startActivity(intent);
    }

    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }
}
