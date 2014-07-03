/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package com.cuddlesoft.nori.test;

import com.cuddlesoft.nori.api.clients.Danbooru;
import com.cuddlesoft.nori.api.clients.SearchClient;

/**
 * Tests for the Danbooru 2.x API.
 */
public class DanbooruTests extends SearchClientTestCase {
  // TODO: Test API key authentication.

  @Override
  protected SearchClient createSearchClient() {
    return new Danbooru("http://danbooru.donmai.us");
  }
}
