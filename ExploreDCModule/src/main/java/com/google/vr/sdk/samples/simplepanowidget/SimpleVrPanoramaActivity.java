/*
 * Copyright 2017 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.vr.sdk.samples.simplepanowidget;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.vr.sdk.widgets.common.VrWidgetView;
import com.google.vr.sdk.widgets.pano.VrPanoramaEventListener;
import com.google.vr.sdk.widgets.pano.VrPanoramaView;
import com.google.vr.sdk.widgets.pano.VrPanoramaView.Options;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import android.widget.NumberPicker;

import org.w3c.dom.Text;

/**
 * A basic PanoWidget Activity to load panorama images from disk. It will load a test image by
 * default. It can also load an arbitrary image from disk using:
 *   adb shell am start -a "android.intent.action.VIEW" \
 *     -n "com.google.vr.sdk.samples.simplepanowidget/.SimpleVrPanoramaActivity" \
 *     -d "/sdcard/FILENAME.JPG"
 *
 * To load stereo images, "--ei inputType 2" can be used to pass in an integer extra which will set
 * VrPanoramaView.Options.inputType.
 */
public class SimpleVrPanoramaActivity extends Activity {
  private static final String TAG = SimpleVrPanoramaActivity.class.getSimpleName();
  /** Actual panorama widget. **/
  private VrPanoramaView panoWidgetView;
  private VrPanoramaView panoWidgetView2;
  /**
   * Arbitrary variable to track load status. In this example, this variable should only be accessed
   * on the UI thread. In a real app, this variable would be code that performs some UI actions when
   * the panorama is fully loaded.
   */
  public boolean loadImageSuccessful;
  /** Tracks the file to be loaded across the lifetime of this app. **/
  private Uri fileUri;
  /** Configuration information for the panorama. **/
  private Options panoOptions = new Options();
  private Options panoOptions2 = new Options();
  private ImageLoaderTask backgroundImageLoaderTask;

  private TextView p1;
  private int SecondsPerPano = 3;
  private int MSPerPano = 3000;
  private Button myButton;

    /** Called when the user taps the Explore button */
    public void startExploration(View view) {
        myButton = findViewById(R.id.button);
        myButton.setEnabled(false); //don't let em click dat button again

        MSPerPano = SecondsPerPano * 1000; //duh

        panoWidgetView.setDisplayMode(VrWidgetView.DisplayMode.FULLSCREEN_MONO);

        Timer buttonTimer = new Timer();
        buttonTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        Options panoOptions = null;  // It's safe to use null VrPanoramaView.Options.
                        InputStream istr = null;

                            AssetManager assetManager = getAssets();
                            try {
                                istr = assetManager.open("pano_marchwomen18_00.png");
                                panoOptions = new Options();
                                panoOptions.inputType = Options.TYPE_MONO;
                            } catch (IOException e) {
                                Log.e(TAG, "Could not decode default bitmap: " + e);
                                return;
                            }


                        panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);
                        myButton.setEnabled(true);
                    }
                });
            }
        }, MSPerPano);
    }

  /**
   * Called when the app is launched via the app icon or an intent using the adb command above. This
   * initializes the app and loads the image to render.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_layout);

    // Make the source link clickable.
    TextView sourceText = (TextView) findViewById(R.id.source);
    sourceText.setText(Html.fromHtml(getString(R.string.source)));
    sourceText.setMovementMethod(LinkMovementMethod.getInstance());

    panoWidgetView = (VrPanoramaView) findViewById(R.id.pano_view);
    panoWidgetView.setEventListener(new ActivityEventListener());

    panoWidgetView2 = (VrPanoramaView) findViewById(R.id.pano_view2);
    panoWidgetView2.setEventListener(new ActivityEventListener());

    //can we get sum html up in herr?
      p1 = (TextView) findViewById(R.id.paragraph1);
      p1.setText(getText(R.string.paragraph1));

    //setup the number picker for number of seconds to display each image
      NumberPicker np = (NumberPicker) findViewById(R.id.np);

      //Populate NumberPicker values from minimum and maximum value range
      //Set the minimum value of NumberPicker
      np.setMinValue(2);
      //Specify the maximum value/number of NumberPicker
      np.setMaxValue(10);

      np.setValue(3);

      //Set a value change listener for NumberPicker
      np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
          @Override
          public void onValueChange(NumberPicker picker, int oldVal, int newVal){
              SecondsPerPano = newVal;
          }
      });

      //Gets whether the selector wheel wraps when reaching the min/max value.
      np.setWrapSelectorWheel(true);

    // Initial launch of the app or an Activity recreation due to rotation.
    handleIntent(getIntent());
  }

  /**
   * Called when the Activity is already running and it's given a new intent.
   */
  @Override
  protected void onNewIntent(Intent intent) {
    Log.i(TAG, this.hashCode() + ".onNewIntent()");
    // Save the intent. This allows the getIntent() call in onCreate() to use this new Intent during
    // future invocations.
    setIntent(intent);
    // Load the new image.
    handleIntent(intent);
  }

  /**
   * Load custom images based on the Intent or load the default image. See the Javadoc for this
   * class for information on generating a custom intent via adb.
   */
  private void handleIntent(Intent intent) {
    // Determine if the Intent contains a file to load.
    if (Intent.ACTION_VIEW.equals(intent.getAction())) {
      Log.i(TAG, "ACTION_VIEW Intent recieved");

      fileUri = intent.getData();
      if (fileUri == null) {
        Log.w(TAG, "No data uri specified. Use \"-d /path/filename\".");
      } else {
        Log.i(TAG, "Using file " + fileUri.toString());
      }

      panoOptions.inputType = intent.getIntExtra("inputType", Options.TYPE_MONO);
      Log.i(TAG, "Options.inputType = " + panoOptions.inputType);
    } else {
      Log.i(TAG, "Intent is not ACTION_VIEW. Using default pano image.");
      fileUri = null;
      panoOptions.inputType = Options.TYPE_MONO;
    }

    // Load the bitmap in a background thread to avoid blocking the UI thread. This operation can
    // take 100s of milliseconds.
    if (backgroundImageLoaderTask != null) {
      // Cancel any task from a previous intent sent to this activity.
      backgroundImageLoaderTask.cancel(true);
    }
    backgroundImageLoaderTask = new ImageLoaderTask();
    backgroundImageLoaderTask.execute(Pair.create(fileUri, panoOptions));
  }

  @Override
  protected void onPause() {
    panoWidgetView.pauseRendering();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    panoWidgetView.resumeRendering();
  }

  @Override
  protected void onDestroy() {
    // Destroy the widget and free memory.
    panoWidgetView.shutdown();

    // The background task has a 5 second timeout so it can potentially stay alive for 5 seconds
    // after the activity is destroyed unless it is explicitly cancelled.
    if (backgroundImageLoaderTask != null) {
      backgroundImageLoaderTask.cancel(true);
    }
    super.onDestroy();
  }

  /**
   * Helper class to manage threading.
   */
  class ImageLoaderTask extends AsyncTask<Pair<Uri, Options>, Void, Boolean> {

    /**
     * Reads the bitmap from disk in the background and waits until it's loaded by pano widget.
     */
    @Override
    protected Boolean doInBackground(Pair<Uri, Options>... fileInformation) {
      Options panoOptions = null;  // It's safe to use null VrPanoramaView.Options.
      InputStream istr = null;

      //attempt to populate secoind vr view
      Options panoOptions2 = null;  // It's safe to use null VrPanoramaView.Options.
      InputStream istr2 = null;

      //original ish
      if (fileInformation == null || fileInformation.length < 1
          || fileInformation[0] == null || fileInformation[0].first == null) {
        AssetManager assetManager = getAssets();
        try {
          istr = assetManager.open("pano_scusD00.png");
          panoOptions = new Options();
          panoOptions.inputType = Options.TYPE_MONO;
        } catch (IOException e) {
          Log.e(TAG, "Could not decode default bitmap: " + e);
          return false;
        }
      } else {
        try {
          istr = new FileInputStream(new File(fileInformation[0].first.getPath()));
          panoOptions = fileInformation[0].second;
        } catch (IOException e) {
          Log.e(TAG, "Could not load file: " + e);
          return false;
        }
      }

      panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);

      //dat new new
      if (fileInformation == null || fileInformation.length < 1
              || fileInformation[0] == null || fileInformation[0].first == null) {
        AssetManager assetManager = getAssets();
        try {
          istr = assetManager.open("pano_lincN00.png");
          panoOptions2 = new Options();
          panoOptions2.inputType = Options.TYPE_MONO;
        } catch (IOException e) {
          Log.e(TAG, "Could not decode default bitmap: " + e);
          return false;
        }
      } else {
        try {
          istr = new FileInputStream(new File(fileInformation[0].first.getPath()));
          panoOptions = fileInformation[0].second;
        } catch (IOException e) {
          Log.e(TAG, "Could not load file: " + e);
          return false;
        }
      }

      panoWidgetView2.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);

      //les wrap it up!!!
      try {
        istr.close();
      } catch (IOException e) {
        Log.e(TAG, "Could not close input stream: " + e);
      }

      return true;
    }
  }

  /**
   * Listen to the important events from widget.
   */
  private class ActivityEventListener extends VrPanoramaEventListener {
    /**
     * Called by pano widget on the UI thread when it's done loading the image.
     */
    @Override
    public void onLoadSuccess() {
      loadImageSuccessful = true;
    }

    /**
     * Called by pano widget on the UI thread on any asynchronous error.
     */
    @Override
    public void onLoadError(String errorMessage) {
      loadImageSuccessful = false;
      Toast.makeText(
          SimpleVrPanoramaActivity.this, "Error loading pano: " + errorMessage, Toast.LENGTH_LONG)
          .show();
      Log.e(TAG, "Error loading pano: " + errorMessage);
    }

      @Override
      public void onDisplayModeChanged(int newDisplayMode) {
          super.onDisplayModeChanged(newDisplayMode);
          if (newDisplayMode == VrWidgetView.DisplayMode.FULLSCREEN_MONO) {
              // SimpleVrPanoramaActivity.this.finish(); //this was original github code to return to force return to previous screen. haven't tested it yet.

            //testing 1..2..mic check mic check...
             // SecondsPerPano++;
             // p1.setText("Beep: " + SecondsPerPano);

              Timer buttonTimer = new Timer();
              buttonTimer.schedule(new TimerTask() {

                  @Override
                  public void run() {
                      runOnUiThread(new Runnable() {

                          @Override
                          public void run() {

                              Options panoOptions = null;  // It's safe to use null VrPanoramaView.Options.
                              InputStream istr = null;

                              AssetManager assetManager = getAssets();
                              try {
                                  istr = assetManager.open("pano_marchwomen18_01.png");
                                  panoOptions = new Options();
                                  panoOptions.inputType = Options.TYPE_MONO;
                              } catch (IOException e) {
                                  Log.e(TAG, "Could not decode default bitmap: " + e);
                                  return;
                              }

                              panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);

                          }
                      });
                  }
              }, MSPerPano);




          }
      }


  }
}
