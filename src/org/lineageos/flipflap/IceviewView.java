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
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import android.util.Log;

import java.util.List;

public class IceviewView extends FlipFlapView {
    private static final String TAG = "IceviewView";

    private ClockPanel mClockPanel;
    private List<StatusBarNotification> mNotifications;
    private final ArrayAdapter<StatusBarNotification> mNotificationsAdapter;

    public IceviewView(Context context) {
        super(context);

        inflate(mContext, R.layout.iceview_view, this);

        mClockPanel = (ClockPanel) findViewById(R.id.clock_panel);
        mClockPanel.bringToFront();

        mNotificationsAdapter = new ArrayAdapter<StatusBarNotification>(context, 0) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = inflate(mContext, R.layout.notification_item, null);
                }
                populateNotificationView(convertView, getItem(position));
                return convertView;
            }
        };

        ListView notificationsList = (ListView) findViewById(R.id.iceview_notifications);
        notificationsList.setAdapter(mNotificationsAdapter);
    }

    @Override
    public boolean supportsNotifications() {
        return true;
    }

    @Override
    public void updateNotifications(List<StatusBarNotification> notifications) {
Log.e(TAG, "got new notifications: " + notifications);
        mNotifications = notifications;
        mNotificationsAdapter.clear();
        mNotificationsAdapter.addAll(notifications);
        mNotificationsAdapter.notifyDataSetChanged();
    }

    private void populateNotificationView(View view, StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        TextView title = (TextView) view.findViewById(R.id.notification_title);
        title.setText(notification.extras.getString(Notification.EXTRA_TITLE));
Log.e(TAG, "notification: " + notification.extras.getString(Notification.EXTRA_TITLE));
    }
}
