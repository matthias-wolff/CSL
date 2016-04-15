package de.tucottbus.kt.csl.hardware.audio.input.audiodevices;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;

import com.github.rjeschke.jpa.PaBuffer;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.audio.input.APortAudioInputDevice;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.output.MAudioDeviceLine12;
import de.tucottbus.kt.csl.hardware.audio.output.MAudioDeviceLine34;

/**
 * Concrete implementation of the APortAudioInputDevice
 * to get a audio stream from the physical audio hardware
 * called Hammerfall DSP.
 * 
 * @author Martin Birth
 *
 */
public class MAudioMTrackEightAudioInputDevice extends APortAudioInputDevice {

  private static final String DEVICE_NAME = "M-Audio M-Track Eight";
  
  private static final int CHANNEL_COUNT = 8;

  public static final String NOTIFY_RMS = "NOTIFY_RMS";
  
  private final float[] inBuffer = new float[AudioInputConstants.FRAME_SIZE * CHANNEL_COUNT];
  
  private final float[][] outBuffer = new float[CHANNEL_COUNT][AudioInputConstants.FRAME_SIZE];

  private final float[] levelsMs = new float[CHANNEL_COUNT];
  
  private static volatile MAudioMTrackEightAudioInputDevice singleton = null;
  

  public static synchronized MAudioMTrackEightAudioInputDevice getInstance() {
    if (singleton == null)
      singleton = new MAudioMTrackEightAudioInputDevice();
    return singleton;
  }

  private MAudioMTrackEightAudioInputDevice() {
    
    super(DEVICE_NAME, CHANNEL_COUNT);
    
    setAudioFormat(new AudioFormat(16000,
        16, 1, true, false)); 
  }
    
  // -- Implementation of APortAudioInputDevice --

  @Override
  public AHardware getParent() {
    return null;
  }
  
  //private static MAudioDeviceLine34 audio34;

  /**
   * This method involves all audio data, calling by the JPA library.
   * 
   * @param inputBuffer
   *          PaBuffer
   * @param outputBuffer
   *          PaBuffer
   * @param numFrames
   *          length of output frames
   */
  @Override
  protected void callback(PaBuffer inputBuffer, PaBuffer outputBuffer, int numFrames) {
    if (inputBuffer.getSampleSize() == 0) return;
    
    inputBuffer.getFloatBuffer().get(inBuffer);
    
    // Calculate all MS (mean of squares) values
    Arrays.fill(levelsMs,0f);
    for (int sample = 0, channel = 0; sample < AudioInputConstants.FRAME_SIZE * CHANNEL_COUNT; sample++, channel++) {
      levelsMs[channel] += (inBuffer[sample] * inBuffer[sample]);
      if (channel == CHANNEL_COUNT - 1)
        channel = -1;
    }
    
    for (int i = 0; i < CHANNEL_COUNT; i++)
      levelsMs[i] = levelsMs[i]/AudioInputConstants.FRAME_SIZE;
    
    // Split inBuffer into n (CHANNEL_COUNT) outBuffers
    int n=0;
    for(int s=0;s<AudioInputConstants.FRAME_SIZE;s++){
      for(int c=0;c<CHANNEL_COUNT;c++){
        outBuffer[c][s] = inBuffer[n];
        n++;
      }
    }
    
    //audio12.sendAudioData(outBuffer[7]);
    //audio34.sendAudioData(outBuffer[0]);
  }
  
  /**
   * {@link APortAudioInputDevice#isInBufferZero()}
   */
  @Override
  protected boolean isInBufferZero(){
    int counter = 0;
    for (int i = 0; i < inBuffer.length; i ++)
        if (inBuffer[i] == 0)
            counter ++;
    
    if(counter==inBuffer.length) return true;
    return false;
  }
  
  /**
   * Returns the current RMS of an input channel.
   * 
   * @param channel
   *          The zero-based channel index.
   * @return The RMS, [0...1].
   */
  public float getRMS(int channel){
    return (float)Math.sqrt(levelsMs[channel]);
  }
  
  /**
   * Returns the current level of an input channel.
   *  
   * @param channel
   *          The zero-based channel index.
   * @return The level, [-&infin;...0] dB.
   */
  public float getLevel(int channel){
    return (float)(10*Math.log10(levelsMs[channel]));
  }
    
  // -- Main method (debugging and testing only!) --
  @SuppressWarnings("unused")
  private static MAudioDeviceLine12 audio12;
  @SuppressWarnings("unused")
  private static MAudioDeviceLine34 audio34;
  
  public static void main(String[] args) {
    audio12 = MAudioDeviceLine12.getInstance();
    audio34 = MAudioDeviceLine34.getInstance();
    //MAudioMTrackEightAudioInputDevice.getInstance();
  }

}
