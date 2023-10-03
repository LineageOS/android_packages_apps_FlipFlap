/*
 * SPDX-FileCopyrightText: 2017-2021 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.View;
import android.widget.LinearLayout;

import java.util.List;

public class IceviewView extends FlipFlapView {
    private static final String TAG = "IceviewView";

    private final ClockPanel mClockPanel;
    private final LinearLayout mNotificationsView;

    public IceviewView(Context context) {
        super(context);

        inflate(context, R.layout.iceview_view, this);

        mClockPanel = findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();

        mNotificationsView = findViewById(R.id.iceview_notifications);
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

    @Override
    protected boolean supportsNotifications() {
        return true;
    }

    @Override
    protected void updateNotifications(List<StatusBarNotification> notifications) {
        mNotificationsView.removeAllViews();
        for (StatusBarNotification sbn : notifications) {
            if (shouldShowNotification(sbn)) {
                IceviewNotificationView inv = (IceviewNotificationView) inflate(getContext(),
                        R.layout.iceview_notification_view, null);
                inv.setNotification(sbn.getNotification());
                mNotificationsView.addView(inv);
            }
        }
    }

    private boolean shouldShowNotification(StatusBarNotification sbn) {
        return !FlipFlapUtils.OUR_PACKAGE_NAME.equals(sbn.getPackageName());
    }
}
