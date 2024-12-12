package com.filantrop.pvnclient.views.callback;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;

import org.checkerframework.checker.nullness.qual.Nullable;

public interface DBFutureCallback<V extends @Nullable Object> extends FutureCallback<V> {

    @Override
    default void onFailure(@NonNull Throwable t) {
        Log.e(DBFutureCallback.class.getName(), "Failed to load from DB", t);
    }

}
