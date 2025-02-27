package org.fptn.vpn.utils;

import android.content.Context;

public class ResourcesUtils {

    public static String getStringResourceByName(Context context, String aString) {
        int resId = context.getResources().getIdentifier(aString, "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : null;
    }
}
