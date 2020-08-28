/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import io.github.tjg1.nori.R;

;

/**
 * Adapter for the SafeSearch setting List (in the fdroid version)
 */
public class SafeSearchListAdapter extends BaseAdapter implements
        CompoundButton.OnCheckedChangeListener {

    //region Instance fields
    /**
     * Android context the adapter is used in.
     */
    private final Context context;
    /**
     * Listener used to interact with the {@link android.app.Activity} using this Adapter.
     */
    private final SafeSearchListAdapter.Listener listener;
    //endregion

    //region Constructors

    /**
     * Create a new Adapter used to populate a ListView with safe search settings.
     *
     * @param context  Android context the adapter is used in.
     * @param listener Listener used to interact with the {@link android.app.Activity} using this
     *                 Adapter.
     */
    public SafeSearchListAdapter(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;
    }
    //endregion

    //region BaseAdapter methods
    @Override
    public int getCount() {
        return listener.getSafeSearchEntries().length;
    }

    @Override
    public Object getItem(int position) {
        return listener.getSafeSearchEntries()[position];
    }

    @Override
    public long getItemId(int position) {
        // Position == item id.
        return position;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
        // Recycle view if possible.
        View view = recycledView;
        if (view == null) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            view = inflater.inflate(R.layout.listitem_safesearch_rating, container, false);
        }

        // Populate views.
        final CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkbox);
        checkBox.setChecked(listener.getSafeSearchCurrentSetting().contains(listener.getSafeSearchValues()[position]));
        checkBox.setOnCheckedChangeListener(this);
        checkBox.setTag(listener.getSafeSearchValues()[position]);
        final TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(listener.getSafeSearchEntries()[position]);
        final TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setText(listener.getSafeSearchSummaries()[position]);

        return view;
    }
    //endregion

    //region CompoundButton.OnCheckedChangeListener
    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        List<String> safeSearchCurrentSetting = listener.getSafeSearchCurrentSetting();

        if (checked && !safeSearchCurrentSetting.contains(compoundButton.getTag())) {
            safeSearchCurrentSetting.add((String) compoundButton.getTag());
        } else if (!checked && safeSearchCurrentSetting.contains(compoundButton.getTag())) {
            safeSearchCurrentSetting.remove(compoundButton.getTag());
        }

        // Update SharedPreferences.
        listener.updateSafeSearchSettings(safeSearchCurrentSetting
                .toArray(new String[safeSearchCurrentSetting.size()]));
    }
    //endregion

    //region Listener interface

    /**
     * Listener used to interact with the {@link android.app.Activity} using this Adapter.
     */
    public interface Listener {

        /**
         * Get the human-readable labels for each SafeSearch setting.
         */
        public String[] getSafeSearchEntries();

        /**
         * Get the human-readable summaries for each SafeSearch setting.
         */
        public String[] getSafeSearchSummaries();

        /**
         * Get values for each SafeSearch setting stored in {@link android.content.SharedPreferences}
         */
        public String[] getSafeSearchValues();

        /**
         * Get current values of the preference_safeSearch preference.
         */
        public List<String> getSafeSearchCurrentSetting();

        /**
         * Update the value of the preference_safeSearch shared preference.
         */
        public void updateSafeSearchSettings(@NonNull String[] safeSearchCurrentSetting);
    }
    //endregion
}