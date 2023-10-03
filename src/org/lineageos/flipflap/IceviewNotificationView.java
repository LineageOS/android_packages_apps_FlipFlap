/*
 * SPDX-FileCopyrightText: 2017-2021 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.app.Notification;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class IceviewNotificationView extends LinearLayout {
    private static final String TAG = "IceviewNotificationView";

    public IceviewNotificationView(Context context) {
        this(context, null);
    }

    public IceviewNotificationView(Context context, AttributeSet attrSet) {
        super(context, attrSet);
    }

    public void setNotification(Notification notification) {
        Icon largeIcon = notification.getLargeIcon();
        Icon icon = largeIcon != null ? largeIcon : notification.getSmallIcon();
        String titleString = notification.extras.getString(Notification.EXTRA_TITLE);
        String textString = notification.extras.getString(Notification.EXTRA_TEXT);

        ImageView iconView = findViewById(R.id.iceview_notification_icon);
        TextView titleView = findViewById(R.id.iceview_notification_title);
        TextView descView = findViewById(R.id.iceview_notification_description);

        titleView.setText(titleString);
        descView.setText(textString);
        if (icon != null) {
            Drawable drawable = icon.loadDrawable(mContext);
            iconView.setImageDrawable(drawable);
        }
    }
}
