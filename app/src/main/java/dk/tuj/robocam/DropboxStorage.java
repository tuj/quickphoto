package dk.tuj.robocam;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;

/**
 * DropboxStorage.
 */
public class DropboxStorage implements Storage {
  private String APP_KEY = Configuration.APP_KEY;
  private String APP_SECRET = Configuration.APP_SECRET;
  private static final String ACCOUNT_PREFS_NAME = "gembon_prefs";
  private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
  private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

  private static final String TAG = "DropboxStorage";

  DropboxAPI<AndroidAuthSession> mApi;

  private Activity activity;

  public DropboxStorage(Activity activity) {
    this.activity = activity;

    // We create a new AuthSession so that we can use the Dropbox API.
    AndroidAuthSession session = buildSession();
    mApi = new DropboxAPI<AndroidAuthSession>(session);

    checkAppKeySetup();
  }

  @Override
  public void connect() {
    if (!mApi.getSession().isLinked()) {
      mApi.getSession().startOAuth2Authentication(activity);
    }
  }

  public void onResume() {
    if (mApi == null) {
      return;
    }

    AndroidAuthSession session = mApi.getSession();

    // The next part must be inserted in the onResume() method of the
    // activity from which session.startAuthentication() was called, so
    // that Dropbox authentication completes properly.
    if (session.authenticationSuccessful()) {
      try {
        // Mandatory call to complete the auth
        session.finishAuthentication();

        // Store it locally in our app for later use
        storeAuth(session);
      } catch (IllegalStateException e) {
        showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
        Log.i(TAG, "Error authenticating", e);
      }
    }
  }

  @Override
  public void saveFile(File file, String filename) {
    if (!mApi.getSession().isLinked()) {
      mApi.getSession().startOAuth2Authentication(activity);
    }

    try {
      FileInputStream inputStream = new FileInputStream(file);
      mApi.putFile("/" + filename + ".jpg", inputStream, file.length(), null, null);
      Log.i(TAG, "File uploaded to dropbox");
    }
    catch (Exception e) {
      // @TODO: Throw relevant Storage exception.
      Log.e(TAG, e.toString());
    }
  }

  @Override
  public void saveData(byte[] data, String filename) {
    if (!mApi.getSession().isLinked()) {
      mApi.getSession().startOAuth2Authentication(activity);
    }

    try {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
      mApi.putFile("/" + filename + ".jpg", inputStream, data.length, null, null);
      Log.i(TAG, "File uploaded to dropbox");
    }
    catch (Exception e) {
      // @TODO: Throw relevant Storage exception.
      Log.e(TAG, e.toString());
    }
  }

  private void logOut() {
    // Remove credentials from the session
    mApi.getSession().unlink();

    // Clear our stored keys
    clearKeys();
  }

  private void checkAppKeySetup() {
    // Check to make sure that we have a valid app key
    if (APP_KEY.startsWith("CHANGE") ||
        APP_SECRET.startsWith("CHANGE")) {
      showToast("You must apply for an app key and secret from developers.dropbox.com, and add them to the app before trying it.");
      activity.finish();
      return;
    }

    // Check if the app has set up its manifest properly.
    Intent testIntent = new Intent(Intent.ACTION_VIEW);
    String scheme = "db-" + APP_KEY;
    String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
    testIntent.setData(Uri.parse(uri));
    PackageManager pm = activity.getPackageManager();
    if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
      showToast("URL scheme in your app's " +
          "manifest is not set up correctly. You should have a " +
          "com.dropbox.client2.android.AuthActivity with the " +
          "scheme: " + scheme);
      activity.finish();
    }
  }

  private void showToast(String msg) {
    Toast error = Toast.makeText(activity, msg, Toast.LENGTH_LONG);
    error.show();
  }

  /**
   * Shows keeping the access keys returned from Trusted Authenticator in a local
   * store, rather than storing user name & password, and re-authenticating each
   * time (which is not to be done, ever).
   */
  private void loadAuth(AndroidAuthSession session) {
    SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
    String key = prefs.getString(ACCESS_KEY_NAME, null);
    String secret = prefs.getString(ACCESS_SECRET_NAME, null);
    if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

    if (key.equals("oauth2:")) {
      // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
      session.setOAuth2AccessToken(secret);
    } else {
      // Still support using old OAuth 1 tokens.
      session.setAccessTokenPair(new AccessTokenPair(key, secret));
    }
  }

  /**
   * Shows keeping the access keys returned from Trusted Authenticator in a local
   * store, rather than storing user name & password, and re-authenticating each
   * time (which is not to be done, ever).
   */
  private void storeAuth(AndroidAuthSession session) {
    // Store the OAuth 2 access token, if there is one.
    String oauth2AccessToken = session.getOAuth2AccessToken();
    if (oauth2AccessToken != null) {
      SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
      SharedPreferences.Editor edit = prefs.edit();
      edit.putString(ACCESS_KEY_NAME, "oauth2:");
      edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
      edit.commit();
      return;
    }
    // Store the OAuth 1 access token, if there is one.  This is only necessary if
    // you're still using OAuth 1.
    AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
    if (oauth1AccessToken != null) {
      SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
      SharedPreferences.Editor edit = prefs.edit();
      edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
      edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
      edit.commit();
      return;
    }
  }

  private void clearKeys() {
    SharedPreferences prefs = activity.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
    SharedPreferences.Editor edit = prefs.edit();
    edit.clear();
    edit.commit();
  }

  private AndroidAuthSession buildSession() {
    AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

    AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
    loadAuth(session);
    return session;
  }
}
