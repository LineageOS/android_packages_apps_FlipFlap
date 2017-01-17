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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FlipFlapUtils {

    static final String ACTION_KILL_ACTIVITY = "org.lineageos.flipflap.KILL_ACTIVITY";
    static final String ACTION_COVER_CLOSED = "org.lineageos.flipflap.COVER_CLOSED";
    static final String ACTION_ALARM_ALERT = "com.android.deskclock.ALARM_ALERT";

    static final String ACTION_ALARM_DISMISS = "com.android.deskclock.ALARM_DISMISS";
    static final String ACTION_ALARM_SNOOZE = "com.android.deskclock.ALARM_SNOOZE";

    static final int COVER_STATE_OPENED = 0;
    static final int COVER_STATE_CLOSED = 1;

    // These have to match with "config_deviceCoverType" from res/values/config.xml
    static final int COVER_STYLE_NONE = 0;
    static final int COVER_STYLE_DOTCASE = 1;
    static final int COVER_STYLE_CIRCLE = 2;
    static final int COVER_STYLE_RECTANGULAR = 3;

    static final int TIMEOUT_UNPLUGGED = 20;
    static final int TIMEOUT_PLUGGED = 40;

    static final String KEY_TIMEOUT_UNPLUGGED = "timeout_unplugged";
    static final String KEY_TIMEOUT_PLUGGED = "timeout_plugged";

    private static final String KEY_ENABLED = "flipflap_enable";

    public static Boolean isEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_ENABLED, false);
    }

    public static int getPluggedTimeout(Context context) {
        return Integer.parseInt(getPreferences(context).getString(KEY_TIMEOUT_PLUGGED, "30"));
    }

    public static int getUnpluggedTimeout(Context context) {
        return Integer.parseInt(getPreferences(context).getString(KEY_TIMEOUT_PLUGGED, "10"));
    }

    public static int getTimeout(Context context, String key) {
        int timeOut;
        if (KEY_TIMEOUT_PLUGGED.equals(key)) {
            timeOut = getPluggedTimeout(context);
        } else {
            timeOut = getUnpluggedTimeout(context);
        }

        return timeOut;
    }

    private static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
