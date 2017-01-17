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
import android.view.View;
import android.widget.RelativeLayout;

public class RectangularView extends RelativeLayout implements FlipFlapView {
    private static final String TAG = "RectangularView";

    private final Context mContext;

    private RectangularBatteryView mBatteryView;
    private ClockPanel mClockPanel;

    public RectangularView(Context context) {
        super(context);

        mContext = context;

        inflate(mContext, R.layout.rectangular_view, this);

        mBatteryView = (RectangularBatteryView) findViewById(R.id.rectangular_battery);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();
    }

    @Override
    public void postInvalidate() {
        mBatteryView.postInvalidate();
        mClockPanel.postInvalidate();
        super.postInvalidate();
    }

    @Override
    public boolean canUseProximitySensor() {
        return false;
    }

    @Override
    public boolean supportsAlarmActions() {
        return false;
    }

    @Override
    public boolean supportsCallActions() {
        return false;
    }

    @Override
    public float getScreenBrightness() {
        return 0.5f;
    }
}
