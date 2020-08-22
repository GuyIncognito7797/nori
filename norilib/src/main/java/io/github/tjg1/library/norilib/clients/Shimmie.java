/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Client for the Shimmie2 API.
 * Shimmie2 provides an extension that enables a Danbooru 1.x-based API.
 */
public class Shimmie extends DanbooruLegacy {

  //region Constructors
  public Shimmie(Context context, String name, String endpoint) {
    super(context, name, endpoint);
  }

  public Shimmie(Context context, String name, String endpoint, String username, String password) {
    super(context, name, endpoint, username, password);
  }
  //endregion

  //region Service detection
  /**
   * Checks if the given URL exposes a supported API endpoint.
   *
   * @param context Android {@link Context}.
   * @param uri     URL to test.
   * @param timeout Timeout in milliseconds.
   * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
   */
  @Nullable
  public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
    final String endpointUrl = Uri.withAppendedPath(uri, "/api/danbooru/find_posts/index.xml")
        .toString();

    try {
      final Response<DataEmitter> response = Ion.with(context)
          .load(endpointUrl)
          .setTimeout(timeout)
          .userAgent(SearchClient.USER_AGENT)
          .followRedirect(false)
          .noCache()
          .asDataEmitter()
          .withResponse()
          .get();

      // Close the connection.
      final DataEmitter dataEmitter = response.getResult();
      if (dataEmitter != null) dataEmitter.close();

      if (response.getHeaders().code() == 200) {
        return uri.toString();
      }
    } catch (InterruptedException | ExecutionException ignored) {
    }
    return null;
  }
  //endregion

  //region SearchClient methods
  @Override
  public Settings getSettings() {
    return new Settings(Settings.APIType.SHIMMIE, name, apiEndpoint, username, password);
  }
  //endregion

  //region Creating search URLs
  @Override
  protected String createSearchURL(String tags, int pid, int limit) {
    // Page numbers are 1-indexed for this api.
    final int page = pid + 1;
    return String.format(
            Locale.US, "%s/api/danbooru/find_posts/index.xml?tags=%s&page=%d&limit=%d",
            apiEndpoint,
            tags.replace(" ", "%20"),
            page,
            limit
    );
  }
  //endregion

  //region Parsing responses
  @Override
  protected String webUrlFromId(String id) {
    return String.format(Locale.US, "%s/post/view/%s", apiEndpoint, id);
  }
  //endregion
}
