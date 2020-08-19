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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/**
 * Client for the Gelbooru API.
 * The Gelbooru API is based on the Danbooru 1.x API with a few minor differences.
 */
public class Gelbooru extends DanbooruLegacy {
  //region Constants
  /** Date format used by Gelbooru. */
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US);
  //endregion

  //region Constructors
  public Gelbooru(Context context, String name, String endpoint) {
    super(context, name, endpoint);
  }

  public Gelbooru(Context context, String name, String endpoint, String username, String password) {
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
    final String endpointUrl = Uri.withAppendedPath(uri, "/index.php?page=dapi&s=post&q=index")
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
    return new Settings(Settings.APIType.GELBOARD, name, apiEndpoint, username, password);
  }
  //endregion

  //region Creating search URLs
  @Override
  protected String createSearchURL(String tags, int pid, int limit) {
    // Unlike DanbooruLegacy, page numbers are 0-indexed for Gelbooru APIs.
    return String.format(Locale.US, "%s/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&limit=%d", apiEndpoint, Uri.encode(tags), pid, limit);
  }
  //endregion

  //region Parsing responses
  @Override
  protected String webUrlFromId(String id) {
    return String.format(Locale.US, "%s/index.php?page=post&s=view&id=%s", apiEndpoint, id);
  }

  @Override
  protected Date dateFromString(String date) throws ParseException {
    // Override Danbooru 1.x date format.
    return DATE_FORMAT.parse(date);
  }
  //endregion
}
