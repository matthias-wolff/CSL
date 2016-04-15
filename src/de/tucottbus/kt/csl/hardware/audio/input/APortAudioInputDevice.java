package de.tucottbus.kt.csl.hardware.audio.input;

import javax.sound.sampled.AudioFormat;

import com.github.rjeschke.jpa.JPA;
import com.github.rjeschke.jpa.PaBuffer;
import com.github.rjeschke.jpa.PaCallback;
import com.github.rjeschke.jpa.PaDeviceInfo;
import com.github.rjeschke.jpa.PaError;
import com.github.rjeschke.jpa.PaHostApiInfo;
import com.github.rjeschke.jpa.PaHostApiType;
import com.github.rjeschke.jpa.PaSampleFormat;
import com.github.rjeschke.jpa.PaStreamParameters;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * Abstract PortAudio/Asio implementation using ({@link JPA}).
 * For more Informations please look at ({@link JPA#initialize()})
 * or the github page of Rene Jeschke or the JPA (JavaPortAudio) wiki page.
 * 
 * @see <a href="https://github.com/rjeschke/jpa">https://github.com/rjeschke/jpa</a>
 * @see <a href="http://javadoc.renejeschke.de/com/github/rjeschke/jpa/">http://javadoc.renejeschke.de/com/github/rjeschke/jpa/</a>
 * 
 * @author Martin Birth
 * @author Peter Gessler
 */
public abstract class APortAudioInputDevice extends AAtomicHardware implements Runnable{
  
  /**
   * PortAudio flags
   */
  protected static final int FLAGS = 0;
  
  /**
   * FIXME: disable permanent thread creation with 'JPA.enableThreadDetach(false);'
   */
  private static final boolean THREAD_ATTACH = Boolean.FALSE;
  
  /**
   * Parameters for audio format
   */
  private final int sampleSizeInBits = 16; // 8 or 16
  private final int channels = 1; // 1,2
  private final boolean signed = true;
  private final boolean bigEndian = false;
  
  /**
   * Audio format for audio output stream
   */
  private AudioFormat audioFormat;
  
  /**
   * Name of audio device.
   */
  private final String deviceName;
  
  /**
   * The number of audio input channels.
   */
  private final int audioChannels;
  
  /**
   * The ASIO device ID.
   */
  protected int deviceId;
  
  /**
   * Device info of the associated ASIO device ID.
   */
  private PaDeviceInfo di = null;
  
  /**
   * Type of stream
   */
  private final PaSampleFormat SAMPLE_FORMAT = PaSampleFormat.paFloat32;
  
  /**
   * Asio device parameters 
   */
  private PaStreamParameters streamParameters;
  
  /**
   * Error message of JPA ({@link com.github.rjeschke.jpa.PaError})
   */
  private PaError err = null;
  
  /**
   * Manages the audio connection.
   */
  protected Thread guard;
  
  /**
   * Boolean to control the thread loops
   */
  private boolean runGuard = Boolean.FALSE;
  
  
  /**
   * Creates a new PortAudio/Asio device wrapper.
   * 
   * @param deviceName
   *          Device name using by the system audio mixer.
   * @param audioChannels
   *          Numbers of channels to use.
   */
  protected APortAudioInputDevice(String deviceName, int audioChannels){
    this.deviceName=deviceName;
    this.audioChannels=audioChannels;
    
    if(deviceName==null){
      throw new IllegalArgumentException("Device name is null.");
    }
    
    // starting the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.setPriority(Thread.MAX_PRIORITY);
    guard.start();
  }
  
  @Override
  public void run() {
    runGuard = true;
    final int sleepMillis = 100;
    int ctr = 0;
    
    while(runGuard){
      try {
        // - Initialize connection
        err = JPA.initialize();
        deviceId = getAudioDeviceId(deviceName);
        setStreamParameters(deviceId, audioChannels);
        JPA.enableThreadDetach(THREAD_ATTACH);
        err = JPA.openStream(streamParameters, null, AudioInputConstants.SAMPLERATE,AudioInputConstants.FRAME_SIZE, FLAGS);

        if (!JPA.isStreamActive()) {
          err=JPA.startStream();
          log("Audio input stream started.");
        }
        
        PaCallback callback = new PaCallback() {
          @Override
          public void paCallback(PaBuffer inputBuffer, PaBuffer outputBuffer, int numFrames) {            
            callback(inputBuffer, outputBuffer, numFrames);
          }
        };
        JPA.setCallback(callback);
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        
        // - Run connection
        while(runGuard){
          try { Thread.sleep(sleepMillis); } catch (InterruptedException e) {}
          if (!runGuard)
            break;
          
          if (!isConnected()) break;
          
          ctr+=sleepMillis;
          if (ctr>=1000)
          {
            setChanged();
            notifyObservers(NOTIFY_STATE);
            ctr = 0;
          }
        }
        
        // - End connection
        if(JPA.isStreamActive()){
          err = JPA.stopStream();
          err = JPA.closeStream();
          err = JPA.terminate();
        }
        
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        
      } catch (Exception e) {
        logErr(JPA.getErrorText(err), e);
        
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try { Thread.sleep(1000); } catch (InterruptedException e1) {}
      }
    }
  }
  
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
  protected abstract void callback(PaBuffer inputBuffer, PaBuffer outputBuffer, int numFrames);
  
  /**
   * Searching for the ID of the audio device.
   * 
   * @param device
   *          String - device name
   * @return
   *        int - ID
   */
  protected int getAudioDeviceId(String device) {
    for (int dId = 0; dId < JPA.getDeviceCount(); dId++) {
      di = JPA.getDeviceInfo(dId);
      PaHostApiInfo hi = JPA.getHostApiInfo(di.getHostApi());

      if (hi.getType() != PaHostApiType.paASIO)
        continue;

      String diName = di.getName();
      log("Device #" + dId + ": " + diName + "(" + hi.getName()
          + "), Max channel: " + di.getMaxInputChannels());

      if (diName.contains(device)) {
        return dId;
      }
    }
    log("ERROR: There is no ASIO device named "+device+".");
    return 0;
  }
  
  /**
   * Setting all input parameters for
   * 
   * @param inParameters
   *          Create of input parameters.
   */
  private void setStreamParameters(int id, int channels) {
    streamParameters = new PaStreamParameters(id, channels, SAMPLE_FORMAT,
        di.getDefaultHighInputLatency());
  }
  
  @Override
  public void dispose() {
    if (guard!=null)
    {
      runGuard = false;
      guard.interrupt();
      try { guard.join(); } catch (Exception e) {}
    }
    super.dispose();
  }
  
  @Override
  public boolean isConnected() {
    if (isDisposed()) return false;
    if (isInBufferZero()==true) return false;
    return JPA.isStreamActive();
  }
  
  /**
   * Method returns true if each sample in inBuffer is zero,
   * else false. 
   * 
   * @return boolean
   * @see #isConnected()
   */
  protected abstract boolean isInBufferZero();

  @Override
  public String getName() {
    return deviceName;
  }

  @Override
  public AHardware getParent() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) {
    // TODO Auto-generated method stub
    return null;
  }

  protected void setDefaultAudioFormat(){
    audioFormat = new AudioFormat(AudioInputConstants.SAMPLERATE,
        sampleSizeInBits, channels, signed, bigEndian);
  }
  
  protected void setAudioFormat(AudioFormat format){
    audioFormat = format;
  }
  
  /**
   * Get the sample rate in an integer format.
   * 
   * @return Integer Value
   */
  public AudioFormat getAudioFormat(){
    return audioFormat;
  }

}
