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
import android.os.IBinder;
import android.os.UEventObserver;
import android.util.Log;

import org.cyanogenmod.internal.util.FileUtils;

public class FlipFlapService extends Service {

    private static final String TAG = "FlipFlap";

    private DeviceCover mDeviceCover;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Creating service");
        final Resources res = getResources();

        String ueventMatch = res.getString(R.string.cover_uevent_match);
        String coverNode = res.getString(R.string.cover_node);

        Log.e(TAG,"Cover uevent path :" + ueventMatch);
        mFlipFlapObserver.startObserving(ueventMatch);

        mDeviceCover = new DeviceCover(this);
        mDeviceCover.onCoverEvent(FileUtils.readOneLine(coverNode));
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    private final UEventObserver mFlipFlapObserver = new UEventObserver() {
        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            mDeviceCover.onCoverEvent(event.get("SWITCH_STATE"));
        }
    };
}
