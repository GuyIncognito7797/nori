package io.github.tjg1.library.norilib.test;

import io.github.tjg1.library.norilib.clients.E621Legacy;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Tests for the E621 client.
 */
public class E621Test extends SearchClientTestCase {

    @Override
    protected SearchClient createSearchClient() {
        return new E621Legacy(getInstrumentation().getContext(), "E926", "https://e926.net");
    }

    @Override
    protected String getDefaultTag() {
        return "blonde_hair";
    }
}
