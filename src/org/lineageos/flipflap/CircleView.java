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
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.BatteryManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CircleView extends FlipFlapView {
    private static final String TAG = "CircleView";

    private final Context mContext;
    private final Resources res;
    private final IntentFilter mFilter = new IntentFilter();
    private Paint mPaint;
    private int mCenter_x;
    private int mCenter_y;
    private int mRadius;
    private int mOffset_x;
    private int mOffset_y;
    private int mOffset_rad;

    private Intent batteryStatus;

    private AlarmManager mAlarmManager;

    private CircleView mView;
    private TextView mHours;
    private TextView mMins;
    private TextView mAmPm;
    private TextView mDate;
    private LinearLayout mClockPanel;
    private ImageView mAlarmIcon;
    private TextView mAlarmText;

    public CircleView(Context context) {
        super(context);

        mContext = context;

        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

        // TODO(intervigil): Merge clock panel layout back into CircleView itself
        mView = (CircleView) findViewById(R.id.circle_view);
        mHours = (TextView) findViewById(R.id.clock1);
        mMins = (TextView) findViewById(R.id.clock2);
        mAmPm = (TextView) findViewById(R.id.clock_ampm);
        mDate = (TextView) findViewById(R.id.date_regular);
        mClockPanel = (LinearLayout) findViewById(R.id.clock_panel);
        mAlarmIcon = (ImageView) findViewById(R.id.alarm_icon);
        mAlarmText = (TextView) findViewById(R.id.nextAlarm_regular);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        res = mContext.getResources();

        mOffset_x = res.getInteger(R.integer.x_offset);
        mOffset_y = res.getInteger(R.integer.y_offset);
        mOffset_rad = res.getInteger(R.integer.radius_offset);

        mCenter_x = FlipFlapUtils.getScreenWidth() / 2 + mOffset_x;
        mCenter_y = FlipFlapUtils.getScreenHeight() * 13 / 48  + mOffset_y;
        mRadius = FlipFlapUtils.getScreenWidth() * 4 / 9 + mOffset_rad;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = mContext.registerReceiver(null, filter);
    }

    @Override
    public void onDraw(Canvas canvas) {
        drawBackground(canvas);
    }

    private void drawBackground(Canvas canvas) {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        canvas.drawRGB(0, 0, 0);
        mPaint.setStyle(Style.FILL);

        if (isCharging) {
            mPaint.setColor(res.getColor(R.color.charge_bat_bg));
        } else if (level >= 15) {
            mPaint.setColor(res.getColor(R.color.full_bat_bg));
        } else {
            mPaint.setColor(res.getColor(R.color.low_bat_bg));
        }
        canvas.drawCircle((float) mCenter_x, (float) mCenter_y, (float) mRadius, mPaint);
    }

    @Override
    public boolean supportsAlarmActions() {
        return false;
    }

    @Override
    public boolean supportsCallActions() {
        return false;
    }

    @Override
    public float getScreenBrightness() {
        return 0.5f;
    }

    @Override
    public void onInvalidate() {
        mView.postInvalidate();
        refreshClock();
        refreshAlarmStatus();
        mClockPanel.bringToFront();
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

        mHours.setText(hours);
        mMins.setText(minutes);
        mAmPm.setText(amPm);
        mDate.setText(date);
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
            return getNextAlarmFormattedTime(nextAlarmClock.getTriggerTime());
        }

        return null;
    }

    private String getNextAlarmFormattedTime(long time) {
        String skeleton = DateFormat.is24HourFormat(mContext) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }
}
