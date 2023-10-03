/*
 * SPDX-FileCopyrightText: 2017-2021 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextClock;

import java.util.Locale;

public class DatePanel extends LinearLayout {
    private static final String TAG = "DatePanel";

    private final Context mContext;

    public DatePanel(Context context) {
        this(context, null);
    }

    public DatePanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DatePanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        final CharSequence dateFormat = getDateFormat(mContext);
        TextClock dateView = findViewById(R.id.date_regular);
        dateView.setFormat12Hour(dateFormat);
        dateView.setFormat24Hour(dateFormat);
    }

    private static CharSequence getDateFormat(Context context) {
        final String dateFormat = context.getString(R.string.abbrev_wday_month_day_no_year);
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), dateFormat);
    }
}
