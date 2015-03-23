package dk.tuj.robocam;

/**
 * DataFilenamePair
 */
public class DataFilenamePair {
  public byte[] data;
  public String filename;

  public DataFilenamePair(byte[] data, String filename) {
    this.data = data;
    this.filename = filename;
  }
}
