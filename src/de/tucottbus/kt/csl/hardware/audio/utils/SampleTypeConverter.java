package de.tucottbus.kt.csl.hardware.audio.utils;


/**
 * Utility class for audio signal processing.
 * 
 * @author Martin Birth
 *
 */
public class SampleTypeConverter {

  /**
   * Singleton class
   */
  private SampleTypeConverter() {}

  /**
   * Converts a buffer of floats to a buffer of bytes with a given byte order.
   * 
   * @param out
   *          buffer of bytes.
   * @param in
   *          buffer of floats.
   * @param bigEndian
   *          true for big endian byte order, false otherwise.
   * @param gain
   *          float value of the gain
   * 
   */
  public static void floatToByte(byte[] out, float[] in, boolean bigEndian,
      float gain) {
    floatToByte(out, 0, in, 0, in.length, bigEndian, gain);
  }

  /**
   * Converts a buffer of floats to a buffer of bytes with a given byte order.
   * 
   * @param out
   *          Output array
   * @param outstart
   *          Start index of output
   * @param in
   *          Input array
   * @param instart
   *          Start index of input
   * @param inlength
   *          Number of floats to copy
   * @param bigEndian
   *          Format
   */
  public static void floatToByte(byte[] out, int outstart, float[] in,
      int instart, int inlength, boolean bigEndian, float gain) {
    int bufsz = Math.min(inlength, in.length);
    int ib = outstart;
    if (bigEndian) {
      for (int i = 0; i < bufsz; ++i) {
        short y = (short) (gain * 32767. * Math.min(
            Math.max(in[i + instart], -1.0f), 1.0f));
        out[ib++] = (byte) ((y >> 8) & 0xFF);
        out[ib++] = (byte) (y & 0xFF);
      }
    } else {
      for (int i = 0; i < bufsz; ++i) {
        short y = (short) (gain * 32767. * in[i + instart]);
        out[ib++] = (byte) (y & 0xFF);
        out[ib++] = (byte) ((y >> 8) & 0xFF);
      }
    }
  }

  /**
   * Converts a buffer of floats to a buffer of shorts.
   * 
   * @param in
   *          buffer of floats.
   * @return buffer of shorts.
   */
  public static short[] floatToShort(float[] in) {
    short[] out = new short[in.length];
    for (int i = 0; i < in.length; i++) {
      out[i] = (short) (32767. * in[i]);
    }
    return out;
  }

  /**
   * Converts a buffer of shorts to a buffer of floats.
   * 
   * @param in
   *          buffer of shorts.
   * @return buffer of floats.
   */
  public static float[] shortToFloat(short[] in) {
    float[] out = new float[in.length];
    for (int i = 0; i < in.length; i++) {
      out[i] = (float) (in[i] / 32768.);
    }
    return out;
  }

  /**
   * Converts a buffer of bytes to a buffer of floats with a given byte order.
   * 
   * @param out
   *          buffer of floats.
   * @param in
   *          buffer of bytes.
   * @param bigEndian
   *          true for big endian byte order, false otherwise.
   */
  public static void byteToFloat(float[] out, byte[] in, boolean bigEndian) {
    byteToFloat(out, in, bigEndian, out.length);
  }

  /**
   * Converts a buffer of bytes to a buffer of floats with a given byte order.
   * Will copy numFloat floats to out.
   * 
   * @param out
   *          buffer of floats.
   * @param in
   *          buffer of bytes.
   * @param bigEndian
   *          true for big endian byte order, false otherwise.
   * @param numFloats
   *          number of elements to copy into out
   */
  public static void byteToFloat(float[] out, byte[] in, boolean bigEndian,
      int numFloats) {
    byteToFloat(out, in, bigEndian, 0, numFloats);
  }

  /**
   * Converts a buffer of bytes to a buffer of floats with a given byte order.
   * Will copy numFloat floats to out.
   * 
   * @param out
   *          buffer of floats.
   * @param in
   *          buffer of bytes.
   * @param bigEndian
   *          true for big endian byte order, false otherwise.
   * @param startIndexInByteArray
   *          where to start copying from
   * @param numFloats
   *          number of elements to copy into out
   */
  public static void byteToFloat(float[] out, byte[] in, boolean bigEndian,
      int startIndexInByteArray, int numFloats) {
    byteToFloat(out, in, bigEndian, startIndexInByteArray, 0, numFloats);
  }

  /**
   * Converts a buffer of bytes to a buffer of floats with a given byte order.
   * Will copy numFloat floats to out.
   * 
   * @param out
   *          buffer of floats.
   * @param in
   *          buffer of bytes.
   * @param bigEndian
   *          true for big endian byte order, false otherwise.
   * @param startIndexInByteArray
   *          where to start copying from
   * @param startIndexInFloatArray
   *          where to start copying to
   * @param numFloats
   *          number of elements to copy into out
   */
  public static void byteToFloat(float[] out, byte[] in, boolean bigEndian,
      int startIndexInByteArray, int startIndexInFloatArray, int numFloats) {
    if (bigEndian) {
      int ib = startIndexInByteArray;
      int min = Math.min(out.length, startIndexInFloatArray + numFloats);
      for (int i = startIndexInFloatArray; i < min; ++i) {
        float sample = ((in[ib + 0] << 8) | (in[ib + 1] & 0xFF)) / 32768.0F;
        ib += 2;
        out[i] = sample;
      }
    } else {
      int ib = startIndexInByteArray;
      int min = Math.min(out.length, startIndexInFloatArray + numFloats);
      for (int i = startIndexInFloatArray; i < min; ++i) {
        float sample = ((in[ib] & 0xFF) | (in[ib + 1] << 8)) / 32768.0F;
        ib += 2;
        out[i] = sample;
      }
    }
  }


}
