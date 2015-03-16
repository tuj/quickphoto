package dk.tuj.robocam;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends Activity {
  private Camera mCamera;
  private CameraPreview mPreview;
  private static final String TAG = "RoboCam";
  private byte[] imageData;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_camera);

    // Create an instance of Camera
    mCamera = getCameraInstance();

    mCamera.setDisplayOrientation(90);

    // get Camera parameters
    Camera.Parameters params = mCamera.getParameters();

    List<String> focusModes = params.getSupportedFocusModes();
    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
      // Autofocus mode is supported
      // set the focus mode
      params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
      // set Camera parameters
      mCamera.setParameters(params);
    }

    List<String> flashModes = params.getSupportedFocusModes();
    if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
      // Autofocus mode is supported
      // set the focus mode
      params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
      // set Camera parameters
      mCamera.setParameters(params);
    }

    // Create our Preview view and set it as the content of our activity.
    mPreview = new CameraPreview(this, mCamera);
    FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
    preview.addView(mPreview);

    // Add a listener to the Capture button
    ImageButton captureButton = (ImageButton) findViewById(R.id.button_capture);
    captureButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            // get an image from the camera

            mCamera.autoFocus(new Camera.AutoFocusCallback() {
              @Override
              public void onAutoFocus(boolean success, Camera camera) {
                mCamera.takePicture(null, null, mPicture);
              }
            });
          }
        }
    );
  }

  /** A safe way to get an instance of the Camera object. */
  public static Camera getCameraInstance(){
    Camera c = null;
    try {
      c = Camera.open(); // attempt to get a Camera instance
    }
    catch (Exception e){
      // Camera is not available (in use or does not exist)
      // @TODO: Throw Toast!
    }
    return c; // returns null if camera is unavailable
  }

  /** Check if this device has a camera */
  private boolean checkCameraHardware(Context context) {
    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
      // this device has a camera
      return true;
    } else {
      // no camera on this device
      return false;
    }
  }

  private PictureCallback mPicture = new PictureCallback() {

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
      imageData = data;

      /*

      File pictureFile = getOutputMediaFile();
      if (pictureFile == null){
        Log.d(TAG, "Error creating media file, check storage permissions");
        return;
      }

      try {
        FileOutputStream fos = new FileOutputStream(pictureFile);
        fos.write(data);
        fos.close();
      } catch (FileNotFoundException e) {
        Log.d(TAG, "File not found: " + e.getMessage());
      } catch (IOException e) {
        Log.d(TAG, "Error accessing file: " + e.getMessage());
      }

       */

      showEnterNameScreen();
    }
  };

  private void showEnterNameScreen() {
    setContentView(R.layout.activity_save);
  }

  public static final int MEDIA_TYPE_IMAGE = 1;

  /** Create a file Uri for saving an image or video */
  private static Uri getOutputImageFileUri(String filename){
    return Uri.fromFile(getOutputMediaFile(filename));
  }

  /** Create a File for saving an image or video */
  private static File getOutputMediaFile(String filename){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_PICTURES), "RoboCam");
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
      if (! mediaStorageDir.mkdirs()){
        Log.d(TAG, "failed to create directory");
        return null;
      }
    }

    // Create a media file name
    String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HHmm", Locale.getDefault()).format(new Date());
    return new File(mediaStorageDir.getPath() + File.separator +
        filename + "_" + timeStamp + ".jpg");
  }

  @Override
  protected void onPause() {
    super.onPause();
    releaseCamera();              // release the camera immediately on pause event
  }

  private void releaseCamera(){
    if (mCamera != null){
      mCamera.release();        // release the camera for other applications
      mCamera = null;
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_camera, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
