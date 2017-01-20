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
import android.graphics.Canvas;
import android.view.View;
import android.widget.ImageButton;

public class CircleView extends FlipFlapView {
    private static final String TAG = "CircleView";

    private ClockPanel mClockPanel;
    private DatePanel mDatePanel;
    private NextAlarmPanel mNextAlarmPanel;
    private AlarmPanel mAlarmPanel;
    private ImageButton mAlarmSnoozeButton;
    private ImageButton mAlarmDismissButton;

    public CircleView(Context context) {
        super(context);

        inflate(context, R.layout.circle_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();
        mDatePanel = (DatePanel) findViewById(R.id.date_panel);
        mNextAlarmPanel = (NextAlarmPanel) findViewById(R.id.next_alarm_panel);
        mAlarmPanel = (AlarmPanel) findViewById(R.id.alarm_panel);

        mAlarmSnoozeButton = (ImageButton) findViewById(R.id.snooze_button);
        mAlarmDismissButton = (ImageButton) findViewById(R.id.dismiss_button);

        mAlarmSnoozeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                snoozeAlarm();
            }
        });

        mAlarmDismissButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissAlarm();
            }
        });
    }

    @Override
    protected boolean supportsAlarmActions() {
        return true;
    }

    @Override
    protected void updateAlarmState(boolean active) {
        super.updateAlarmState(active);
        if (active) {
            mDatePanel.setVisibility(View.GONE);
            mNextAlarmPanel.setVisibility(View.GONE);
            mAlarmPanel.setVisibility(View.VISIBLE);
        } else {
            mDatePanel.setVisibility(View.VISIBLE);
            mNextAlarmPanel.setVisibility(View.VISIBLE);
            mAlarmPanel.setVisibility(View.GONE);
        }
    }
}
