package dk.tuj.robocam;

import java.io.File;

/**
 * Storage interface.
 */
public interface Storage {
  /**
   * Save a file to the storage.
   *
   * @param file
   *   The file to save.
   * @param filename
   *   The filename to save the file under.
   */
  public void saveFile(File file, String filename);

  /**
   * Save a byte array to the storage.
   * @param data
   *   The data to save.
   * @param filename
   *   The filename to save the file under.
   */
  public void saveData(byte[] data, String filename);

  /**
   * Connect the storage.
   */
  public void connect();

  /**
   * Called after control has returned to the Activity.
   */
  public void onResume();
}
