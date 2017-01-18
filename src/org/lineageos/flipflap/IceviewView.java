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
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

import java.util.List;

public class IceviewView extends FlipFlapView {
    private static final String TAG = "IceviewView";

    private ClockPanel mClockPanel;
    private final NotificationsAdapter mNotificationsAdapter;

    public IceviewView(Context context) {
        super(context);

        inflate(mContext, R.layout.iceview_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();

        // If a call is ringing and the cover is closed, hide ourselves to let the user answer it
        TelephonyManager tm = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        if (tm.getCallState() == TelephonyManager.CALL_STATE_RINGING) {
            setVisibility(View.INVISIBLE);
        }

        mNotificationsAdapter = new NotificationsAdapter(context);
        ListView notificationsList = (ListView) findViewById(R.id.iceview_notifications);
        notificationsList.setAdapter(mNotificationsAdapter);
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
        mNotificationsAdapter.setNotifyOnChange(false);
        mNotificationsAdapter.clear();
        mNotificationsAdapter.addAll(notifications);
        mNotificationsAdapter.notifyDataSetChanged();
    }

    private static class NotificationsAdapter extends ArrayAdapter<StatusBarNotification> {
        private LayoutInflater mInflater;

        public NotificationsAdapter(Context context) {
            super(context, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.notification_item, parent, false);
            }

            Notification notification = getItem(position).getNotification();
            TextView title = (TextView) convertView.findViewById(R.id.notification_title);
            title.setText(notification.extras.getString(Notification.EXTRA_TITLE));

            return convertView;
        }
    }
}
