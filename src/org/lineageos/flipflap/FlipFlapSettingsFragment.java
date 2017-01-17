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

import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.PreferenceFragment;
import android.provider.Settings;
import android.util.Log;

import org.lineageos.flipflap.R;

public class FlipFlapSettingsFragment extends PreferenceFragment {

    public final String TAG = "FlipFlapSettings";

    ListPreference mUnpluggedTimeout;
    ListPreference mPluggedTimeout;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.flipflapsettings_panel);

        mPluggedTimeout = (ListPreference) findPreference(FlipFlapUtils.KEY_TIMEOUT_PLUGGED);
        mPluggedTimeout.setOnPreferenceChangeListener(mChangeListener);
        mUnpluggedTimeout = (ListPreference) findPreference(FlipFlapUtils.KEY_TIMEOUT_UNPLUGGED);
        mUnpluggedTimeout.setOnPreferenceChangeListener(mChangeListener);

        setTimeoutSummary(mPluggedTimeout, 0);
        setTimeoutSummary(mUnpluggedTimeout, 0);
    }

    Preference.OnPreferenceChangeListener mChangeListener =
            new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String value = (String) newValue;
            String key = preference.getKey();
            Log.d(TAG, "Preference changed: " + key + ": " + value);

            switch (key) {
                case FlipFlapUtils.KEY_TIMEOUT_PLUGGED:
                case FlipFlapUtils.KEY_TIMEOUT_UNPLUGGED:
                    setTimeoutSummary(preference, Integer.parseInt(value));
                    return true;

                default:
                    return true;

            }
        }
    };

    private void setTimeoutSummary(Preference pref, int timeOut) {
        timeOut = (timeOut != 0) ? timeOut : FlipFlapUtils.getTimeout(getContext(), pref.getKey());

        String[] entries = getResources().getStringArray(R.array.timeout_entries);
        int index =  mPluggedTimeout.findIndexOfValue(String.valueOf(timeOut));

        if (timeOut == -1) {
            pref.setSummary(R.string.timeout_summary_never);
        } else {
            pref.setSummary(getString(R.string.timeout_summary, entries[index]));
        }
    }
}

