/*
 * SPDX-FileCopyrightText: 2017-2021 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class NextAlarmPanel extends LinearLayout {
    private static final String TAG = "ClockPanel";

    private final Context mContext;

    private final AlarmManager mAlarmManager;
    private TextView mAlarmText;

    private boolean mReceiverRegistered;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED.equals(action)) {
                refreshAlarmStatus();
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mReceiverRegistered) {
            IntentFilter filter = new IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
            mReceiverRegistered = true;
            refreshAlarmStatus();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mReceiverRegistered) {
            mContext.unregisterReceiver(mReceiver);
            mReceiverRegistered = false;
        }
    }

    public NextAlarmPanel(Context context) {
        this(context, null);
    }

    public NextAlarmPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NextAlarmPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;
        mAlarmManager = mContext.getSystemService(AlarmManager.class);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mAlarmText = findViewById(R.id.next_alarm_regular);
    }

    private void refreshAlarmStatus() {
        String nextAlarm = getNextAlarm();
        mAlarmText.setText(nextAlarm);
    }

    public String getNextAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = mAlarmManager.getNextAlarmClock();
        if (nextAlarmClock != null) {
            String skeleton = DateFormat.is24HourFormat(mContext) ? "EHm" : "Ehma";
            String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
            return (String) DateFormat.format(pattern, nextAlarmClock.getTriggerTime());
        }

        return null;
    }
}
