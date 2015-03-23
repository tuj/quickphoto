package dk.tuj.robocam;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraActivity extends Activity {
  private Camera mCamera;
  private CameraPreview mPreview;
  private static final String TAG = "RobotiCam";
  private byte[] imageData;

  private Storage storage;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_camera);

    startCameraPage();

    storage = new DropboxStorage(CameraActivity.this);
  }

  private void startCameraPage() {
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

  private void saveToDropboxDone(Boolean result) {
    Log.i(TAG, "SaveToDropbox done with result: " + result);

    setContentView(R.layout.activity_done);

    final Button saveButton = (Button) findViewById(R.id.button2);
    saveButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        startCameraPage();
      }
    });

    if (result) {
      Toast success = Toast.makeText(CameraActivity.this, "Saving to dropbox successful", Toast.LENGTH_LONG);
      success.show();
    }
    else {
      Toast error = Toast.makeText(CameraActivity.this, "Saving to dropbox failed", Toast.LENGTH_LONG);
      error.show();
    }
  }

  private class SaveToDropbox extends AsyncTask<DataFilenamePair, Void, Boolean> {
    protected Boolean doInBackground(DataFilenamePair... dfps) {
      for (DataFilenamePair dfp : dfps) {
        try {
          Bitmap storedBitmap = BitmapFactory.decodeByteArray(dfp.data, 0, dfp.data.length, null);

          Matrix mat = new Matrix();
          mat.postRotate(90);  // angle is the desired angle you wish to rotate
          storedBitmap = Bitmap.createBitmap(storedBitmap, 0, 0, storedBitmap.getWidth(), storedBitmap.getHeight(), mat, true);

          ByteArrayOutputStream stream = new ByteArrayOutputStream();
          storedBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

          storage.saveData(stream.toByteArray(), dfp.filename);
        }
        catch (Exception e) {
          Log.e(TAG, e.toString());
          return false;
        }
      }

      return true;
    }

    protected void onPostExecute(Boolean result) {
      saveToDropboxDone(result);
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

      releaseCamera();

      showEnterNameScreen();
    }
  };

  private void showEnterNameScreen() {
    setContentView(R.layout.activity_save);

    storage.connect();

    final Button saveButton = (Button) findViewById(R.id.button);
    saveButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        EditText txtDescription = (EditText) findViewById(R.id.editText);

        String filename = txtDescription.getText().toString();

        DataFilenamePair ffp = new DataFilenamePair(imageData, filename);

        setContentView(R.layout.activity_saving);

        new SaveToDropbox().execute(ffp);
      }
    });

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

  /**
   * When returning to the app from storage authentication.
   */
  @Override
  protected void onResume() {
    super.onResume();

    storage.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    releaseCamera();              // release the camera immediately on pause event
  }

  /**
   * Release the camera.
   */
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
