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

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.List;
import android.util.Log;

public class IceviewView extends FlipFlapView {
    private static final String TAG = "IceviewView";

    private ClockPanel mClockPanel;
    private LinearLayout mNotificationsView;

    public IceviewView(Context context) {
        super(context);

        inflate(mContext, R.layout.iceview_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();

        mNotificationsView = (LinearLayout) findViewById(R.id.iceview_notifications);
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
                IceviewNotificationView inv = (IceviewNotificationView) inflate(mContext,
                        R.layout.iceview_notification_view, null);
                inv.setNotification(sbn.getNotification());
                mNotificationsView.addView(inv);
            }
        }
    }

    private boolean shouldShowNotification(StatusBarNotification sbn) {
        if (FlipFlapUtils.OUR_PACKAGE_NAME.equals(sbn.getPackageName())) {
             return false;
        }
        return true;
    }
}
