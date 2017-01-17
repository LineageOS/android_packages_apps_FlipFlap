/*
 * Copyright (c) 2014 The CyanogenMod Project
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

public class FlipFlapStatus {
    private boolean mPocketed = false;
    private boolean mRinging = false;
    private boolean mAlarmClock = false;
    private int mRingCounter = 0;
    private String mCallerNumber = "";
    private String mCallerName = "";
    private int mCallerTicker = 0;

    synchronized boolean isPocketed() {
        return mPocketed;
    }

    synchronized void setPocketed(boolean val) {
        mPocketed = val;
    }

    synchronized int ringCounter() {
        return mRingCounter;
    }

    synchronized void resetRingCounter() {
        mRingCounter = 0;
    }

    synchronized void incrementRingCounter() {
        mRingCounter++;
    }

    synchronized void startRinging(String number, String name) {
        mCallerName = name;
        startRinging(number);
    }

    synchronized void startRinging(String number) {
        mRinging = true;
        mRingCounter = 0;
        mCallerNumber = number;
        mCallerTicker = -6;
    }

    synchronized void stopRinging() {
        mRinging = false;
        mCallerNumber = "";
        mCallerName = "";
    }

    synchronized int callerTicker() {
        if (mCallerTicker <= 0) {
            return 0;
        } else {
            return mCallerTicker;
        }
    }

    synchronized void incrementCallerTicker() {
        mCallerTicker++;
        if (mCallerTicker >= mCallerName.length()) {
            mCallerTicker = -3;
        }
    }

    synchronized void startAlarm() {
        mAlarmClock = true;
        mRingCounter = 0;
    }

    synchronized void stopAlarm() {
        mAlarmClock = false;
    }

    synchronized boolean isRinging() {
        return mRinging;
    }

    synchronized boolean isAlarm() {
        return mAlarmClock;
    }

    synchronized String getCallerName() {
        return mCallerName;
    }

    synchronized String getCallerNumber() {
        return mCallerNumber;
    }
}
