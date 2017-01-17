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
import android.view.View;
import android.widget.RelativeLayout;

import java.util.List;

public class CircleView extends RelativeLayout implements FlipFlapView {
    private static final String TAG = "CircleView";

    private final Context mContext;

    private ClockPanel mClockPanel;

    public CircleView(Context context) {
        super(context);

        mContext = context;

        inflate(mContext, R.layout.circle_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();
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
    public boolean supportsNotifications() {
        return false;
    }

    @Override
    public float getScreenBrightness() {
        return 0.5f;
    }

    @Override
    public void updateNotifications(List<String> packages) {
    }
}
