/*
 * SPDX-FileCopyrightText: 2017 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class AlarmPanel extends RelativeLayout {
    private static final String TAG = "AlarmPanel";

    public AlarmPanel(Context context) {
        this(context, null);
    }

    public AlarmPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AlarmPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
