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
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class CircleView extends FlipFlapView {
    private static final String TAG = "CircleView";

    private ClockPanel mClockPanel;
    private DatePanel mDatePanel;
    private NextAlarmPanel mNextAlarmPanel;
    private AlarmPanel mAlarmPanel;
    private PhonePanel mPhonePanel;
    private ImageButton mAlarmSnoozeButton;
    private ImageButton mAlarmDismissButton;
    private ImageButton mAnswerCallButton;
    private ImageButton mIgnoreCallButton;
    private ImageButton mEndCallButton;
    private TextView mIncomingCallName;
    private TextView mIncomingCallNumber;
    private boolean mRinging;
    private boolean mCallActive;
    private boolean mAlarmActive;

    public CircleView(Context context) {
        super(context);

        inflate(context, R.layout.circle_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();
        mDatePanel = (DatePanel) findViewById(R.id.date_panel);
        mNextAlarmPanel = (NextAlarmPanel) findViewById(R.id.next_alarm_panel);
        mAlarmPanel = (AlarmPanel) findViewById(R.id.alarm_panel);
        mPhonePanel = (PhonePanel) findViewById(R.id.phone_panel);

        mIncomingCallName = (TextView) findViewById(R.id.incoming_call_name);
        mIncomingCallNumber = (TextView) findViewById(R.id.incoming_call_number);
        mAnswerCallButton = (ImageButton) findViewById(R.id.answer_button);
        mIgnoreCallButton = (ImageButton) findViewById(R.id.ignore_button);
        mEndCallButton = (ImageButton) findViewById(R.id.end_call_button);
        mAnswerCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptRingingCall();
            }
        });
        mIgnoreCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                endCall();
            }
        });
        mEndCallButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                endCall();
            }
        });

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
    protected boolean supportsCallActions() {
        return true;
    }

    @Override
    protected void updateAlarmState(boolean active) {
        super.updateAlarmState(active);
        mAlarmActive = active;
        updateViewVisibility();
    }

    @Override
    public void updateCallState(CallState callState) {
        super.updateCallState(callState);
        mRinging = callState.isRinging();
        mCallActive = callState.isActive();
        mIncomingCallName.setText (mRinging ? callState.getName() : null);
        mIncomingCallNumber.setText(mRinging ? callState.getNumber() : null);
        updateViewVisibility();
    }

    private void updateViewVisibility() {
        if (mRinging || mCallActive) {
            mClockPanel.setVisibility(View.GONE);
            mDatePanel.setVisibility(View.GONE);
            mNextAlarmPanel.setVisibility(View.GONE);
            mAlarmPanel.setVisibility(View.GONE);
            mPhonePanel.setVisibility(View.VISIBLE);
            if (mRinging) {
                mAnswerCallButton.setVisibility(View.VISIBLE);
                mIgnoreCallButton.setVisibility(View.VISIBLE);
                mEndCallButton.setVisibility(View.GONE);
            } else {
                mAnswerCallButton.setVisibility(View.GONE);
                mIgnoreCallButton.setVisibility(View.GONE);
                mEndCallButton.setVisibility(View.VISIBLE);
            }
        } else if (mAlarmActive) {
            mClockPanel.setVisibility(View.VISIBLE);
            mDatePanel.setVisibility(View.GONE);
            mNextAlarmPanel.setVisibility(View.GONE);
            mAlarmPanel.setVisibility(View.VISIBLE);
            mPhonePanel.setVisibility(View.GONE);
        } else {
            mClockPanel.setVisibility(View.VISIBLE);
            mDatePanel.setVisibility(View.VISIBLE);
            mNextAlarmPanel.setVisibility(View.VISIBLE);
            mAlarmPanel.setVisibility(View.GONE);
            mPhonePanel.setVisibility(View.GONE);
        }
    }
}
