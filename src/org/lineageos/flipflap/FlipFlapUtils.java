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

public class FlipFlapUtils {

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

    static final int DELAYED_SCREEN_OFF_MS = 5000;
}
