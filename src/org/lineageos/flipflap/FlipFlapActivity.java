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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.lang.Math;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FlipFlapActivity extends Activity {
    private static final String TAG = "FlipFlapActivity";

    private Context mContext;

    private FlipFlapStatus mStatus;
    private FlipFlapView mView;
    private String mCoverNode;

    private GestureDetector mDetector;
    private PowerManager mPowerManager;
    private SensorManager mSensorManager;
    private TelecomManager mTelecomManager;

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
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mTelecomManager = (TelecomManager) mContext.getSystemService(Context.TELECOM_SERVICE);

        int coverStyle = getResources().getInteger(R.integer.config_deviceCoverType);
        mView = coverStyle == 1 ? new DotcaseView(mContext, mStatus) :
            new CircleView(mContext);
        setContentView(mView);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = mView.getScreenBrightness();
        getWindow().setAttributes(lp);

        IntentFilter filter = new IntentFilter();
        filter.addAction(FlipFlapUtils.ACTION_COVER_CLOSED);
        filter.addAction(FlipFlapUtils.ACTION_KILL_ACTIVITY);
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
            }
        }
    };
}
