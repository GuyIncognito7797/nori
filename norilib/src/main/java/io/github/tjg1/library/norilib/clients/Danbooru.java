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
import android.text.TextUtils;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * Client for the Danbooru 2.x API.
 */
public class Danbooru implements SearchClient {
  static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
  //region Constants
  /**
   * Number of images per search results page.
   * Best to use a large value to minimize number of unique HTTP requests.
   */
  private static final int DEFAULT_LIMIT = 100;
  /** Thumbnail size set if not returned by the API. */
  private static final int THUMBNAIL_SIZE = 150;
  /** Sample size set if not returned by the API. */
  private static final int SAMPLE_SIZE = 850;
  //endregion

  //region Service configuration instance fields
  /** Android context. */
  protected final Context context;
  /** Human-readable service name */
  protected final String name;
  /** URL to the HTTP API Endpoint - the server implementing the API. */
  protected final String apiEndpoint;
  /** Username used for authentication. (optional) */
  protected final String username;
  /** API key used for authentication. (optional) */
  protected final String apiKey;
  //endregion

  //region Constructors
  /**
   * Create a new Danbooru 2.x client without authentication.
   *
   * @param name     Human-readable service name.
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   */
  public Danbooru(Context context, String name, String endpoint) {
    this.context = context;
    this.name = name;
    this.apiEndpoint = endpoint;
    this.username = null;
    this.apiKey = null;
  }

  /**
   * Create a new Danbooru 1.x client with authentication.
   *
   * @param name     Human-readable service name.
   * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
   * @param username Username used for authentication.
   * @param apiKey   API key used for authentication.
   */
  public Danbooru(Context context, String name, String endpoint, String username, final String apiKey) {
    this.context = context;
    this.name = name;
    this.apiEndpoint = endpoint;
    this.username = username;
    this.apiKey = apiKey;
  }
  //endregion

  //region Service detection
  /**
   * Checks if the given URL exposes a supported API endpoint.
   *
   * @param context Android {@link Context}.
   * @param uri URL to test.
   * @param timeout Timeout in milliseconds.
   * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
   */
  @Nullable
  public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
    final String endpointUrl = Uri.withAppendedPath(uri, "/posts.xml").toString();

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
  public SearchResult search(String tags) throws IOException {
    // Return results for page 0.
    return search(tags, 0);
  }

  @Override
  public SearchResult search(String tags, int pid) throws IOException {
    try {
      return Ion.with(this.context)
          .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
          .userAgent(SearchClient.USER_AGENT)
          .as(new SearchResultParser(tags, pid))
          .get();
    } catch (InterruptedException | ExecutionException e) {
      // Normalise exception to IOException, so method signatures are not tied to a single HTTP
      // library.
      throw new IOException(e);
    }
  }

  @Override
  public void search(String tags, SearchCallback callback) {
    // Return results for page 0.
    search(tags, 0, callback);
  }

  @Override
  public void search(final String tags, final int pid, final SearchCallback callback) {
    Ion.with(this.context)
        .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
        .userAgent(SearchClient.USER_AGENT)
        .as(new SearchResultParser(tags, pid))
        .setCallback(new FutureCallback<SearchResult>() {
          @Override
          public void onCompleted(Exception e, SearchResult result) {
            if (e != null) {
              callback.onFailure(new IOException(e));
            } else {
              callback.onSuccess(result);
            }
          }
        });
  }

  @Override
  public String getDefaultQuery() {
    // Show work-safe images by default.
    return "";
  }

  @Override
  public Settings getSettings() {
    return new Settings(Settings.APIType.DANBOARD, name, apiEndpoint, username, apiKey);
  }

  @Override
  public AuthenticationType requiresAuthentication() {
    return AuthenticationType.OPTIONAL;
  }
  //endregion

  //region Creating search URLs

  /**
   * Generate request URL to the search API endpoint.
   *
   * @param tags  Space-separated tags.
   * @param pid   Page number (0-indexed).
   * @param limit Images to fetch per page.
   * @return URL to search results API.
   */
  protected String createSearchURL(String tags, int pid, int limit) {
    // Page numbers are 1-indexed for this API.
    final int page = pid + 1;

    if (!TextUtils.isEmpty(this.username) && !TextUtils.isEmpty(this.apiKey)) {
      return String.format(Locale.US, apiEndpoint + "/posts.xml?tags=%s&page=%d&limit=%d&login=%s&api_key=%s",
          Uri.encode(tags), page, limit, Uri.encode(this.username), Uri.encode(this.apiKey));
    }
    return String.format(Locale.US, apiEndpoint + "/posts.xml?tags=%s&page=%d&limit=%d", Uri.encode(tags), page, limit);
  }
  //endregion

  //region Parsing responses

  protected SearchResult parseAPIResponse(String body, String tags, int offset) throws IOException {
    return parseXMLResponse(body, tags, offset);
  }

  /**
   * Parse an XML response returned by the API.
   *
   * @param body   HTTP Response body.
   * @param tags   Tags used to retrieve the response.
   * @param offset Current paging offset.
   * @return A {@link io.github.tjg1.library.norilib.SearchResult} parsed from given XML.
   */
  @SuppressWarnings("FeatureEnvy")
  protected SearchResult parseXMLResponse(String body, String tags, int offset) throws IOException {
    // Create variables to hold the values as XML is being parsed.
    final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
    Image image = new Image();
    List<Tag> imageTags = new ArrayList<>();
    int position = 0;

    try {
      // Create an XML parser factory and disable namespace awareness for security reasons.
      // See: (http://lists.w3.org/Archives/Public/public-xmlsec/2009Dec/att-0000/sws5-jensen.pdf).
      final XmlPullParserFactory xmlParserFactory = XmlPullParserFactory.newInstance();
      xmlParserFactory.setNamespaceAware(false);

      // Create a new XML parser from factory and feed HTTP response data into it.
      final XmlPullParser xpp = xmlParserFactory.newPullParser();
      xpp.setInput(new StringReader(body));

      // Iterate over each XML element and handle pull parser "events".
      while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
        if (xpp.getEventType() == XmlPullParser.START_TAG) {
          // Get the tag's name.
          final String name = xpp.getName();

          if ("post".equals(name)) {
            // Create a new image for each <post> tag.
            image = new Image();
            imageTags = new ArrayList<>();
            image.searchPage = offset;
            image.searchPagePosition = position;
          }
          // Extract image metadata from XML tags.
          else if ("large-file-url".equals(name)) {
            image.sampleUrl = xpp.nextText();
          } else if ("image-width".equals(name)) {
            image.width = Integer.parseInt(xpp.nextText());
          } else if ("image-height".equals(name)) {
            image.height = Integer.parseInt(xpp.nextText());
          } else if ("preview-file-url".equals(name)) {
            image.previewUrl = xpp.nextText();
          } else if ("file-url".equals(name)) {
            image.fileUrl = xpp.nextText();
          } else if ("tag-string-general".equals(name)) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.GENERAL)));
          } else if ("tag-string-artist".equals(name)) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.ARTIST)));
          } else if ("tag-string-character".equals(name)) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.CHARACTER)));
          } else if ("tag-string-copyright".equals(name)) {
            imageTags.addAll(Arrays.asList(Tag.arrayFromString(xpp.nextText(), Tag.Type.COPYRIGHT)));
          } else if ("id".equals(name)) {
            image.id = xpp.nextText();
          } else if ("parent-id".equals(name)) {
            image.parentId = xpp.getAttributeValue(null, "nil") != null ? null : xpp.nextText();
          } else if ("pixiv-id".equals(name)) {
            image.pixivId = xpp.getAttributeValue(null, "nil") != null ? null : xpp.nextText();
          } else if ("rating".equals(name)) {
            image.safeSearchRating = Image.SafeSearchRating.fromString(xpp.nextText());
          } else if ("score".equals(name)) {
            image.score = Integer.parseInt(xpp.nextText());
          } else if ("source".equals(name)) {
            image.source = xpp.nextText();
          } else if ("md5".equals(name)) {
            image.md5 = xpp.nextText();
          } else if ("created-at".equals(name)) {
            image.createdAt = dateFromString(xpp.nextText());
          }
          // createdAt
        } else if (xpp.getEventType() == XmlPullParser.END_TAG) {
          if ("post".equals(xpp.getName())) {
            // Convert tag list to array.
            image.tags = imageTags.toArray(new Tag[imageTags.size()]);
            // Append values not returned by API to image.
            image.webUrl = webUrlFromId(image.id);
            // FIXME: API does not return thumbnail sizes.
            image.previewWidth = THUMBNAIL_SIZE;
            image.previewHeight = THUMBNAIL_SIZE;
            // FIXME: API does not return sample sizes.
            image.sampleWidth = SAMPLE_SIZE;
            image.sampleHeight = SAMPLE_SIZE;
            // Discard images requiring a gold account. They do not return a valid file_url.
            if (image.fileUrl != null) {
              // Add to result.
              imageList.add(image);
              position++;
            }
          }
        }
        xpp.next();
      }
    } catch (XmlPullParserException | ParseException e) {
      // Convert into IOException.
      // Needed for consistent method signatures in the SearchClient interface for different APIs.
      // (Throwing an XmlPullParserException would be fine, until dealing with an API using JSON, etc.)
      throw new IOException(e);
    }

    return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), offset);
  }

  /**
   * Create a {@link java.util.Date} object from String date representation used by this API.
   *
   * @param date Date string.
   * @return Date converted from given String.
   */
  protected static Date dateFromString(String date) throws ParseException {
    // Normalise the ISO8601 time zone into a format parse-able by SimpleDateFormat.
    if (!TextUtils.isEmpty(date)) {
      String newDate = date.replace("Z", "+0000");
      if (newDate.length() == 25) {
        newDate = newDate.substring(0, 22) + newDate.substring(23); // Remove timezone colon.
      }
      return DATE_FORMAT.parse(newDate);
    }
    return null;
  }

  /**
   * Get a URL viewable in the system web browser for given Image ID.
   *
   * @param id {@link io.github.tjg1.library.norilib.Image} ID.
   * @return URL for viewing the image in the browser.
   */
  protected String webUrlFromId(String id) {
    return String.format(Locale.US, "%s/posts/%s", apiEndpoint, id);
  }
  //endregion

  //region Ion async SearchResult parser
  /** Asynchronous search parser to use with ion. */
  protected class SearchResultParser implements AsyncParser<SearchResult> {
    /** Tags searched for. */
    protected final String tags;
    /** Current page offset. */
    protected final int pageOffset;

    public SearchResultParser(String tags, int pageOffset) {
      this.tags = tags;
      this.pageOffset = pageOffset;
    }

    @Override
    public Future<SearchResult> parse(DataEmitter emitter) {
      return new StringParser().parse(emitter)
          .then(new TransformFuture<SearchResult, String>() {
            @Override
            protected void transform(String result) throws Exception {
              setComplete(parseAPIResponse(result, tags, pageOffset));
            }
          });
    }

    @Override
    public void write(DataSink sink, SearchResult value, CompletedCallback completed) {
      // Not implemented.
    }

    @Override
    public Type getType() {
      return null;
    }
  }
  //endregion
}
