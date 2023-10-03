/*
 * SPDX-FileCopyrightText: 2017 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;

public class CallState {
    private static final String TAG = "CallState";

    private static final String[] DISPLAY_NAME = { ContactsContract.PhoneLookup.DISPLAY_NAME };
    private final int mState;
    private final String mName;
    private final String mNumber;

    public CallState(Context context, String state, String number) {
        this(context, stateStringToInt(state), number);
    }

    public CallState(Context context, int state, String number) {
        mState = state;
        if ("restricted".equalsIgnoreCase(number)) {
            // If call is restricted, don't show a number
            mName = number;
            mNumber = "";
        } else if (number == null) {
            mName = null;
            mNumber = null;
        } else {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number));
            Cursor cursor = context.getContentResolver().query(uri, DISPLAY_NAME, number, null,
                    null);
            if (cursor != null) {
                mName = cursor.moveToFirst() ? cursor.getString(0) : "";
                cursor.close();
            } else {
                mName = "";
            }

            mNumber = number;
        }
    }

    public boolean isRinging() {
        return mState == TelephonyManager.CALL_STATE_RINGING;
    }

    public boolean isActive() {
        return mState != TelephonyManager.CALL_STATE_IDLE;
    }

    public String getName() {
        return mName;
    }

    public String getNumber() {
        return mNumber;
    }

    public String toString() {
        return "CallState[mState=" + mState + ", mName=" + mName + ", mNumber=" + mNumber + "]";
    }

    private static int stateStringToInt(String state) {
        if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
            return TelephonyManager.CALL_STATE_IDLE;
        } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
            return TelephonyManager.CALL_STATE_OFFHOOK;
        } else if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
            return TelephonyManager.CALL_STATE_RINGING;
        } else {
            throw new IllegalArgumentException("Invalid state " + state);
        }
    }
}
