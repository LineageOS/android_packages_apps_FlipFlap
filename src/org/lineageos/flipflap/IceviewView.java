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
import android.view.LayoutInflater;

public class IceviewView extends FlipFlapView {
    private static final String TAG = "IceviewView";

    private ClockPanel mClockPanel;

    public IceviewView(Context context) {
        super(context);

        inflate(mContext, R.layout.iceview_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();
    }

    @Override
    protected boolean supportsAlarmActions() {
        return true;
    }

    @Override
    protected void updateAlarmState(boolean active) {
        setVisibility(active ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    protected boolean supportsCallActions() {
        return true;
    }

    @Override
    protected void updateCallState(CallState callState) {
        setVisibility(callState.isActive() ? View.INVISIBLE : View.VISIBLE);
    }
}
