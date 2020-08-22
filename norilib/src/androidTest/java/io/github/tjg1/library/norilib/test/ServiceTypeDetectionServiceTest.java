/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.test.InstrumentationTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.tjg1.library.norilib.clients.Danbooru;
import io.github.tjg1.library.norilib.clients.DanbooruLegacy;
import io.github.tjg1.library.norilib.clients.E621Legacy;
import io.github.tjg1.library.norilib.clients.Flickr;
import io.github.tjg1.library.norilib.clients.FlickrUser;
import io.github.tjg1.library.norilib.clients.Gelbooru;
import io.github.tjg1.library.norilib.service.ServiceTypeDetectionService;

import static org.fest.assertions.api.Assertions.assertThat;

/** Test unit for the {@link io.github.tjg1.library.norilib.service.ServiceTypeDetectionService} service. */
public class ServiceTypeDetectionServiceTest extends InstrumentationTestCase {
  /** Wait this many seconds for the detection to complete. */
  private static final int RESPONSE_TIMEOUT = 120000;
  /** {@link IntentFilter} used to receive responses from {@link io.github.tjg1.library.norilib.service.ServiceTypeDetectionService}. */
  private final static IntentFilter INTENT_FILTER = new IntentFilter(ServiceTypeDetectionService.ACTION_DONE);

  /** Test detection of the Danbooru 2.x API */
  public void testDanbooruDetection() throws Throwable {
    String url = Danbooru.detectService(getInstrumentation().getContext(),
        Uri.parse("https://danbooru.donmai.us"), RESPONSE_TIMEOUT);

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://danbooru.donmai.us");
  }

  /** Test detection of the Danbooru 1.x API */
  public void testDanbooruLegacyDetection() throws Throwable {
    String url = DanbooruLegacy.detectService(getInstrumentation().getContext(),
        Uri.parse("https://danbooru.donmai.us"), RESPONSE_TIMEOUT);

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://danbooru.donmai.us");
  }

  /** Test detection of the Gelbooru API. */
  public void testGelbooruDetection() throws Throwable {
    String url = Gelbooru.detectService(getInstrumentation().getContext(),
        Uri.parse("http://safebooru.org"), RESPONSE_TIMEOUT);

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("http://safebooru.org");
  }

  /** Test detection of the Shimmie API. */
  public void testShimmieDetection() throws Throwable {
    // Disable dollbooru tests, as its down.
    //String url = Shimmie.detectService(getInstrumentation().getContext(),
    //    Uri.parse("http://dollbooru.org"), RESPONSE_TIMEOUT);

    //assertThat(url).isNotNull();
    //assertThat(url).isEqualTo("http://dollbooru.org");
  }

  /** Test detection of Moebooru (Danbooru 1.x fork) boards. */
  public void testMoebooruDetection() throws Throwable {
    String url = DanbooruLegacy.detectService(getInstrumentation().getContext(),
        Uri.parse("https://yande.re"), RESPONSE_TIMEOUT);

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://yande.re");
  }

  /** Test detection of the Mono-sodium Glutamate API. */
  public void testE621Detection() throws Throwable {
    String url = E621Legacy.detectService(Uri.parse("https://e621.net"));

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://e621.net");
  }

  /** Test detection of the Chlorine dioxide API. */
  public void testE926Detection() throws Throwable {
    String url = E621Legacy.detectService(Uri.parse("https://e926.net"));

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://e926.net");
  }

  /** Test detection of the Chlorine dioxide API. */
  public void testFlickrDetection() throws Throwable {
    String url = Flickr.detectService(Uri.parse("https://api.flickr.com"));

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://api.flickr.com/services/rest");
  }

  /** Test detection of the Chlorine dioxide API. */
  public void testFlickrUserDetection() throws Throwable {
    String url = FlickrUser.detectService(Uri.parse("http://flickr.com/photos/128962151@N05"));

    assertThat(url).isNotNull();
    assertThat(url).isEqualTo("https://flickr.com/photos/128962151@N05");
  }

  /** Test error returned when an invalid URL is supplied. */
  public void testInvalidUrlError() throws Throwable {
    // Create a lock that waits for the request to complete in background.
    final CountDownLatch lock = new CountDownLatch(1);
    // Values received from the BroadcastReceiver.
    // One-element arrays are a hack used to set values from outside the main thread without bothering with locking.
    final int[] resultCode = new int[1];

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Register the broadcast receiver.
        getInstrumentation().getContext().registerReceiver(new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            // Set values received in the intent.
            resultCode[0] = intent.getIntExtra(ServiceTypeDetectionService.RESULT_CODE, -1);
            // Unregister broadcast receiver.
            getInstrumentation().getContext().unregisterReceiver(this);
            // Clear the lock in the main thread.
            lock.countDown();
          }
        }, INTENT_FILTER);
        // Start the service.
        getInstrumentation().getContext().startService(new Intent(getInstrumentation().getContext(),
            ServiceTypeDetectionService.class)
            .putExtra(ServiceTypeDetectionService.ENDPOINT_URL, "danbooru###$(%*#)@(*)(#@*)(os"));
      }
    });

    // Wait to receive broadcast from the service.
    lock.await(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
    assertThat(resultCode[0]).isEqualTo(ServiceTypeDetectionService.RESULT_FAIL_INVALID_URL);
  }

  /** Test detection of sites that do not expose a supported API */
  public void testNoServiceDetected() throws Throwable {
    // Create a lock that waits for the request to complete in background.
    final CountDownLatch lock = new CountDownLatch(1);
    // Values received from the BroadcastReceiver.
    // One-element arrays are a hack used to set values from outside the main thread without bothering with locking.
    final int[] resultCode = new int[1];

    runTestOnUiThread(new Runnable() {
      @Override
      public void run() {
        // Register the broadcast receiver.
        getInstrumentation().getContext().registerReceiver(new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            // Set values received in the intent.
            resultCode[0] = intent.getIntExtra(ServiceTypeDetectionService.RESULT_CODE, -1);
            // Unregister broadcast receiver.
            getInstrumentation().getContext().unregisterReceiver(this);
            // Clear the lock in the main thread.
            lock.countDown();
          }
        }, INTENT_FILTER);
        // Start the service.
        getInstrumentation().getContext().startService(new Intent(getInstrumentation().getContext(),
            ServiceTypeDetectionService.class)
            .putExtra(ServiceTypeDetectionService.ENDPOINT_URL, "http://google.com"));
      }
    });

    // Wait to receive broadcast from the service.
    lock.await(RESPONSE_TIMEOUT, TimeUnit.SECONDS);
    assertThat(resultCode[0]).isEqualTo(ServiceTypeDetectionService.RESULT_FAIL_NO_API);
  }
}
