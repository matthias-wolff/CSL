package de.tucottbus.kt.csl.hardware.audio.output;

import de.tucottbus.kt.csl.hardware.AHardware;

/**
 * Concrete implementation of the M-Audio Fast Track 
 * audio device for audio output on line 1/2 (stereo).
 * 
 * @author Peter Gessler
 *
 */

public class MAudioDeviceLine12 extends AAudioOutputDevice {

  private final static String DEVICE_NAME = "Line 1/2 (M-Audio M-Track Eight)";

  private final static int CHANNELS_NUM = 1;

  private static volatile MAudioDeviceLine12 singleton = null;

  /**
   * Private Constructor -> please use {@link #getInstance()}
   */
  private MAudioDeviceLine12() {
    super(DEVICE_NAME, CHANNELS_NUM);
  }
  
  public static synchronized MAudioDeviceLine12 getInstance() {
    if (singleton == null)
      singleton = new MAudioDeviceLine12();
    return singleton;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public AHardware getParent() {
    // TODO Auto-generated method stub
    return null;
  }
  
}
