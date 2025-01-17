package com.google.vr.sdk.rubicon.panopano;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.NumberPicker;

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

//import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.s3.transferutility.*;
import com.amazonaws.mobile.client.AWSMobileClient;

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
  private ImageLoaderTask backgroundImageLoaderTask;

  private TextView p1;
  private int SecondsPerPano = 3;
  private int MSPerPano = 3000;
  private Button myButton;

  private String fileLocPrefix;
  private static final String TEST_KEY = "pano_namN00.png";

    Handler handler = new Handler();
    int handlerDelay = 1000; //milliseconds
    private float lastPitch = 0;
    private float lastYaw = 0;




  private boolean TriggerChangesOnFullScreenPano = false;

  public void CheckHeadPosish(){
      float[] fVector = {0,0};
      panoWidgetView.getHeadRotation(fVector);
      lastPitch = fVector[0];
      lastYaw = fVector[1];
      Log.d(TAG, "Pitch: " + lastPitch + "   Yaw: " + lastYaw);

  }


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

                        Bitmap b;
                            AssetManager assetManager = getAssets();
                            try {
                                 b = BitmapFactory.decodeFile(fileLocPrefix + "/" + TEST_KEY);
                            } catch (Exception e) { //orig:   } catch (IOException e) {
                                Log.e(TAG, "GODDAMN IT VAKAITIS!!!! " + e);
                                return;
                            }

                        Options panoOptions  = new Options();
                        panoOptions.inputType = Options.TYPE_MONO;
                     //   panoWidgetView.loadImageFromBitmap(BitmapFactory.decodeStream(istr), panoOptions);
                        panoWidgetView.loadImageFromBitmap(b, panoOptions);
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

    fileLocPrefix = Environment.getExternalStorageDirectory().toString();

    //yo dawg i copied this code str8 from https://docs.aws.amazon.com/aws-mobile/latest/developerguide/how-to-integrate-an-existing-bucket.html
    AWSMobileClient.getInstance().initialize(this).execute();

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



    //whelp..
      downloadWithTransferUtility(TEST_KEY);
  }

    public void downloadWithTransferUtility(String kkey) {

        TransferUtility transferUtility =
                TransferUtility.builder()
                        .context(getApplicationContext())
                        .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                        .s3Client(new AmazonS3Client(AWSMobileClient.getInstance().getCredentialsProvider()))
                        .build();

        TransferObserver downloadObserver =
                transferUtility.download(
                        kkey,
                        new File(fileLocPrefix + "/" + kkey));
       // Log.d(TAG, "CORN DOGS FOR ALL THESE PEOPLE JACKIE: " + fileLocPrefix + "/" + kkey);
        // Attach a listener to the observer to get notified of the
        // updates in the state and the progress
        downloadObserver.setTransferListener(new TransferListener() {

            @Override
            public void onStateChanged(int id, TransferState state) {
                if (TransferState.COMPLETED == state) {
                    // Handle a completed upload.
                    Log.d(TAG, "FOURTH PLACE" + fileLocPrefix + "/" + TEST_KEY);
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float)bytesCurrent/(float)bytesTotal) * 100;
                int percentDone = (int)percentDonef;

                Log.d("MainActivity", "   ID:" + id + "   bytesCurrent: " + bytesCurrent + "   bytesTotal: " + bytesTotal + " " + percentDone + "%");
            }

            @Override
            public void onError(int id, Exception ex) {
                // Handle errors
                Log.e(TAG, "well shit -> "  + ex.getMessage());
            }

        });

        // If you do not want to attach a listener and poll for the data
        // from the observer, you can check for the state and the progress
        // in the observer.
        if (TransferState.COMPLETED == downloadObserver.getState()) {
            // Handle a completed upload.
        }

        Log.d("YourActivity", "Bytes Transferrred: " + downloadObserver.getBytesTransferred());
        Log.d("YourActivity", "Bytes Total: " + downloadObserver.getBytesTotal());
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
    panoWidgetView2.pauseRendering();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    panoWidgetView.resumeRendering();
    panoWidgetView2.resumeRendering();
  }

  @Override
  protected void onDestroy() {
    // Destroy the widget and free memory.
    panoWidgetView.shutdown();
    panoWidgetView2.shutdown();

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


          //this is for the check pitch/yaw every so often
          handler.postDelayed(new Runnable(){
              public void run(){
                  //do something
                  CheckHeadPosish();
                  handler.postDelayed(this, handlerDelay);
              }
          }, handlerDelay);




          if (newDisplayMode == VrWidgetView.DisplayMode.FULLSCREEN_MONO && TriggerChangesOnFullScreenPano) {
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
