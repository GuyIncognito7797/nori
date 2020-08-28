/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;

import io.github.tjg1.library.norilib.clients.Gelbooru;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Tests for the Gelbooru API client.
 */
public class GelbooruTest extends SearchClientTestCase {

    @Override
    protected SearchClient createSearchClient() {
        return new Gelbooru(getInstrumentation().getContext(), "Gelbooru", "http://gelbooru.com");
    }

    @Override
    protected String getDefaultTag() {
        return "blonde_hair";
    }
}
