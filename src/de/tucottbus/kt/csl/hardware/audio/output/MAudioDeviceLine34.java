package de.tucottbus.kt.csl.hardware.audio.output;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.Beamformer3D;

/**
 * Concrete implementation of the M-Audio Fast Track 
 * audio device for audio output on line 3/4 (stereo).
 * 
 * @author Peter Gessler
 *
 */
public class MAudioDeviceLine34 extends AAudioOutputDevice {

  private final static String DEVICE_NAME = "Line 3/4 (M-Audio M-Track Eight)";

  private final static int CHANNELS_NUM = 1;

  private static volatile MAudioDeviceLine34 singleton = null;
  
  /**
   * Private Constructor -> please use {@link #getInstance()}
   */
  private MAudioDeviceLine34() {
    super(DEVICE_NAME, CHANNELS_NUM);
  }
  
  public static synchronized MAudioDeviceLine34 getInstance() {
    if (singleton == null)
      singleton = new MAudioDeviceLine34();
    return singleton;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public AHardware getParent() {
    return Beamformer3D.getInstance();
  }
  
}
