/*
 * SPDX-FileCopyrightText: 2017-2021 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ClockPanel extends LinearLayout {
    private static final String TAG = "ClockPanel";

    public ClockPanel(Context context) {
        this(context, null);
    }

    public ClockPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClockPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
