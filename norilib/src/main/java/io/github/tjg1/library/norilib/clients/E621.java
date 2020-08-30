/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * {@link io.github.tjg1.library.norilib.clients.SearchClient} for the E621 imageboard.
 */
public class E621 extends Danbooru {
    final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S", Locale.US);
    //region Constants
    /**
     * Number of images to fetch with each search.
     */
    private static final int DEFAULT_LIMIT = 100;
    //endregion

    //region Constructors
    public E621(Context context, String name, String endpoint) {
        super(context, name, endpoint);
    }

    public E621(Context context, String name, String endpoint, String username, String password) {
        super(context, name, endpoint, username, password);
    }
    //endregion

    //region Service detection

    /**
     * Checks if the given URL exposes a supported API endpoint.
     *
     * @param uri URL to test.
     * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
     */
    @Nullable
    public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
        final String endpointUrl = Uri.withAppendedPath(uri, "/posts.json").toString();
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
        return new Settings(Settings.APIType.E621, name, apiEndpoint, username, apiKey);
    }
    //endregion

    @Override
    protected String createSearchURL(String tags, int pid, int limit) {
        // Page numbers are 1-indexed for this API.
        final int page = pid + 1;

        if (!TextUtils.isEmpty(this.username) && !TextUtils.isEmpty(this.apiKey)) {
            return String.format(Locale.US,  "%s/posts.json?tags=%s&page=%d&limit=%d&login=%s&api_key=%s",
                    apiEndpoint, Uri.encode(tags), page, limit, Uri.encode(this.username), Uri.encode(this.apiKey));
        }
        return String.format(Locale.US,  "%s/posts.json?tags=%s&page=%d&limit=%d",
                apiEndpoint, Uri.encode(tags), page, limit);
    }

    //region Parsing responses
    @Override
    protected String webUrlFromId(String id) {
        return String.format("%s/%s/%s", apiEndpoint, "post/show", id);
    }

    @Override
    protected SearchResult parseAPIResponse(String body, String tags, int offset) {
        return parseJSONResponse(body, tags, offset);
    }

    @Override
    protected SearchResult parseJSONResponse(String body, String tags, int offset) {
        final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(body);
            JSONArray posts = jsonObject.getJSONArray("posts");
            for (int i = 0; i < posts.length(); i++) {
                JSONObject post = (JSONObject) posts.get(i);
                JSONObject postFile = (JSONObject) post.get("file");
                JSONObject postPreview = (JSONObject) post.get("preview");
                JSONObject postSample = (JSONObject) post.get("sample");
                JSONObject postScore = (JSONObject) post.get("score");
                JSONArray postSources = (JSONArray) post.get("sources");
                JSONObject postTags = (JSONObject) post.get("tags");
                JSONObject postRelationships = (JSONObject) post.get("relationships");

                final Image image = new Image();
                image.searchPage = offset;
                image.searchPagePosition = i;

                // Base level attributes
                image.id = post.get("id").toString();
                image.createdAt = dateFromString(post.get("created_at").toString());
                image.safeSearchRating = Image.SafeSearchRating.fromString(post.get("rating").toString());

                // File attributes
                image.fileUrl = postFile.get("url").toString();
                image.md5 = postFile.get("md5").toString();
                image.width = (int) postFile.get("width");
                image.height = (int) postFile.get("height");

                // Preview attributes
                image.previewUrl = postPreview.get("url").toString();
                image.previewWidth = (int) postPreview.get("width");
                image.previewHeight = (int) postPreview.get("height");

                // Sample attributes
                image.sampleUrl = postSample.get("url").toString();
                image.sampleWidth = (int) postSample.get("width");
                image.sampleHeight = (int) postSample.get("height");

                // Sources
                // Just use the first source for now
                if ( postSources.length() > 0 ) {
                    image.source = postSources.get(0).toString();
                }

                // Tag array
                // Get arrays out of JSON
                JSONArray artistTags = (JSONArray) postTags.get("artist");
                JSONArray characterTags = (JSONArray) postTags.get("character");
                JSONArray copyrightTags = (JSONArray) postTags.get("copyright");
                JSONArray generalTags = (JSONArray) postTags.get("general");
                JSONArray speciesTags = (JSONArray) postTags.get("species");

                // Create Tag arrays for each type
                Tag[] artistTagsArr = Tag.arrayFromStringArray(stringArrayFromJSONArray(artistTags), Tag.Type.ARTIST);
                Tag[] characterTagsArr = Tag.arrayFromStringArray(stringArrayFromJSONArray(characterTags), Tag.Type.CHARACTER);
                Tag[] copyrightTagsArr = Tag.arrayFromStringArray(stringArrayFromJSONArray(copyrightTags), Tag.Type.COPYRIGHT);
                Tag[] speciesTagsArr = Tag.arrayFromStringArray(stringArrayFromJSONArray(speciesTags), Tag.Type.SPECIES);
                Tag[] generalTagsArr = Tag.arrayFromStringArray(stringArrayFromJSONArray(generalTags), Tag.Type.GENERAL);
                image.tags = Tag.arrayFromTagArrays(artistTagsArr, characterTagsArr, copyrightTagsArr, speciesTagsArr, generalTagsArr);

                image.webUrl = webUrlFromId(image.id);
                image.parentId = postRelationships.get("parent_id").toString();

                // Score attributes
                image.score = (int) postScore.get("total");

                if (image.fileUrl != null) {
                    imageList.add(image);
                }
            }
        } catch (JSONException | ParseException e) {
            e.printStackTrace();
        }

        return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), offset);
    }

    String[] stringArrayFromJSONArray(JSONArray jsonArray) throws JSONException {
        String[] stringArray = new String[jsonArray.length()];
        for (int i = 0; i < stringArray.length; i++) {
            stringArray[i] = jsonArray.get(i).toString();
        }
        return stringArray;
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
            return DATE_FORMAT.parse(date);
        }
        return null;
    }
}
