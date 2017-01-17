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
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class DeviceCover {

    private static final String TAG = "FlipFlap";

    private static final int COVER_STATE_CHANGED = 0;

    private final Object mLock = new Object();

    private Context mContext;
    int mCoverStyle;

    public DeviceCover(Context context) {
        mContext = context;
        final Resources res = mContext.getResources();

        mCoverStyle = res.getInteger(R.integer.config_deviceCoverType);
        Log.e(TAG, "cover style detected:" + mCoverStyle);
    }

    private void handleCoverChange(int state) {
        if (state == FlipFlapUtils.COVER_STATE_CLOSED &&
                mCoverStyle != FlipFlapUtils.COVER_STYLE_NONE) {
            Log.i(TAG, "Cover Closed, Creating FlipFlap Activity");
            mContext.startActivity(new Intent(mContext, FlipFlapActivity.class));
        } else {
            Log.i(TAG, "Cover Opened, Killing FlipFlap Activity");
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                    new Intent(FlipFlapUtils.ACTION_KILL_ACTIVITY));
        }
    }

    public void onCoverEvent(String state) {
        Message message = new Message();
        message.what = COVER_STATE_CHANGED;
        message.arg1 = Integer.parseInt(state);

        mHandler.sendMessage(message);
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

    public static FlipFlapView createFlipFlapView(Context context) {
        final Resources res = context.getResources();
        int coverStyle = res.getInteger(R.integer.config_deviceCoverType);
        switch (coverStyle) {
            case FlipFlapUtils.COVER_STYLE_DOTCASE: return new DotcaseView(context);
            case FlipFlapUtils.COVER_STYLE_CIRCLE: return new CircleView(context);
            case FlipFlapUtils.COVER_STYLE_RECTANGULAR: return new RectangularView(context);
        }
        // Not possible because of the check, above, matching on the valid covers
        return null;
    }
}
