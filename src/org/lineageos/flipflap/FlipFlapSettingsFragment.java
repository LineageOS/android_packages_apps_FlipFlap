/*
 * Copyright (C) 2017-2019 The LineageOS Project
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

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v14.preference.PreferenceFragment;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.lineageos.flipflap.R;

public class FlipFlapSettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, CompoundButton.OnCheckedChangeListener {

    public final String TAG = "FlipFlapSettings";

    private final String KEY_BEHAVIOUR_CATEGORY = "category_behaviour";
    private final String KEY_DESIGN_CATEGORY = "category_design";
    private final String KEY_TOUCH_SENSITIVITY = "use_high_touch_sensitivity";

    private TextView mTextView;
    private View mSwitchBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup frame =
                (ViewGroup) inflater.inflate(R.layout.flipflap_settings, container, false);
        final View content = super.onCreateView(inflater, frame, savedInstanceState);
        frame.addView(content);
        return frame;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSwitchBar = view.findViewById(R.id.switch_bar);
        Switch switchWidget = mSwitchBar.findViewById(android.R.id.switch_widget);
        switchWidget.setChecked(isEventReceiverEnabled());
        switchWidget.setOnCheckedChangeListener(this);
        mSwitchBar.setOnClickListener(v -> {
            switchWidget.setChecked(!switchWidget.isChecked());
            mSwitchBar.setActivated(switchWidget.isChecked());
        });

        mTextView = (TextView) view.findViewById(R.id.switch_text);

        updateEnableStates(switchWidget.isChecked());
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.flipflapsettings_panel);
        final ActionBar actionBar = getActivity().getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        PreferenceCategory behaviourCategory =
                (PreferenceCategory) getPreferenceScreen().findPreference(KEY_BEHAVIOUR_CATEGORY);
        PreferenceCategory designCategory =
                (PreferenceCategory) getPreferenceScreen().findPreference(KEY_DESIGN_CATEGORY);

        Preference touchSensitivityPref = (SwitchPreference) findPreference(KEY_TOUCH_SENSITIVITY);

        setupTimeoutPreference(FlipFlapUtils.KEY_TIMEOUT_PLUGGED);
        setupTimeoutPreference(FlipFlapUtils.KEY_TIMEOUT_UNPLUGGED);

        int cover = FlipFlapUtils.getCoverStyle(getActivity());
        if (!FlipFlapUtils.showsChargingStatus(cover)) {
            getPreferenceScreen().removePreference(designCategory);
        }
        if (!FlipFlapUtils.getHighTouchSensitivitySupported(getContext())) {
            behaviourCategory.removePreference(touchSensitivityPref);
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
    public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
        ComponentName cn = new ComponentName(getContext(), EventReceiver.class);
        int state = enabled
                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        getContext().getPackageManager().setComponentEnabledSetting(cn, state,
                PackageManager.DONT_KILL_APP);

        updateEnableStates(enabled);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    private void setupTimeoutPreference(String key) {
        ListPreference list = (ListPreference) findPreference(key);
        list.setOnPreferenceChangeListener(this);
        setTimeoutSummary(list, FlipFlapUtils.getTimeout(getActivity(), key));
    }

    private void updateEnableStates(boolean masterSwitchEnabled) {
        PreferenceScreen ps = getPreferenceScreen();
        int count = ps.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            ps.getPreference(i).setEnabled(masterSwitchEnabled);
        }

        mTextView.setText(getString(masterSwitchEnabled ?
                R.string.switch_bar_on : R.string.switch_bar_off));
        mSwitchBar.setActivated(masterSwitchEnabled);
    }

    private boolean isEventReceiverEnabled() {
        ComponentName cn = new ComponentName(getContext(), EventReceiver.class);
        int state = getContext().getPackageManager().getComponentEnabledSetting(cn);
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }

    private void setTimeoutSummary(Preference pref, int timeOut) {
        pref.setSummary(timeOut < 0
                ? R.string.timeout_summary_never
                : timeOut == 0
                    ? R.string.timeout_summary_immediately
                    : R.string.timeout_summary);
    }
}
