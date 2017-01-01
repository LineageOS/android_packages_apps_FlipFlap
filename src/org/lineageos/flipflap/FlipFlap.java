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

import android.app.Activity;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FlipFlap extends Activity {

    private static final String TAG = "FlipFlap";

    private Resources res;
    private final IntentFilter mFilter = new IntentFilter();
    private GestureDetector mDetector;
    private PowerManager mPowerManager;
    private static Context mContext;
    private CircleView circleView;
    private TextView mHours;
    private TextView mMins;
    private TextView ampm;
    private TextView mDate;
    private LinearLayout mClockPanel;
    private ImageView mAlarmIcon;
    private TextView mAlarmText;
    static FlipFlapStatus sStatus = new FlipFlapStatus();

    String COVER_NODE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate: FlipFlap.class");

        mContext = this;
        res = getResources();

        mFilter.addAction(FlipFlapUtils.ACTION_COVER_CLOSED);
        mFilter.addAction(FlipFlapUtils.ACTION_KILL_ACTIVITY);
        mContext.getApplicationContext().registerReceiver(receiver, mFilter);

        COVER_NODE = res.getString(R.string.cover_node);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 120;
        getWindow().setAttributes(lp);

        setContentView(R.layout.circle_layout);
        circleView = (CircleView) findViewById(R.id.circle_view);
        mHours = (TextView) findViewById(R.id.clock1);
        mMins = (TextView) findViewById(R.id.clock2);
        ampm = (TextView) findViewById(R.id.clock_ampm);
        mDate = (TextView) findViewById(R.id.date_regular);
        mClockPanel = (LinearLayout) findViewById(R.id.clock_panel);
        mAlarmIcon = (ImageView) findViewById(R.id.alarm_icon);
        mAlarmText = (TextView) findViewById(R.id.nextAlarm_regular);

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mDetector = new GestureDetector(mContext, new FlipFlapGestureListener());
        sStatus.stopRunning();
    }

    @Override
    public void onStart() {
        super.onStart();

        new Thread(new Service()).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean screenOn = mPowerManager.isInteractive();
        // Starting up or coming back from screen off
        // Ensure device is awake and redraw
        if (!screenOn) {
            mPowerManager.wakeUp(SystemClock.uptimeMillis(), "Cover Closed");
        }

        Log.d(TAG, "Cover closed, Time to do work");
        circleView.postInvalidate();
        refreshClock();
        refreshAlarmStatus();
        mClockPanel.bringToFront();
    }

    @Override
    public void onPause() {
        super.onPause();

        // Pausing activity and turning screen off
        sStatus.stopRunning();
        mPowerManager.goToSleep(SystemClock.uptimeMillis());

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "FlipFlap, onDestroy");
        sStatus.stopRunning();

        // Closing up activity, lets wake up device.
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), "Cover Opened");

        super.onDestroy();

    }

    //===============================================================================================
    // Clock related functionality
    //===============================================================================================
    public void refreshClock() {
        Locale locale = Locale.getDefault();
        Date now = new Date();
        String dateFormat = getString(R.string.abbrev_wday_month_day_no_year);
        CharSequence date = DateFormat.format(dateFormat, now);
        String hours = new SimpleDateFormat(getHourFormat(), locale).format(now);
        String minutes = new SimpleDateFormat(getString(R.string.widget_12_hours_format_no_ampm_m),
                locale).format(now);
        String amPM = new SimpleDateFormat(getString(R.string.widget_12_hours_format_ampm),
                locale).format(now);

        mHours.setText(hours);
        mMins.setText(minutes);
        ampm.setText(amPM);
        mDate.setText(date);
    }

    private String getHourFormat() {
        String format;
        if (DateFormat.is24HourFormat(this)) {
            format = getString(R.string.widget_24_hours_format_h_api_16);
        } else {
            format = getString(R.string.widget_12_hours_format_h);
        }
        return format;
    }

    //===============================================================================================
    // Alarm related functionality
    //===============================================================================================
    private void refreshAlarmStatus() {

        String nextAlarm = getNextAlarm();
        if (!TextUtils.isEmpty(nextAlarm)) {
            // An alarm is set, deal with displaying it
            int color = getColor(R.color.clock_white);

            // Overlay the selected color on the alarm icon and set the imageview
            mAlarmIcon.setColorFilter(color);
            mAlarmIcon.setVisibility(View.VISIBLE);

            mAlarmText.setText(nextAlarm);
            mAlarmText.setVisibility(View.VISIBLE);
            mAlarmText.setTextColor(color);

            return;
        } else {

            // No alarm set or Alarm display is hidden, hide the views
            mAlarmIcon.setVisibility(View.GONE);
            mAlarmText.setVisibility(View.GONE);
        }
    }

    /**
     * @return A formatted string of the next alarm or null if there is no next alarm.
     */
    private String getNextAlarm() {
        String nextAlarm = null;

        AlarmManager am =(AlarmManager) getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo alarmClock = am.getNextAlarmClock();
        if (alarmClock != null) {
            nextAlarm = getNextAlarmFormattedTime(this, alarmClock.getTriggerTime());
        }

        return nextAlarm;
    }

    private static String getNextAlarmFormattedTime(Context context, long time) {
        String skeleton = DateFormat.is24HourFormat(context) ? "EHm" : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return (String) DateFormat.format(pattern, time);
    }

    class Service implements Runnable {
        @Override
        public void run() {
            if (!sStatus.isRunning()) {
                sStatus.startRunning();
                while (sStatus.isRunning()) {
                    Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
                            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                    int timeout;

                    if(batteryIntent.getIntExtra("plugged", -1) > 0) {
                        timeout = 40;
                    } else {
                        timeout = 20;
                    }

                    for (int i = 0; i <= timeout; i++) {
                        if (!sStatus.isRunning()) {
                            return;
                        }

                        try {
                            BufferedReader br = new BufferedReader(new FileReader(
                                    COVER_NODE));
                            String value = br.readLine();
                            br.close();

                            if (value.equals("0")) {
                                sStatus.stopRunning();
                                finish();
                                overridePendingTransition(0, 0);
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Error reading cover device", e);
                        }

                        try {
                            Thread.sleep(500);
                        } catch (IllegalArgumentException e) {
                            // This isn't going to happen
                        } catch (InterruptedException e) {
                            Log.i(TAG, "Sleep interrupted", e);
                        }

                        circleView.postInvalidate();
                    }
                    mPowerManager.goToSleep(SystemClock.uptimeMillis());
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class FlipFlapGestureListener extends GestureDetector.SimpleOnGestureListener
    {

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            //if screen on, screen off & "pause"
            boolean screenOn = mPowerManager.isInteractive();
            Log.d(TAG, "DT detected, screenon:" + screenOn );
            if(screenOn) {
                onPause();
            }
            return true;
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(FlipFlapUtils.ACTION_KILL_ACTIVITY))  {
                try {
                    context.getApplicationContext().unregisterReceiver(receiver);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Failed to unregister receiver", e);
                }
                sStatus.stopRunning();
                finish();
                overridePendingTransition(0, 0);
                onDestroy();
            } else if (intent.getAction().equals(FlipFlapUtils.ACTION_COVER_CLOSED)) {
                onResume();
            }
        }
    };

}
