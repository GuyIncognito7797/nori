/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.tjg1.nori.adapter.SafeSearchListAdapter;
import io.github.tjg1.nori.util.StringUtils;

public class SafeSearchSettingsActivity extends AppCompatActivity
        implements SafeSearchListAdapter.Listener {

    //region Instance fields
    /**
     * Default {@link android.content.SharedPreferences} object.
     */
    private SharedPreferences sharedPreferences;
    /**
     * Human-readable labels for each SafeSearch setting.
     */
    private String[] safeSearchEntries;
    /**
     * Human-readable summaries for each SafeSearch setting.
     */
    private String[] safeSearchSummaries;
    /**
     * Values for each SafeSearch setting stored in {@link android.content.SharedPreferences}
     */
    private String[] safeSearchValues;
    /**
     * Current values of the preference_safeSearch preference.
     */
    private List<String> safeSearchCurrentSetting;
    //endregion

    //region Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_safe_search_settings);

        // Set Toolbar as the app bar.
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region Activity methods (Setters)
    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        super.setContentView(layoutResID);

        if (BuildConfig.FLAVOR.equals("google")) {
            final RadioGroup safeSearchGroup = (RadioGroup) findViewById(R.id.safe_search_radio_group);
            final CheckBox undefinedCheckBox = (CheckBox) findViewById(R.id.safe_search_undefined);
            final SafeSearchCheckedChangeListener listener =
                    new SafeSearchCheckedChangeListener(safeSearchGroup, undefinedCheckBox);
            listener.updateViews();

            // Set View listeners.
            safeSearchGroup.setOnCheckedChangeListener(listener);
            undefinedCheckBox.setOnCheckedChangeListener(listener);
        } else {
            // Set up ListView.
            final ListView listView = (ListView) findViewById(android.R.id.list);
            listView.setAdapter(new SafeSearchListAdapter(this, this));
            listView.setOnItemClickListener((AdapterView.OnItemClickListener) (adapterView, view, i, l) -> {
                final Checkable checkBox = (Checkable) view.findViewById(R.id.checkbox);
                checkBox.toggle();
            });
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);

        // Hide the action bar icon and use the activity title as the home button.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    //endregion

    //region SafeSearchListAdapter.Listener methods (getting & updating preference values)

    /**
     * Get the human-readable labels for each SafeSearch setting.
     */
    @NonNull
    @Override
    public String[] getSafeSearchEntries() {
        if (safeSearchEntries == null) {
            safeSearchEntries = getResources().getStringArray(R.array.preference_safeSearch_entries);
        }
        return safeSearchEntries;
    }

    /**
     * Get the human-readable summaries for each SafeSearch setting.
     */
    @NonNull
    @Override
    public String[] getSafeSearchSummaries() {
        if (safeSearchSummaries == null) {
            safeSearchSummaries = getResources().getStringArray(R.array.preference_safeSearch_summaries);
        }
        return safeSearchSummaries;
    }

    /**
     * Get values for each SafeSearch setting stored in {@link android.content.SharedPreferences}
     */
    @NonNull
    @Override
    public String[] getSafeSearchValues() {
        if (safeSearchValues == null) {
            safeSearchValues = getResources().getStringArray(R.array.preference_safeSearch_values);
        }
        return safeSearchValues;
    }

    /**
     * Get current values of the preference_safeSearch preference.
     */
    @NonNull
    @Override
    public List<String> getSafeSearchCurrentSetting() {
        if (safeSearchCurrentSetting == null) {
            safeSearchCurrentSetting = new ArrayList<>(4);

            String safeSearchPreference = getSharedPreferences()
                    .getString(getString(R.string.preference_safeSearch_key), null);

            if (safeSearchPreference != null && !TextUtils.isEmpty(safeSearchPreference.trim())) {
                safeSearchCurrentSetting
                        .addAll(Arrays.asList(safeSearchPreference.trim().split(" ")));
            } else {
                safeSearchCurrentSetting.addAll(Arrays.asList(getResources()
                        .getStringArray(R.array.preference_safeSearch_defaultValues)));
            }
        }
        return safeSearchCurrentSetting;
    }

    /**
     * Update the value of the preference_safeSearch shared preference.
     */
    @Override
    public void updateSafeSearchSettings(@NonNull String[] safeSearchCurrentSetting) {
        getSharedPreferences().edit()
                .putString(getString(R.string.preference_safeSearch_key),
                        StringUtils.mergeStringArray(safeSearchCurrentSetting, " ").trim())
                .apply();
    }
    //endregion

    //region Getters & Setters

    /**
     * Get the default {@link android.content.SharedPreferences} object.
     */
    @NonNull
    private SharedPreferences getSharedPreferences() {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        }
        return sharedPreferences;
    }
    //endregion

    //region Inner class: Radio group/button listener

    /**
     * {@link android.widget.CompoundButton.OnCheckedChangeListener} for the Google version.
     */
    private class SafeSearchCheckedChangeListener implements
            CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener {

        //region Instance fields
        /**
         * SafeSearch filter setting {@link RadioGroup}.
         */
        private final RadioGroup safeSearchGroup;
        /**
         * Display images with undefined SafeSearch rating, if checked.
         */
        private final CheckBox undefinedCheckBox;
        //endregion

        //region Constructors

        /**
         * Create a new listener for SafeSearch setting {@link RadioGroup} and {@link CheckBox}.
         *
         * @param safeSearchGroup   SafeSearch filter setting {@link RadioGroup}.
         * @param undefinedCheckBox Display images with undefined SafeSearch rating, if checked.
         */
        public SafeSearchCheckedChangeListener(@NonNull RadioGroup safeSearchGroup,
                                               @NonNull CheckBox undefinedCheckBox) {
            this.safeSearchGroup = safeSearchGroup;
            this.undefinedCheckBox = undefinedCheckBox;
        }
        //endregion

        //region CompoundButton.OnCheckedChangeListener
        @Override
        public void onCheckedChanged(CompoundButton button, boolean checked) {
            updatePreferences(safeSearchGroup.getCheckedRadioButtonId(), checked);
        }
        //endregion

        //region RadioGroup.OnCheckedChangeListener
        @Override
        public void onCheckedChanged(RadioGroup radioGroup, int position) {
            updatePreferences(radioGroup.getCheckedRadioButtonId(), undefinedCheckBox.isChecked());
        }
        //endregion

        //region Instance fields (Public)

        /**
         * Update the SafeSearch {@link RadioGroup} and {@link CheckBox} with current preference values.
         */
        public void updateViews() {
            final List<String> currentSetting = getSafeSearchCurrentSetting();

            // Select radio group item based on current setting.
            if (currentSetting.contains("x")) { // eXplicit
                this.safeSearchGroup.check(R.id.safe_search_off);
            } else if (currentSetting.contains("q")) { // Questionable
                this.safeSearchGroup.check(R.id.safe_search_moderate);
            } else if (currentSetting.contains("f")) { // saFe
                this.safeSearchGroup.check(R.id.safe_search_on);
            }

            // Check undefined check box based on current setting.
            this.undefinedCheckBox.setChecked(currentSetting.contains("u"));
        }
        //endregion

        //region Instance fields (Private)

        /**
         * Update {@link SharedPreferences} with the selected SafeSearch setting.
         *
         * @param safeSearchCheckedRadioButtonId Selected SafeSearch radio group item ID.
         * @param undefinedChecked               Show items with undefined SafeSearch ratings.
         */
        private void updatePreferences(int safeSearchCheckedRadioButtonId, boolean undefinedChecked) {
            // Set the show items with undefined SafeSearch ratings setting.
            String[] safeSearchSettings = undefinedChecked ? new String[]{null, "u"} : new String[1];

            // Set SearchClient setting from radio group selected item id.
            switch (safeSearchCheckedRadioButtonId) {
                case R.id.safe_search_moderate:
                    safeSearchSettings[0] = "f q";
                    break;
                case R.id.safe_search_off:
                    safeSearchSettings[0] = "f q x";
                    break;
                default:
                    safeSearchSettings[0] = "f";
                    break;
            }

            // Update SharedPreferences.
            updateSafeSearchSettings(safeSearchSettings);
        }
        //endregion
    }
    //endregion
}
