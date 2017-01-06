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
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
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

import java.lang.Math;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FlipFlapActivity extends Activity {
    private static final String TAG = "FlipFlapActivity";

    private Context mContext;

    private FlipFlapStatus mStatus;
    private FlipFlapView mView;
    private String mCoverNode;
    private GestureDetector mDetector;
    private AlarmManager mAlarmManager;
    private PowerManager mPowerManager;
    private SensorManager mSensorManager;
    private TelecomManager mTelecomManager;

    // TODO(intervigil): Remove this when CircleView becomes a custom view
    int mCoverStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = this;
        mStatus = new FlipFlapStatus();

        mCoverNode = getResources().getString(R.string.cover_node);

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

        mDetector = new GestureDetector(mContext, mGestureListener);
        mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);

        mCoverStyle = getResources().getInteger(R.integer.config_deviceCoverType);
        switch (mCoverStyle) {
            case 1:
                mView = new DotcaseView(mContext, mStatus);
                setContentView(mView);
                break;
            case 2:
                setContentView(R.layout.circle_layout);
                mView = (CircleView) findViewById(R.id.circle_view);
                break;
        }

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = mView.getScreenBrightness();
        getWindow().setAttributes(lp);

        IntentFilter filter = new IntentFilter();
        filter.addAction(FlipFlapUtils.ACTION_COVER_CLOSED);
        filter.addAction(FlipFlapUtils.ACTION_KILL_ACTIVITY);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction("com.android.deskclock.ALARM_ALERT");
        mContext.getApplicationContext().registerReceiver(mReceiver, filter);

        mStatus.stopRunning();
    }

    @Override
    public void onStart() {
        super.onStart();

        new Thread(mService).start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(mSensorEventListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                SensorManager.SENSOR_DELAY_NORMAL);

        boolean screenOn = mPowerManager.isInteractive();
        if (!screenOn) {
            mPowerManager.wakeUp(SystemClock.uptimeMillis(), "Cover Closed");
        }

        mView.onInvalidate();
        if (mCoverStyle > 1) {
            // TODO(intervigil): Remove this when CircleView becomes a custom view
            refreshClock();
            refreshAlarmStatus();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPowerManager.goToSleep(SystemClock.uptimeMillis());
        try {
            mSensorManager.unregisterListener(mSensorEventListener);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to unregister listener", e);
        }
        mStatus.stopRunning();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mStatus.stopRinging();
        mStatus.stopAlarm();
        mStatus.setOnTop(false);
        mStatus.stopRunning();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mStatus.isPocketed()) {
            mDetector.onTouchEvent(event);
            return super.onTouchEvent(event);
        } else {
            // Say that we handled this event so nobody else does
            return true;
        }
    }

    private void refreshClock() {
        LinearLayout clockPanel = (LinearLayout) findViewById(R.id.clock_panel);
        clockPanel.bringToFront();

        Locale locale = Locale.getDefault();
        Date now = new Date();
        String dateFormat = mContext.getString(R.string.abbrev_wday_month_day_no_year);
        CharSequence date = DateFormat.format(dateFormat, now);
        String hours = new SimpleDateFormat(getHourFormat(), locale).format(now);
        String minutes = new SimpleDateFormat(mContext.getString(R.string.widget_12_hours_format_no_ampm_m),
                locale).format(now);
        String amPm = new SimpleDateFormat(
                mContext.getString(R.string.widget_12_hours_format_ampm), locale).format(now);

        TextView hoursView = (TextView) findViewById(R.id.clock1);
        TextView minsView = (TextView) findViewById(R.id.clock2);
        TextView amPmView = (TextView) findViewById(R.id.clock_ampm);
        TextView dateView = (TextView) findViewById(R.id.date_regular);

        hoursView.setText(hours);
        minsView.setText(minutes);
        amPmView.setText(amPm);
        dateView.setText(date);
    }

    private void refreshAlarmStatus() {
        ImageView alarmIcon = (ImageView) findViewById(R.id.alarm_icon);
        TextView alarmText = (TextView) findViewById(R.id.nextAlarm_regular);

        String nextAlarm = getNextAlarm();
        if (!TextUtils.isEmpty(nextAlarm)) {
            // An alarm is set, deal with displaying it
            int color = mContext.getColor(R.color.clock_white);

            // Overlay the selected color on the alarm icon and set the imageview
            alarmIcon.setColorFilter(color);
            alarmIcon.setVisibility(View.VISIBLE);

            alarmText.setText(nextAlarm);
            alarmText.setVisibility(View.VISIBLE);
            alarmText.setTextColor(color);
        } else {
            // No alarm set or Alarm display is hidden, hide the views
            alarmIcon.setVisibility(View.GONE);
            alarmText.setVisibility(View.GONE);
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

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                if (!mStatus.isPocketed()) {
                    if (event.values[0] < event.sensor.getMaximumRange()) {
                        mStatus.setPocketed(true);
                    }
                } else {
                    mStatus.setPocketed(false);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing
        }
    };

    private final Runnable mService = new Runnable() {
        @Override
        public void run() {
            if (mStatus.isRunning()) {
                // Already running
                return;
            }

            mStatus.startRunning();
            while (mStatus.isRunning()) {
                int timeout;
                Intent batteryIntent = mContext.getApplicationContext().registerReceiver(null,
                        new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (batteryIntent.getIntExtra("plugged", -1) > 0) {
                    timeout = 40;
                } else {
                    timeout = 20;
                }

                for (int i = 0; i <= timeout; i++) {
                    if (mStatus.isResetTimer() || mStatus.isRinging() || mStatus.isAlarm()) {
                        i = 0;
                    }

                    if (!mStatus.isRunning()) {
                        return;
                    }

                    try {
                        BufferedReader br = new BufferedReader(
                                new FileReader(mCoverNode));
                        String value = br.readLine();
                        br.close();

                        if (value.equals("0")) {
                            mStatus.stopRunning();
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

                    mView.onInvalidate();
                }
                mPowerManager.goToSleep(SystemClock.uptimeMillis());
            }
        }
    };

    private final GestureDetector.SimpleOnGestureListener mGestureListener =
        new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent event) {
            boolean screenOn = mPowerManager.isInteractive();
            if (screenOn) {
                onPause();
            }
            return true;
        }

        @Override
        public boolean onSingleTapUp (MotionEvent e) {
            mStatus.resetTimer();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.e(TAG,"onScroll");
            if (Math.abs(distanceY) < 60) {
                // Did not meet the threshold for a scroll
                Log.e(TAG,"distanceY <60");
                return true;
            }

            if (mView.supportsCallActions() && mStatus.isRinging()) {
                mStatus.setOnTop(false);
                Log.e(TAG,"isringing, checking scroll length");
                if (distanceY < 60) {
                    mTelecomManager.endCall();
                } else if (distanceY > 60) {
                    mTelecomManager.acceptRingingCall();
                }
            } else if (mView.supportsAlarmActions() && mStatus.isAlarm()) {
                Intent intent = new Intent();
                if (distanceY < 60) {
                    intent.setAction(FlipFlapUtils.ACTION_ALARM_DISMISS);
                    mStatus.setOnTop(false);
                    mContext.sendBroadcast(intent);
                    mStatus.stopAlarm();
                } else if (distanceY > 60) {
                    intent.setAction(FlipFlapUtils.ACTION_ALARM_SNOOZE);
                    mStatus.setOnTop(false);
                    mContext.sendBroadcast(intent);
                    mStatus.stopAlarm();
                }
            }
            return true;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(FlipFlapUtils.ACTION_KILL_ACTIVITY))  {
                try {
                    context.getApplicationContext().unregisterReceiver(mReceiver);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Failed to unregister receiver", e);
                }
                mStatus.stopRunning();
                finish();
                overridePendingTransition(0, 0);
                onDestroy();
            } else if (intent.getAction().equals(FlipFlapUtils.ACTION_COVER_CLOSED)) {
                onResume();
            } else if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) &&
                    mView.supportsCallActions()) {
                Log.e(TAG, "Call recognized");
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (state.equals("RINGING")) {
                    Log.e(TAG, "in Ringing");
                    String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                            Uri.encode(number));
                    Cursor cursor = context.getContentResolver().query(uri,
                            new String[] {ContactsContract.PhoneLookup.DISPLAY_NAME},
                            number, null, null);
                    String name;
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndex(
                                ContactsContract.PhoneLookup.DISPLAY_NAME));
                    } else {
                        name = "";
                    }
                    cursor.close();

                    if (number.equalsIgnoreCase("restricted")) {
                        // If call is restricted, don't show a number
                        name = number;
                        number = "";
                    }

                    name = normalize(name);
                    name = name + "  "; // Add spaces so the scroll effect looks good

                    mStatus.startRinging(number, name);
                    mStatus.setOnTop(true);
                    new Thread(new ensureTopActivity()).start();

                } else {
                    mStatus.setOnTop(false);
                    mStatus.stopRinging();
                }
            } else if (intent.getAction().equals("com.android.deskclock.ALARM_ALERT") &&
                    mView.supportsAlarmActions()) {
                // add other alarm apps here
                mStatus.startAlarm();
                mStatus.setOnTop(true);
                new Thread(new ensureTopActivity()).start();
            }
        }
    };


    /**
     * Normalizes a string to lowercase without diacritics
     */
    private static String normalize(String str) {
        return Normalizer.normalize(str.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("æ", "ae")
                .replaceAll("ð", "d")
                .replaceAll("ø", "o")
                .replaceAll("þ", "th")
                .replaceAll("ß", "ss")
                .replaceAll("œ", "oe");
    }

    private class ensureTopActivity implements Runnable {
        Intent i = new Intent();

        @Override
        public void run() {
            while ((mStatus.isRinging() || mStatus.isAlarm())
                    && mStatus.isOnTop()) {
                ActivityManager am =
                        (ActivityManager) mContext.getSystemService(Activity.ACTIVITY_SERVICE);
                if (!am.getRunningTasks(1).get(0).topActivity.getPackageName().equals(
                        "org.lineageos.flipflap")) {
                    i.setClassName("org.lineageos.flipflap", "org.lineageos.flipflap.FlipFlapActivity");
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mContext.startActivity(i);
                }
                try {
                    Thread.sleep(100);
                } catch (IllegalArgumentException e) {
                    // This isn't going to happen
                } catch (InterruptedException e) {
                    Log.i(TAG, "Sleep interrupted", e);
                }
            }
        }
    }
}
