/*
 * SPDX-FileCopyrightText: 2017 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.internal.util.ArrayUtils;

import lineageos.hardware.LineageHardwareManager;

public class FlipFlapUtils {

    static final String OUR_PACKAGE_NAME = "org.lineageos.flipflap";

    static final String ACTION_COVER_CLOSED = "org.lineageos.flipflap.COVER_CLOSED";
    static final String ACTION_ALARM_ALERT = "com.android.deskclock.ALARM_ALERT";
    static final String ACTION_ALARM_DISMISS = "com.android.deskclock.ALARM_DISMISS";
    static final String ACTION_ALARM_SNOOZE = "com.android.deskclock.ALARM_SNOOZE";
    static final String ACTION_ALARM_DONE = "com.android.deskclock.ALARM_DONE";

    static final int COVER_STATE_OPENED = 0;
    static final int COVER_STATE_CLOSED = 1;

    // These have to match with "config_deviceCoverType" from res/values/config.xml
    static final int COVER_STYLE_NONE = 0;
    static final int COVER_STYLE_DOTCASE = 1;
    static final int COVER_STYLE_CIRCLE = 2;
    static final int COVER_STYLE_RECTANGULAR = 3;
    static final int COVER_STYLE_ICEVIEW = 4;

    static final int DELAYED_SCREEN_OFF_MS = 5000;
    static final int DELAYED_SCREEN_OFF_NEVER = -1000;

    static final String KEY_TIMEOUT_UNPLUGGED = "timeout_unplugged";
    static final String KEY_TIMEOUT_PLUGGED = "timeout_plugged";
    static final String KEY_BATTERY_INDICATION = "battery_indication";

    private static final int[] COVER_TYPES_WITH_CHARGING_INDICATION = new int[] {
            COVER_STYLE_CIRCLE,
            COVER_STYLE_RECTANGULAR
    };

    public static int getCoverStyle(Context context) {
        return context.getResources().getInteger(R.integer.config_deviceCoverType);
    }

    public static boolean showBatteryStatus(Context context) {
        return getPreferences(context).getBoolean(KEY_BATTERY_INDICATION, true);
    }

    public static int getTimeout(Context context, String key) {
        return Integer.parseInt(getPreferences(context).getString(key, "5")) * 1000;
    }

    public static int getTimeout(Context context, boolean isCharging) {
        String key = isCharging ? KEY_TIMEOUT_PLUGGED : KEY_TIMEOUT_UNPLUGGED;
        return getTimeout(context, key);
    }

    public static boolean showsChargingStatus(int cover) {
        return ArrayUtils.contains(COVER_TYPES_WITH_CHARGING_INDICATION, cover);
    }

    public static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static boolean getHighTouchSensitivitySupported(Context context) {
        final LineageHardwareManager hardware = LineageHardwareManager.getInstance(context);
        return hardware.isSupported(LineageHardwareManager.FEATURE_HIGH_TOUCH_SENSITIVITY);
    }

}
