/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.service;

import android.app.IntentService;
import android.content.Intent;

import io.github.tjg1.nori.database.SearchSuggestionDatabase;

/**
 * Service used by {@link io.github.tjg1.nori.SettingsActivity} to remove all recent search history entries stored in
 * {@link io.github.tjg1.nori.database.SearchSuggestionDatabase}.
 */
public class ClearSearchHistoryService extends IntentService {

    //region Constructors
    public ClearSearchHistoryService() {
        // Set service name (useful for debugging).
        super("ClearSearchHistoryService");
    }
    //endregion

    //region IntentService methods (onHandleIntent)
    @Override
    protected void onHandleIntent(Intent intent) {
        // Open the search suggestion database
        SearchSuggestionDatabase db = new SearchSuggestionDatabase(this);

        // Remove recent search history entries.
        db.eraseSearchHistory();

        // Close the database resource.
        db.close();
    }
    //endregion
}
