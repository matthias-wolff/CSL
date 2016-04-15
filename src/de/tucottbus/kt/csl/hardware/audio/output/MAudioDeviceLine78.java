package de.tucottbus.kt.csl.hardware.audio.output;

import de.tucottbus.kt.csl.hardware.AHardware;

/**
 * Concrete implementation of the M-Audio Fast Track 
 * audio device for audio output on line 7/8 (stereo).
 * 
 * @author Peter Gessler
 */
public class MAudioDeviceLine78 extends AAudioOutputDevice {

  private final static String DEVICE_NAME = "Line 7/8 (M-Audio M-Track Eight)";

  private final static int CHANNELS_NUM = 1;

  private static volatile MAudioDeviceLine78 singleton = null;

  /**
   * Private Constructor -> please use {@link #getInstance()}
   */
  private MAudioDeviceLine78() {
    super(DEVICE_NAME, CHANNELS_NUM);
  }
  
  public static synchronized MAudioDeviceLine78 getInstance() {
    if (singleton == null)
      singleton = new MAudioDeviceLine78();
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
