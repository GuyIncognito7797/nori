/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.library.norilib.test;

import io.github.tjg1.library.norilib.clients.FlickrUser;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Tests for the FlickrUser SearchClient.
 */
public class FlickrUserTest extends SearchClientTestCase {
    @Override
    protected SearchClient createSearchClient() {
        return new FlickrUser(getInstrumentation().getContext(), "me", "https://www.flickr.com/photos/128962151@N05/");
    }

    /**
     * @return Tag to search for while testing the support of this API.
     */
    @Override
    protected String getDefaultTag() {
        return "_";
    }
}
