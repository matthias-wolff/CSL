package de.tucottbus.kt.csl.hardware.audio.input.audiodevices;

import de.tucottbus.kt.csl.hardware.audio.input.IAudioListener;
import de.tucottbus.kt.csl.hardware.audio.output.AAudioOutputDevice;
import de.tucottbus.kt.csl.hardware.audio.output.MAudioDeviceLine12;

/**
 * Concrete implementation of shotgun device. This device is connected
 * with Hammerfall RME HDSP AES32 audio device.
 * 
 * @author Peter Gessler
 *
 */
public class Shotgun implements IAudioListener{

  private String channelName;
  
  private int channel;

  private float[] channelBuffer;
  
  private RmeHdspAes32 device;
  
  private AAudioOutputDevice outputDevice;
  
  public Shotgun(String channelName, int channel, AAudioOutputDevice outputDevice) {
    
    this.channelName = channelName;
    this.channel = channel;
    this.outputDevice = outputDevice;
    
    device = RmeHdspAes32.getInstance();
  }

  @Override
  public String getName() {
    return channelName;
  }
  
  /**
   * Add shoutgun as Hammerfall listener
   */
  public void observeAudioDevice() {
    device.addAudioListener(this);
  }
  
  /**
   * Remove shotgun as Hammerfall listener
   */
  public void removeListener() {
    device.removeAudioListener(this);
  }

  @Override
  public void notifyAudioEvent() {
    channelBuffer = device.getChannelBuffer(channel);
    outputDevice.sendAudioData(channelBuffer);
  }
  
  /**
   * Set gain of shotgun. Standard is 0 dB.
   * @param gain
   */
  public void setGain(int gain) {
    RmeHdspAes32.getInstance().setGain(channel, gain);
  }
  
  // ----! Test only !----
  public static void main(String[] args) {
    
    Shotgun shotgun = new Shotgun("Shoutgun Mainviewer", 0, MAudioDeviceLine12.getInstance());
    shotgun.observeAudioDevice();
    
    Thread testGain = new Thread(new Runnable() {
      
      @Override
      public void run() {
        try {
          
          Thread.sleep(5000);
          shotgun.setGain(24);
          
        } catch (Exception e)
        {
          
        }       
      }
    });
    testGain.start();
    
    
  }
}
