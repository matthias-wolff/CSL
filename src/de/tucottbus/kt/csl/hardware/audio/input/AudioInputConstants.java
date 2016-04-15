package de.tucottbus.kt.csl.hardware.audio.input;

/**
 * 
 * @author Martin Birth
 *
 */
public class AudioInputConstants {

  /**
   * Speed of sound (in m/s)
   */
  public final static double SPEED_OF_SOUND = 343.2;
  
  /**
   * Physical input buffer length (in audio samples).
   */
  public static final int FRAME_SIZE = 512; // 11.6 [ms]
  
  /**
   * Default Sample Rate
   */
  public static final int SAMPLERATE = 44100;
  
  /**
   * Only static constants in this class
   */
  private AudioInputConstants() {}
    
}
