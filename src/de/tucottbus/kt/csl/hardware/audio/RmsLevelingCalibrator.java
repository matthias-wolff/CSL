package de.tucottbus.kt.csl.hardware.audio;

import java.util.Arrays;

/**
 * Simple relative calibration of microphone array audio input channels. The
 * calibration procedure assumes that the long-term RMS's of all channels should
 * be equal and that the root mean square of the actually measured long-term
 * channel RMS's is the true value.
 * 
 * @author Matthias Wolff
 */
public class RmsLevelingCalibrator 
{
  /**
   * Ring buffer of mean squares, a {@link #channels} x {@link #bufferLength} 
   * array of floats.
   */
  private float[][] msBuffer;

  /**
   * Current index in {@link #msBuffer}.
   */
  private int bIndex = -1;
  
  /**
   * Number of {@link #putRms(float[])}s.
   */
  private int puts = 0;
  
  /**
   * The number of audio channels.
   */
  private int channels;

  /**
   * The length of the RMS averaging ring buffer.
   */
  private int bufferLength;
  
  /**
   * Creates a new RMS leveling calibrator.
   * 
   * @param channels
   *          The number of audio channels, &ge;2.
   * @param bufferLength
   *          The length of the RMS averaging ring bufffer, &ge;2. 100 is a 
   *          reasonable choice.
   */
  public RmsLevelingCalibrator(int channels, int bufferLength)
  {
    if (channels<=1)
      throw new IllegalArgumentException("Number of channels must be greater than 1");
    if (bufferLength<=1)
      throw new IllegalArgumentException("Queue length must be greater than 1");
    
    this.channels = channels;
    this.bufferLength = bufferLength;

    msBuffer = new float[channels][bufferLength];
    for (int i=0; i<channels; i++)
      Arrays.fill(msBuffer[i],0);
  }
  
  /**
   * Adds <em>uncalibrated</em>(!) RMS values to the calibrator.
   * 
   * @param rms
   *          An array of {@link #channels} uncalibrated RMS values.
   * @throws NullPointerException
   *          if {@code rms} is {@code null}
   * @throws IllegalArgumentException
   *          if {@code rms.length} &ne; {@link #channels}.
   */
  public synchronized void putRms(float[] rms)
  throws IllegalArgumentException
  {
    if (rms.length!=channels)
      throw new IllegalArgumentException("Invalid array length "+rms.length
        + ", should be "+channels);

    puts++;
    bIndex = (bIndex+1)%bufferLength;
    for (int channel=0; channel<channels; channel++)
      msBuffer[channel][bIndex] = rms[channel]*rms[channel];
  }

  /**
   * Adds <em>uncalibrated</em>(!) MS (mean of squares, MS = RMS<sup>2</sup>) 
   * values to the calibrator.
   * 
   * @param ms
   *          An array of {@link #channels} uncalibrated MS values.
   * @throws NullPointerException
   *          if {@code rms} is {@code null}
   * @throws IllegalArgumentException
   *          if {@code rms.length} &ne; {@link #channels}.
   */
  public synchronized void putMs(float[] ms)
  throws IllegalArgumentException
  {
    if (ms.length!=channels)
      throw new IllegalArgumentException("Invalid array length "+ms.length
        + ", should be "+channels);

    puts++;
    bIndex = (bIndex+1)%bufferLength;
    for (int channel=0; channel<channels; channel++)
      msBuffer[channel][bIndex] = ms[channel];
  }
  
  /**
   * Returns an array of calibration gain factors.
   * 
   * @return An array of {@link #channels} gain values to multiply incoming 
   *         audio samples with.
   * @throws IllegalStateException
   *           if {@link #putRms(float[])} was called less than {@link #bufferLength}
   *           times
   */
  public synchronized float[] getGains()
  throws IllegalStateException
  {
    if (puts<bufferLength)
      throw new IllegalStateException("Insufficient data (too few puts)");
    
    // 1. Compute long-term mean of squares for all channels
    double[] ltMs = new double[channels];
    Arrays.fill(ltMs,0);
    for (int channel=0; channel<channels; channel++)
    {
      for (int i=0; i<bufferLength; i++)
        ltMs[channel] += msBuffer[channel][i];
      ltMs[channel] /= bufferLength;
    }

    // 2. Compute global mean RMS
    double gltRms = 0;
    for (int channel=0; channel<channels; channel++)
      gltRms += Math.sqrt(ltMs[channel]);
    gltRms /= channels;
    
    // 3. Compute gain factors
    float gains[] = new float[channels];
    for (int channel=0; channel<channels; channel++)
      gains[channel] = (float)(gltRms/Math.sqrt(ltMs[channel]));
    
    return gains;
  }
  
}