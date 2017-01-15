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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.UserHandle;
import android.os.UEventObserver;
import android.util.Log;

import org.cyanogenmod.internal.util.FileUtils;

public class FlipFlapService extends Service {

    private static final String TAG = "FlipFlap";

    private static final int COVER_STATE_CHANGED = 0;

    private final Object mLock = new Object();

    private Context mContext;
    int mCoverStyle;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating service");
        mContext = this;
        Resources res = mContext.getResources();

        String ueventMatch = res.getString(R.string.cover_uevent_match);
        String coverNode = res.getString(R.string.cover_node);

        Log.e(TAG,"Cover uevent path :" + ueventMatch);
        mFlipFlapObserver.startObserving(ueventMatch);

        mCoverStyle = res.getInteger(R.integer.config_deviceCoverType);
        Log.e(TAG, "cover style detected:" + mCoverStyle);

        onCoverEvent(FileUtils.readOneLine(coverNode));
    }

    private void handleCoverChange(int state) {
        synchronized (mLock) {
            if (state == 1) {
                Log.i(TAG, "Cover Closed, Creating FlipFlap Activity");
                Intent intent = new Intent();
                switch (mCoverStyle) {
                    case 1:
                    case 2:
                        Log.i(TAG, "1 cover style detected:" + mCoverStyle);
                        intent.setClass(this, FlipFlapActivity.class);
                        intent.setAction(FlipFlapUtils.ACTION_COVER_CLOSED);
                        break;
                    case 0:
                        Log.w(TAG, "Invalid Lid Style, closing lid activity");
                        intent.setAction(FlipFlapUtils.ACTION_KILL_ACTIVITY);
                        break;
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            } else {
                Log.i(TAG, "Cover Opened, Killing FlipFlap Activity");
                Intent intent = new Intent(FlipFlapUtils.ACTION_KILL_ACTIVITY);
                mContext.sendBroadcastAsUser(intent, new UserHandle(UserHandle.USER_SYSTEM));
            }
        }
    }

    private void onCoverEvent(String state) {
        Message message = new Message();
        message.what = COVER_STATE_CHANGED;
        message.arg1 = Integer.parseInt(state);

        mHandler.sendMessage(message);
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private final Handler mHandler = new Handler(true /*async*/) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case COVER_STATE_CHANGED:
                    handleCoverChange(msg.arg1);
                    break;
            }
        }
    };

    private final UEventObserver mFlipFlapObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            onCoverEvent(event.get("SWITCH_STATE"));
        }
    };
}
