/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package com.cuddlesoft.nori.test;

import com.cuddlesoft.nori.api.clients.SearchClient;
import com.cuddlesoft.nori.api.clients.Shimmie;

/**
 * Tests for the Shimmie2 API client.
 */
public class ShimmieTests extends SearchClientTestCase {

  @Override
  protected SearchClient createSearchClient() {
    return new Shimmie("http://dollbooru.org");
  }
}
