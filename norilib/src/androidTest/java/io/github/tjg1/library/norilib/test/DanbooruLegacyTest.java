/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;

/**
 * Tests for the Danbooru 1.x API client.
 */
public class DanbooruLegacyTest /* extends SearchClientTestCase */ {
    // TODO: Test Basic Auth authentication
    // Disable Danbooru Legacy tests as I can't think of a site still exposing this
    // deprecated API
  /*
  @Override
  protected SearchClient createSearchClient() {
    return new DanbooruLegacy(getInstrumentation().getContext(),
        "Danbooru", "https://danbooru.donmai.us");
  }

  @Override
  protected String getDefaultTag() {
    return "blonde_hair";
  }
  */
}