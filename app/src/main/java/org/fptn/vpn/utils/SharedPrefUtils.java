package org.fptn.vpn.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.fptn.vpn.R;
import org.fptn.vpn.core.common.Constants;

public class SharedPrefUtils {

    public static String getSniHostname(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Constants.APPLICATION_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        return sharedPreferences.getString(Constants.CURRENT_SNI_SHARED_PREF_KEY, context.getString(R.string.default_sni));
    }
}
