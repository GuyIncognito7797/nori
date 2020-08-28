/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;

import io.github.tjg1.library.norilib.clients.Danbooru;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Tests for the Danbooru 2.x API.
 */
public class DanbooruTests extends SearchClientTestCase {
    // TODO: Test API key authentication.

    @Override
    protected SearchClient createSearchClient() {
        return new Danbooru(getInstrumentation().getContext(),
                "Danbooru", "https://danbooru.donmai.us");
    }

    @Override
    protected String getDefaultTag() {
        return "blonde_hair";
    }
}
