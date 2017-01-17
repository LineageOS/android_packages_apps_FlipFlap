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
import android.content.ComponentName;
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
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.text.Normalizer;

public class FlipFlapActivity extends Activity {
    private static final String TAG = "FlipFlapActivity";

    private FlipFlapStatus mStatus;
    private FlipFlapView mView;
    private GestureDetector mDetector;
    private PowerManager mPowerManager;
    private SensorManager mSensorManager;
    private TelecomManager mTelecomManager;
    private LocalBroadcastManager mBroadcastManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatus = new FlipFlapStatus();

        getWindow().addFlags(
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

        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(FlipFlapUtils.ACTION_KILL_ACTIVITY);
        mBroadcastManager.registerReceiver(mLocalReceiver, localFilter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(FlipFlapUtils.ACTION_ALARM_ALERT);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mReceiver, filter);

        if (mView.supportsNotifications()) {
            try {
                mNotificationListener.registerAsSystemService(this,
                        new ComponentName(this, getClass()), UserHandle.USER_ALL);
            } catch (RemoteException e) {
                Log.e(TAG, "Unable to register notification listener", e);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBroadcastManager.unregisterReceiver(mLocalReceiver);
        unregisterReceiver(mReceiver);

        if (mView.supportsNotifications()) {
            try {
                mNotificationListener.unregisterAsSystemService();
            } catch (RemoteException e) {
                // Ignore.
            }
        }

        mStatus.stopRinging();
        mStatus.stopAlarm();
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), "Cover Opened");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mView.canUseProximitySensor()) {
            mSensorManager.registerListener(mSensorEventListener,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        boolean screenOn = mPowerManager.isInteractive();
        if (!screenOn) {
            mPowerManager.wakeUp(SystemClock.uptimeMillis(), "Cover Closed");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPowerManager.goToSleep(SystemClock.uptimeMillis());
        if (mView.canUseProximitySensor()) {
            try {
                mSensorManager.unregisterListener(mSensorEventListener);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to unregister listener", e);
            }
        }
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
                mStatus.setPocketed(event.values[0] < event.sensor.getMaximumRange());
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
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (Math.abs(distanceY) < 60) {
                // Did not meet the threshold for a scroll
                return true;
            }

            if (mView.supportsCallActions() && mStatus.isRinging()) {
                if (distanceY < 60) {
                    mTelecomManager.endCall();
                } else if (distanceY > 60) {
                    mTelecomManager.acceptRingingCall();
                }
            } else if (mView.supportsAlarmActions() && mStatus.isAlarm()) {
                if (distanceY < 60) {
                    sendBroadcast(new Intent(FlipFlapUtils.ACTION_ALARM_DISMISS));
                    mStatus.stopAlarm();
                } else if (distanceY > 60) {
                    sendBroadcast(new Intent(FlipFlapUtils.ACTION_ALARM_SNOOZE));
                    mStatus.stopAlarm();
                }
            }
            return true;
        }
    };

    private final BroadcastReceiver mLocalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (FlipFlapUtils.ACTION_KILL_ACTIVITY.equals(action)) {
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
                    ((View) mView).postInvalidate();
                } else {
                    mStatus.stopRinging();
                }
            } else if (intent.getAction().equals(FlipFlapUtils.ACTION_ALARM_ALERT) &&
                    mView.supportsAlarmActions()) {
                // add other alarm apps here
                mStatus.startAlarm();
                ((View) mView).postInvalidate();
            }
        }
    };

    private final NotificationListenerService mNotificationListener =
            new NotificationListenerService() {
        private RankingMap mRankingMap;
        private final Comparator<StatusBarNotification> mRankingComparator =
                new Comparator<StatusBarNotification>() {

            private final Ranking mLhsRanking = new Ranking();
            private final Ranking mRhsRanking = new Ranking();

            @Override
            public int compare(StatusBarNotification lhs, StatusBarNotification rhs) {
                mRankingMap.getRanking(lhs.getKey(), mLhsRanking);
                mRankingMap.getRanking(rhs.getKey(), mRhsRanking);
                return Integer.compare(mLhsRanking.getRank(), mRhsRanking.getRank());
            }
        };

        @Override
        public void onListenerConnected() {
            handleNotificationUpdate(getCurrentRanking());
        }

        @Override
        public void onNotificationPosted(StatusBarNotification sbn, RankingMap ranking) {
            handleNotificationUpdate(ranking);
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn, RankingMap ranking) {
            handleNotificationUpdate(ranking);
        }

        @Override
        public void onNotificationRankingUpdate(RankingMap ranking) {
            handleNotificationUpdate(ranking);
        }

        private void handleNotificationUpdate(RankingMap ranking) {
            mRankingMap = ranking;

            List<StatusBarNotification> notifications = Arrays.asList(getActiveNotifications());
            Collections.sort(notifications, mRankingComparator);

            ArrayList<String> packageNames = new ArrayList<>();
            for (StatusBarNotification sbn : notifications) {
                if (!packageNames.contains(sbn.getPackageName())) {
                    packageNames.add(sbn.getPackageName());
                }
            }
            mView.updateNotifications(packageNames);
        }
    };
}
