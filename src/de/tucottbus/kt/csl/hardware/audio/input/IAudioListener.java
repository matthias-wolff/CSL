package de.tucottbus.kt.csl.hardware.audio.input;

/**
 * Interface to connect concrete audio hardware devices like shotgun
 * with audio input device like Hammerfall soundcard.
 * 
 * @author Peter Gessler
 *
 */

public interface IAudioListener {

  /**
   * listener name
   * @return
   */
  public String getName();
  
  /**
   * If new data from soundcard are retrievable,
   * method throw signal.<br> Buffer with data are available
   * with AHammerfallAudioDevice.getOutBuffer()<br> or
   * AHammerfallAudioDevice.getChannelBuffer(int channelNum)
   */
  public void notifyAudioEvent();
}
