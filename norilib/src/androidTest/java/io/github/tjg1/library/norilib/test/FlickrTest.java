/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.library.norilib.test;

import io.github.tjg1.library.norilib.clients.Flickr;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Tests for the Flickr SearchClient.
 */
public class FlickrTest extends SearchClientTestCase {
    @Override
    protected SearchClient createSearchClient() {
        return new Flickr(getInstrumentation().getContext(), "Flickr", Flickr.FLICKR_API_ENDPOINT.toString());
    }

    /**
     * @return Tag to search for while testing the support of this API.
     */
    @Override
    protected String getDefaultTag() {
        return "duck";
    }
}
