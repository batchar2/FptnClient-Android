package com.filantrop.pvnclient.utils;

import android.content.Context;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatSpinner;

import com.filantrop.pvnclient.R;

public class CustomSpinner extends AppCompatSpinner {

    private boolean isDropDownOpen = false;

    public CustomSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean performClick() {
        isDropDownOpen = !isDropDownOpen;
        updateArrow();
        return super.performClick();
    }

    private void updateArrow() {
//        int drawableRes = isDropDownOpen ? R.drawable.spinner_arrow_up : R.drawable.spinner_arrow_down;
//        setBackgroundResource(drawableRes);
    }
}