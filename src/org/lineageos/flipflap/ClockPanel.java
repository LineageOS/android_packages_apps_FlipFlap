/*
 * Copyright (c) 2017 The LineageOS Project
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * Also add information on how to contact you by electronic and paper mail.
 *
 */

package org.lineageos.flipflap;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockPanel extends LinearLayout {
    private static final String TAG = "ClockPanel";

    private final Context mContext;

    private AlarmManager mAlarmManager;

    private TextView mHoursView;
    private TextView mMinsView;
    private TextView mAmPmView;
    private TextView mDateView;

    private ImageView mAlarmIcon;
    private TextView mAlarmText;

    private boolean mReceiverRegistered;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)) {
                refreshClock();
            } else if (AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED.equals(action)) {
                refreshAlarmStatus();
            }
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (!mReceiverRegistered) {
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIME_TICK);
            filter.addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED);
            mContext.registerReceiver(mReceiver, filter);
            mReceiverRegistered = true;
            refreshClock();
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

    public ClockPanel(Context context) {
        this(context, null);
    }

    public ClockPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        mContext = context;

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        mHoursView = (TextView) findViewById(R.id.clock1);
        mMinsView = (TextView) findViewById(R.id.clock2);
        mAmPmView = (TextView) findViewById(R.id.clock_ampm);
        mDateView = (TextView) findViewById(R.id.date_regular);

        mAlarmIcon = (ImageView) findViewById(R.id.alarm_icon);
        mAlarmText = (TextView) findViewById(R.id.next_alarm_regular);
    }

    private void refreshClock() {
        Locale locale = Locale.getDefault();
        Date now = new Date();
        String dateFormat = mContext.getString(R.string.abbrev_wday_month_day_no_year);
        CharSequence date = DateFormat.format(dateFormat, now);
        String hours = new SimpleDateFormat(getHourFormat(), locale).format(now);
        String minutes = new SimpleDateFormat(mContext.getString(R.string.widget_12_hours_format_no_ampm_m),
                locale).format(now);
        String amPm = new SimpleDateFormat(
                mContext.getString(R.string.widget_12_hours_format_ampm), locale).format(now);

        mHoursView.setText(hours);
        mMinsView.setText(minutes);
        mAmPmView.setText(amPm);
        mDateView.setText(date);
    }

    private void refreshAlarmStatus() {
        String nextAlarm = getNextAlarm();
        if (!TextUtils.isEmpty(nextAlarm)) {
            // An alarm is set, deal with displaying it
            int color = mContext.getColor(R.color.clock_white);

            // Overlay the selected color on the alarm icon and set the imageview
            mAlarmIcon.setColorFilter(color);
            mAlarmIcon.setVisibility(View.VISIBLE);

            mAlarmText.setText(nextAlarm);
            mAlarmText.setVisibility(View.VISIBLE);
            mAlarmText.setTextColor(color);
        } else {
            // No alarm set or Alarm display is hidden, hide the views
            mAlarmIcon.setVisibility(View.GONE);
            mAlarmText.setVisibility(View.GONE);
        }
    }

    private String getHourFormat() {
        return DateFormat.is24HourFormat(mContext) ?
                mContext.getString(R.string.widget_24_hours_format_h_api_16) :
                mContext.getString(R.string.widget_12_hours_format_h);
    }

    private String getNextAlarm() {
        AlarmManager.AlarmClockInfo nextAlarmClock = mAlarmManager.getNextAlarmClock();
        if (nextAlarmClock != null) {
            String skeleton = DateFormat.is24HourFormat(mContext) ? "EHm" : "Ehma";
            String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
            return (String) DateFormat.format(pattern, nextAlarmClock.getTriggerTime());
        }

        return null;
    }
}
