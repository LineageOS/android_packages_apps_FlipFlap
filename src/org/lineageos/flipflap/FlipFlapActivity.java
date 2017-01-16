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
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.lang.Math;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;

public class FlipFlapActivity extends Activity {
    private static final String TAG = "FlipFlapActivity";

    private FlipFlapStatus mStatus;
    private FlipFlapView mView;
    private String mCoverNode;
    private GestureDetector mDetector;
    private PowerManager mPowerManager;
    private SensorManager mSensorManager;
    private TelecomManager mTelecomManager;
    private LocalBroadcastManager mBroadcastManager;
    private boolean mIsPlugged;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                    View.SYSTEM_UI_FLAG_FULLSCREEN);

        mDetector = new GestureDetector(this, mGestureListener);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mTelecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        mBroadcastManager = LocalBroadcastManager.getInstance(this);

        mView = DeviceCover.createFlipFlapView(this, mStatus);
        setContentView((View) mView);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = mView.getScreenBrightness();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        getWindow().setAttributes(lp);

        mBroadcastManager.registerReceiver(mLocalReceiver,
                new IntentFilter(FlipFlapUtils.ACTION_KILL_ACTIVITY));

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(FlipFlapUtils.ACTION_ALARM_ALERT);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mReceiver, filter);

        mStatus.stopRunning();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBroadcastManager.unregisterReceiver(mLocalReceiver);
        unregisterReceiver(mReceiver);

        mStatus.stopRinging();
        mStatus.stopAlarm();
        mStatus.setOnTop(false);
        mStatus.stopRunning();
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), "Cover Opened");
    }

    @Override
    public void onStart() {
        super.onStart();
        mStatusThread.start();
    }

    @Override
    public void onStop() {
        super.onStop();
        mStatus.stopRunning();
        mStatusThread.interrupt();
        try {
            mStatusThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
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
    public boolean onTouchEvent(MotionEvent event) {
        if (!mStatus.isPocketed()) {
            mDetector.onTouchEvent(event);
            return super.onTouchEvent(event);
        } else {
            // Say that we handled this event so nobody else does
            return true;
        }
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

    private final Thread mStatusThread = new Thread() {
        @Override
        public void run() {
            if (mStatus.isRunning()) {
                // Already running
                return;
            }

            mStatus.startRunning();
            while (mStatus.isRunning()) {
                int timeout;
                if (mIsPlugged) {
                    timeout = FlipFlapUtils.TIMEOUT_PLUGGED;
                } else {
                    timeout = FlipFlapUtils.TIMEOUT_UNPLUGGED;
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

                        if (value.equals(String.valueOf(FlipFlapUtils.COVER_STATE_OPENED))) {
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
            if (Math.abs(distanceY) < 60) {
                // Did not meet the threshold for a scroll
                return true;
            }

            if (mView.supportsCallActions() && mStatus.isRinging()) {
                mStatus.setOnTop(false);
                if (distanceY < 60) {
                    mTelecomManager.endCall();
                } else if (distanceY > 60) {
                    mTelecomManager.acceptRingingCall();
                }
            } else if (mView.supportsAlarmActions() && mStatus.isAlarm()) {
                if (distanceY < 60) {
                    mStatus.setOnTop(false);
                    mBroadcastManager.sendBroadcast(
                            new Intent(FlipFlapUtils.ACTION_ALARM_DISMISS));
                    mStatus.stopAlarm();
                } else if (distanceY > 60) {
                    mStatus.setOnTop(false);
                    mBroadcastManager.sendBroadcast(
                            new Intent(FlipFlapUtils.ACTION_ALARM_SNOOZE));
                    mStatus.stopAlarm();
                }
            }
            return true;
        }
    };

    private Runnable mEnsureTopActivity = new Runnable() {
        @Override
        public void run() {
            while ((mStatus.isRinging() || mStatus.isAlarm())
                    && mStatus.isOnTop()) {
                ActivityManager am =
                        (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
                if (!am.getRunningTasks(1).get(0).topActivity.getPackageName().equals(
                        "org.lineageos.flipflap")) {
                    Intent intent = new Intent();
                    intent.setClassName(FlipFlapActivity.class.getPackage().getName(),
                            FlipFlapActivity.class.getSimpleName());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
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
    };

    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (FlipFlapUtils.ACTION_KILL_ACTIVITY.equals(intent.getAction())) {
                finish();
                overridePendingTransition(0, 0);
            }
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action) &&
                    mView.supportsCallActions()) {
                String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
                if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                    String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                    Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                            Uri.encode(number));
                    Cursor cursor = context.getContentResolver().query(uri,
                            new String[] { ContactsContract.PhoneLookup.DISPLAY_NAME },
                            number, null, null);
                    String name = cursor != null && cursor.moveToFirst()
                            ? cursor.getString(0) : "";
                    if (cursor != null) {
                        cursor.close();
                    }

                    if (number.equalsIgnoreCase("restricted")) {
                        // If call is restricted, don't show a number
                        name = number;
                        number = "";
                    }

                    name = normalize(name);
                    name = name + "  "; // Add spaces so the scroll effect looks good

                    mStatus.startRinging(number, name);
                    mStatus.setOnTop(true);
                    new Thread(mEnsureTopActivity).start();
                } else {
                    mStatus.setOnTop(false);
                    mStatus.stopRinging();
                }
            } else if (intent.getAction().equals(FlipFlapUtils.ACTION_ALARM_ALERT) &&
                    mView.supportsAlarmActions()) {
                // add other alarm apps here
                mStatus.startAlarm();
                mStatus.setOnTop(true);
                new Thread(mEnsureTopActivity).start();
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mIsPlugged = intent.getIntExtra("plugged", -1) > 0;
            }
        }
    };
}
