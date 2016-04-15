package de.tucottbus.kt.csl.zombie.hardware.audio.device;

import javax.sound.sampled.AudioFormat;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.zombie.hardware.Hardware;

/**
 * @deprecated Must be either revised to match {@link AHardware} specification 
 * and moved to {@code de.tucottbus.kt.csl.hardware} or deleted!
 */
public abstract class IAudioDevice extends Hardware{
  /**
   * Default Sample Rate
   */
  private static final int SAMPLERATE = 44100;
  
  /**
   * All variables of a Java audio stream.
   * 
   * @return Returns the audio format for an audio stream.
   */
  public static AudioFormat getAudioFormat() {
    int sampleSizeInBits = 16; // 8,16
    int channels = 1; // 1,2
    boolean signed = true;
    boolean bigEndian = false;
    return new AudioFormat(SAMPLERATE, sampleSizeInBits, channels, signed,
        bigEndian);
  }
  
  /**
   * 
   * @return
   */
  public static int getSampleRate(){
    return SAMPLERATE;
  }
  
  /**
   * Abstract method to find a common audio device
   */
  protected abstract boolean findAudioDevice();  
}
