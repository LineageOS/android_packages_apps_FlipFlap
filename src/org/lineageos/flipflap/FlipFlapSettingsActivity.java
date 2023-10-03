/*
 * SPDX-FileCopyrightText: 2017,2022 The LineageOS Project
 * SPDX-License-Identifier: GPL-2.0-or-later
 */

package org.lineageos.flipflap;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.widget.R;

public class FlipFlapSettingsActivity extends CollapsingToolbarBaseActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(R.id.content_frame,
                new FlipFlapSettingsFragment()).commit();
    }
}
