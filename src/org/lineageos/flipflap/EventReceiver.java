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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.WindowManagerPolicy.WindowManagerFuncs;

public class EventReceiver extends BroadcastReceiver {
    static final String TAG = "FlipFlap";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (cyanogenmod.content.Intent.ACTION_LID_STATE_CHANGED.equals(intent.getAction())) {
            int lidState = intent.getIntExtra(cyanogenmod.content.Intent.EXTRA_LID_STATE, -1);
            Log.d(TAG, "Got lid state change event, new state " + lidState);

            Intent serviceIntent = new Intent(context, FlipFlapService.class);
            if (lidState == WindowManagerFuncs.LID_CLOSED) {
                context.startService(serviceIntent);
            } else {
                context.stopService(serviceIntent);
            }
        }
    }
}
