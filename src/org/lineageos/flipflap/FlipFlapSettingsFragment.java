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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.lineageos.flipflap.R;

public class FlipFlapSettingsFragment extends PreferenceFragment
        implements OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    public final String TAG = "FlipFlapSettings";

    private final String KEY_DESIGN_CATEGORY = "category_design";
    private final String KEY_MASTER_SWITCH = "master_switch";
    private final String KEY_PASS_TO_SECURITY = "pass_to_security_view";

    private Switch mSwitch;

    private SwitchPreference mPassSecurity;
    private SwitchPreference mShowChargingStatus;
    private ListPreference mPluggedTimeout;
    private ListPreference mUnpluggedTimeout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = LayoutInflater.from(getContext()).inflate(R.layout.flipflap_settings, container, false);
        ((ViewGroup) view).addView(super.onCreateView(inflater, container, savedInstanceState));
               return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View switchBar = view.findViewById(R.id.switch_bar);
        mSwitch = (Switch) switchBar.findViewById(android.R.id.switch_widget);
        mSwitch.setChecked(FlipFlapUtils.getPreferences(getContext()).getBoolean(
                KEY_MASTER_SWITCH, true));
        mSwitch.setOnCheckedChangeListener(this);

        switchBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitch.setChecked(!mSwitch.isChecked());
            }
        });
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.flipflapsettings_panel);

        PreferenceScreen preferenceScreen = getPreferenceScreen();

        boolean flipflap_enabled = FlipFlapUtils.getPreferences(getContext()).getBoolean(
                KEY_MASTER_SWITCH, true);

        mPassSecurity = (SwitchPreference) findPreference(KEY_PASS_TO_SECURITY);
        mPassSecurity.setEnabled(flipflap_enabled);
        mShowChargingStatus = (SwitchPreference) findPreference(FlipFlapUtils.KEY_BATTERY_INDICATION);
        mShowChargingStatus.setEnabled(flipflap_enabled);

        mPluggedTimeout = (ListPreference) findPreference(FlipFlapUtils.KEY_TIMEOUT_PLUGGED);
        mPluggedTimeout.setOnPreferenceChangeListener(this);
        mPluggedTimeout.setEnabled(flipflap_enabled);
        mUnpluggedTimeout = (ListPreference) findPreference(FlipFlapUtils.KEY_TIMEOUT_UNPLUGGED);
        mUnpluggedTimeout.setOnPreferenceChangeListener(this);
        mUnpluggedTimeout.setEnabled(flipflap_enabled);

        setTimeoutSummary(mPluggedTimeout, FlipFlapUtils.getTimeout(getActivity(), true));
        setTimeoutSummary(mUnpluggedTimeout, FlipFlapUtils.getTimeout(getActivity(), false));

        int cover = FlipFlapUtils.getCoverStyle(getActivity());
        if (!FlipFlapUtils.showsChargingStatus(cover)) {
            PreferenceCategory designCategory = (PreferenceCategory)
                    findPreference(KEY_DESIGN_CATEGORY);
            preferenceScreen.removePreference(designCategory);
        }
    }

    @Override
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

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

        SharedPreferences sharedPref = FlipFlapUtils.getPreferences(getContext());
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(KEY_MASTER_SWITCH, b ? true : false);
        editor.commit();

        boolean enabled = FlipFlapUtils.getPreferences(getContext()).getBoolean(
                KEY_MASTER_SWITCH, true);

        mPassSecurity.setEnabled(enabled);
        mShowChargingStatus.setEnabled(enabled);
        mPluggedTimeout.setEnabled(enabled);
        mUnpluggedTimeout.setEnabled(enabled);
    }


    private void setTimeoutSummary(Preference pref, int timeOut) {
        pref.setSummary(timeOut < 0
                ? R.string.timeout_summary_never
                : timeOut == 0
                    ? R.string.timeout_summary_immediately
                    : R.string.timeout_summary);
    }
}
