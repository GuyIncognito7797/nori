/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;


import android.os.Bundle;
import android.test.InstrumentationTestCase;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.clients.SearchClient;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Extend this class to test a class implementing the {@link io.github.tjg1.library.norilib.clients.SearchClient} API.
 */
public abstract class SearchClientTestCase extends InstrumentationTestCase {

    public void testSearchUsingTags() throws Throwable {
        // TODO: Ideally this should be mocked, so testing doesn't rely on external APIs.
        // Create a new client connected to the Danbooru API.
        final SearchClient client = createSearchClient();
        // Retrieve a search result.
        final SearchResult result = client.search(getDefaultTag());

        // Make sure we got results back.
        assertThat(result.getImages()).isNotEmpty();
        // Verify metadata for each returned image.
        for (Image image : result.getImages()) {
            ImageTests.verifyImage(image);
        }

        // Check rests of the values.
        assertThat(result.getCurrentOffset()).isEqualTo(0);
        assertThat(result.getQuery()).hasSize(1);
        assertThat(result.getQuery()[0].getName()).isEqualTo(getDefaultTag());
        assertThat(result.hasNextPage()).isTrue();
    }

    public void testSearchUsingTagsAndOffset() throws Throwable {
        // TODO: Ideally this should be mocked, so testing doesn't rely on external APIs.
        // Create a new client connected to the Danbooru API.
        final SearchClient client = createSearchClient();
        // Retrieve search results.
        final SearchResult page1 = client.search(getDefaultTag(), 0);
        final SearchResult page2 = client.search(getDefaultTag(), 1);

        // Make sure that the results differ.
        assertThat(page1.getImages()).isNotEmpty();
        assertThat(page2.getImages()).isNotEmpty();
        assertThat(page2.getCurrentOffset()).isEqualTo(1);
        assertThat(page1.getImages()[0].id).isNotEqualTo(page2.getImages()[0].id);
        assertThat(page1.getImages()[0].searchPage).isEqualTo(page1.getCurrentOffset());
        assertThat(page1.getImages()[0].searchPagePosition).isEqualTo(0);
        assertThat(page1.getImages()[1].searchPagePosition).isEqualTo(1);
        assertThat(page2.getImages()[0].searchPage).isEqualTo(page2.getCurrentOffset());
        assertThat(page2.getImages()[0].searchPagePosition).isEqualTo(0);
        assertThat(page2.getImages()[1].searchPagePosition).isEqualTo(1);
    }

    /**
     * Test asynchronous search requests
     */
    public void testSearchAsync() throws Throwable {
        // Create a lock to wait for the async request to finish.
        final CountDownLatch lock = new CountDownLatch(1);
        // Hold on to errors and SearchResult returned in the SearchCallback.
        // One-element arrays are a hack used to set values from outside the main thread.
        final IOException[] error = new IOException[1];
        final SearchResult[] searchResults = new SearchResult[1];

        // Run search requests on the UI thread.
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                final SearchClient client = createSearchClient();
                // Retrieve a search result.
                client.search(getDefaultTag(), new SearchClient.SearchCallback() {
                    @Override
                    public void onFailure(IOException e) {
                        error[0] = e;
                        // Clear the lock.
                        lock.countDown();
                    }

                    @Override
                    public void onSuccess(SearchResult searchResult) {
                        searchResults[0] = searchResult;
                        // Clear the lock.
                        lock.countDown();
                    }
                });
            }
        });

        // Wait 30 seconds for the async response.
        lock.await(30, TimeUnit.SECONDS);
        // If the callback received an error, throw it and mark the test as failed.
        if (error[0] != null) {
            throw error[0];
        }
        // Make sure a SearchResult was returned.
        assertThat(searchResults[0]).isNotNull();
        assertThat(searchResults[0].getImages()).isNotEmpty();
    }

    public void testGetDefaultQuery() throws Throwable {
        final SearchClient client = createSearchClient();
        assertThat(client.getDefaultQuery()).isNotNull();
    }

    public void testRequiredAuthentication() throws Throwable {
        final SearchClient client = createSearchClient();
        assertThat(client.getDefaultQuery()).isNotNull();
    }

    public void testGetSettings() throws Throwable {
        SearchClient client = createSearchClient();
        SearchClient.Settings settings = client.getSettings();
        assertThat(settings).isNotNull();

        // Parcel the settings object and recreate it from the bundle.
        final Bundle bundle = new Bundle();
        bundle.putParcelable("settings", settings);
        settings = bundle.getParcelable("settings");

        // Recreate SearchClient from the settings object and test it.
        client = settings.createSearchClient(getInstrumentation().getContext());
        assertThat(client).isInstanceOf(createSearchClient().getClass());
    }

    protected abstract SearchClient createSearchClient();

    /**
     * @return Tag to search for while testing the support of this API.
     */
    protected abstract String getDefaultTag();

}
