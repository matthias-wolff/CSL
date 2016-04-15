package de.tucottbus.kt.csl.hardware.audio.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.io.IOUtils;

import de.tucottbus.kt.lcars.logging.Log;

/**
 * Resampling audio data into another sampling frequency.
 * FIXME: @Peter the output array contained apparently incorrect values
 * 
 * @author Peter Geßler
 * @author Martin Birth
 */
public class Resampler {

  /**
   * Input buffer in bytes
   */
  private final ByteBuffer byteBuffer;
  
  /**
   * AudioFormat of the target audio data.
   */
  private final AudioFormat targetFormat;
  
  /**
   * AudioFormat of the source audio data.
   */
  private final AudioFormat sourceFormat;
  
  /**
   * Factor for float to byte converting
   */
  private final int BYTE_FACTOR = 4;

  /**
   * Constructor method with initialization.
   * @param channels - int, channel count
   * @param inputDataLength - int, length of the input data
   * @param currentFormat - int, current Audioformat that's running
   * @param targetSampleRate - int, target samplerate, choose 8000 or 16000
   */
  public Resampler(int inputDataLength, AudioFormat format, int targetSampleRate) {
    sourceFormat = new AudioFormat(
        format.getEncoding(),
        format.getSampleRate(),
        format.getSampleSizeInBits(),
        1, // resampling only one by one channel 
        format.getFrameSize(),
        format.getFrameRate(),
        format.isBigEndian());
    
    byteBuffer = ByteBuffer.allocate(BYTE_FACTOR * inputDataLength);
    
    // create audio format with target sample rate
    targetFormat = new AudioFormat(
        format.getEncoding(),
        targetSampleRate,
        sourceFormat.getSampleSizeInBits(),
        sourceFormat.getChannels(),
        sourceFormat.getFrameSize(),
        targetSampleRate,
        sourceFormat.isBigEndian());
  }
  
  /**
   * Get the target AudioFormat
   * @return AudioFormat
   */
  public AudioFormat getTargetAudioFormat(){
    return targetFormat;
  }

  /**
   * Converting float array to byte array
   * 
   * @param values
   *          - float[] values
   * @return byte[] array
   */
  private byte[] floatArray2ByteArray(float[] values) {
    byteBuffer.clear();
    for (float value : values) {
      byteBuffer.putFloat(value);
    }

    return byteBuffer.array();
  }
  
  /**
   * Converting byte array into a float array.
   * @param dataInputStream - DataInputStream
   * @return float[] array
   * @throws IOException 
   */
  private float[] byteArray2FloatArray(DataInputStream dataInputStream, int length) {
    float[] targetFloatArray = new float[length/BYTE_FACTOR];
    try {
      for (int value = 0; value < targetFloatArray.length; value++) {
        if(dataInputStream.hashCode()>0){
          targetFloatArray[value] = dataInputStream.readFloat();
          System.out.println(value);
        }
      }
    } catch (IOException e) {
      Log.err(e.getMessage(),e);
    }
    return targetFloatArray;
  }
  
  /**
   * Get the resampled audio data only of the channels, which are selected from the ChannelSelector
   * @param inputAudioData, float[][]
   * @param selector - {@link ChannelSelector2}}
   * @return float[][]
   */
  public float[][] getDownSampledAudioData(float[][] inputAudioData, boolean[] channelSelection){
    int channelCount=0;
    for (int i = 0; i < channelSelection.length; i++) {
      channelCount = (channelSelection[i]==true) ? channelCount+1 : channelCount; 
    }
    
    float[][] outputData = new float[channelCount][inputAudioData[0].length];
    
    int j=0;
    for (int i = 0; i < inputAudioData.length; i++) {
      if(channelSelection[i]){
        System.arraycopy(downsampling(inputAudioData[j]), 0, outputData[j], 0, inputAudioData[0].length);
        j++;
      }
    }
    
    return outputData;
  }

  /**
   * Method edit samplerate of given float buffer.
   * 
   * @param audioDataInput
   *          - buffer of floats
   * @return float[]
   * @throws IOException
   */
  private float[] downsampling(float[] audioDataInput) {
    byte[] dataInByte = floatArray2ByteArray(audioDataInput);
    
    // convert byte array and create new audiostream to store these in
    ByteArrayInputStream bais = new ByteArrayInputStream(dataInByte);
    AudioInputStream outputAIS = new AudioInputStream(bais, sourceFormat,
        dataInByte.length / targetFormat.getFrameSize());
    AudioInputStream convertedIn = AudioSystem.getAudioInputStream(
        targetFormat, outputAIS);

    ByteArrayOutputStream targetByteArray = new ByteArrayOutputStream();
    try {
      IOUtils.copy(convertedIn, targetByteArray);
    } catch (IOException e) {
      Log.err(e.getMessage(), e);
    }
    
    try {
      targetByteArray.flush();
      targetByteArray.close();
    } catch (IOException e) {
      Log.err(e.getMessage(), e);
    }

    // convert targetByteArray to targetFloatarray
    bais = new ByteArrayInputStream(targetByteArray.toByteArray());
    DataInputStream dataInputStream = new DataInputStream(bais);
    
    return byteArray2FloatArray(dataInputStream, dataInByte.length);
  }
}
