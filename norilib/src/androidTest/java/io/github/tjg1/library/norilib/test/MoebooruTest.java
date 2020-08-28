/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;

import io.github.tjg1.library.norilib.clients.DanbooruLegacy;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Tests support for Moebooru-based boards in the DanbooruLegacy client.
 */
public class MoebooruTest extends SearchClientTestCase {

    @Override
    protected SearchClient createSearchClient() {
        return new DanbooruLegacy(getInstrumentation().getContext(), "yande.re", "https://yande.re");
    }

    @Override
    protected String getDefaultTag() {
        return "hatsune_miku";
    }
}
