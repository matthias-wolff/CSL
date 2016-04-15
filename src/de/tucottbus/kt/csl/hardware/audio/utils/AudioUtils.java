package de.tucottbus.kt.csl.hardware.audio.utils;

import java.io.File;
import java.io.IOException;

public class AudioUtils {

  /**
   * Calculating the root mean square (RMS) power over a single audio frame / channel.
   * 
   * @param raw - float[], input data
   * @return float - rms value of the input data
   */
  public static float getRMS(float[] raw) {
    float sum = 0;
    if (raw.length == 0)
      return sum;
    
    for (float sample : raw) {
      sum += sample * sample;
    }
    
    float rootMeanSquare = (float) Math.sqrt(sum / raw.length);

    return Math.max(rootMeanSquare, 1);
  }
  
  /**
   * Get the log10 of the rms
   * @param raw - float[], input data
   * @return float - rms value of the input data
   * @see #getRMS(float[])
   */
  public static float getlogRMS(float[] raw){
    float rms = getRMS(raw);
    return (float) (20*Math.log10(rms));
  }
  
  /**
   * Get float array with audio samples of a wave file.
   * @param filename - file path and name as string
   * @param sampleRate - int rate of samples
   * @return float[] array with samples
   */
  public static float[] getDataFromWaveFile(String filename,int sampleRate){
    
    File file = new File(filename);
    
    WavFile wavFile = null;
    try {
      wavFile = WavFile.openWavFile(file);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    double[] sampleBuffer = new double[sampleRate*5];
    try {
      wavFile.readFrames(sampleBuffer, sampleRate*5);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    float[] floatArray = new float[sampleBuffer.length];
    for (int i = 0 ; i < sampleBuffer.length; i++)
    {
        floatArray[i] = (float) sampleBuffer[i];
    }
    
    return floatArray;
  }
  
}
