package de.tucottbus.kt.csl.hardware.audio.output;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.audio.utils.SampleTypeConverter;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * Abstract class for audio output devices running on the Java Sound API.
 * @author Martin Birth
 * @author Peter Geßler
 *
 */
public abstract class AAudioOutputDevice extends AAtomicHardware implements
    Runnable {

  /**
   * Default Sample Rate
   */
  private static final int SAMPLERATE = 44100;

  /**
   * Size of the SourceDataline buffer
   */
  public static final int BUFFER_SIZE = 2048; // in Bytes

  /**
   * Parameters for audio format
   */
  private static final int SAMPLE_BIT_SIZE = 16;
  private static final boolean BIG_ENDIAN = false;

  /**
   * Number of audio channels
   */
  private final int channels;

  /**
   * Name of audio device.
   */
  private final String deviceName;

  /**
   * Audio format for audio output stream
   */
  private AudioFormat audioFormat;

  /**
   * Data line stream for sound output
   */
  private SourceDataLine dataline;

  /**
   * Audio output gain
   */
  private float gain = 1f;

  /**
   * Manages the audio connection.
   */
  protected Thread guard;

  /**
   * Boolean to control the thread loops
   */
  private boolean runGuard = false;
  
  /**
   * show device list only one time
   */
  private static boolean displayDeviceList = true;

  /**
   * Constructor.
   * <br><br>
   * Please do <b>NOT</b> use the constructor direct. Use it only as singleton.
   * 
   * @param deviceName
   * @param channelsNum
   *          Numbers of channels to use.
   */
  protected AAudioOutputDevice(String deviceName, int channelsNum) {
    this.deviceName = deviceName;
    this.channels = channelsNum;

    // starting the guard thread
    guard = new Thread(this, getClass().getSimpleName() + ".guard");
    guard.setPriority(Thread.MAX_PRIORITY);
    guard.start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run() {
    runGuard = true;
    final int sleepMillis = 10;
    int ctr = 0;

    while (runGuard) {
      try {
        // - Initialize connection
        audioFormat = new AudioFormat(SAMPLERATE, SAMPLE_BIT_SIZE, channels,
            true, BIG_ENDIAN);

        dataline = getOutputSourceDataLine(deviceName);

        if (dataline != null) {
          dataline.open(audioFormat, BUFFER_SIZE);
          dataline.start();
          log("Audio output device (" + deviceName + ") is running.");
          setChanged();
          notifyObservers(NOTIFY_CONNECTION);
        }

        // - Run connection
        while (runGuard) {
          try {
            Thread.sleep(sleepMillis);
          } catch (InterruptedException e) {}
          if (!runGuard)
            break;

          if (!isConnected())
            break;

          ctr += sleepMillis;
          if (ctr >= 1000) {
            ctr = 0;
            setChanged();
            notifyObservers(NOTIFY_STATE);
          }
        }

        // - End connection
        stopOutputStream();
      } catch (Exception e) {
        e.printStackTrace();
        logErr(e.getMessage(), e);
        dispose();
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try {
          Thread.sleep(300);
        } catch (InterruptedException e1) {}
      }
    }
  }

  /**
   * This method finds the selected audio output device of the array and
   * initialize it as a SourceDataLine.
   * 
   * @param deviceName
   * @return
   * @throws LineUnavailableException
   */
  protected SourceDataLine getOutputSourceDataLine(String deviceName)
      throws LineUnavailableException {

    SourceDataLine sourceDataLine = null;
    // FIXME: there is no dispose() method in AudioSystem (state is stored in
    // memory)
    Info[] infos = AudioSystem.getMixerInfo();

    for (Info mixerInfo : infos) {
      if (mixerInfo.getName().equals(deviceName)) {
        sourceDataLine = AudioSystem.getSourceDataLine(audioFormat, mixerInfo);
        break;
      }
    }
    
    if ((sourceDataLine == null) && displayDeviceList) {
      
      displayDeviceList = false;
      
      Log.err("Audio device with name " + deviceName + " wasn't founded. Please check device list!");
      
      try {
        // Get and display a list of
        // available mixers.
        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        Log.err("Audio device list:");
        for (int cnt = 0; cnt < mixerInfo.length; cnt++) {
          Log.err(mixerInfo[cnt].getName());
        }// end for loop
      } catch (Exception e) {

      }
    }
      
    return sourceDataLine;
  }

  /**
   * Check is deviceName in Mixer
   * 
   * @param deviceName
   * @return boolean
   * @see #isConnected()
   * @see #getOutputSourceDataLine(String)
   */
  private boolean isSourceDataLineInMixer(String deviceName) {
    for (Info info : AudioSystem.getMixerInfo()) {
      if (info.getName().equals(deviceName))
        return true;
    }
    return false;
  }

  /**
   * Sending float audio data to the audio device.
   * <br>
   * The float values will be converted to byte values and give it
   * the the output. 
   * 
   * @param inBuffer
   *          byte[] of audio samples
   * @see SampleTypeConverter#floatToByte(byte[], float[], boolean, float)
   */
  public void sendAudioData(float[] inBuffer) {
    if (isConnected()) {
      byte[] outBuffer = new byte[inBuffer.length * 2];
      SampleTypeConverter.floatToByte(outBuffer, inBuffer, BIG_ENDIAN, gain);
      dataline.write(outBuffer, 0, outBuffer.length);
    }
  }
  
  /**
   * Sending audio data in byte[] format to the audio device.
   * <br><br>
   * ATTENTION: please note note that the {@link #SAMPLE_BIT_SIZE} 
   * is {@value #SAMPLE_BIT_SIZE} bit long <br>
   * and the byte order {@link #BIG_ENDIAN} is set to {@value #BIG_ENDIAN}
   * 
   * @param outBuffer
   *          byte[] of audio samples
   * @see #sendAudioData(float[])
   */
  public void sendAudioData(byte[] outBuffer) {
    if (isConnected()) {
      dataline.write(outBuffer, 0, outBuffer.length);
    }
  }

  /**
   * Disable the audio output stream for muting the headphones
   * 
   * @throws LineUnavailableException
   */
  private void stopOutputStream() {
    if (dataline != null) {
      dataline.flush();
      dataline.stop();
      dataline.close();
      dataline = null;
      log("Asio output device " + deviceName + " stopped.");
      setChanged();
      notifyObservers(NOTIFY_CONNECTION);
    }
  }

  /**
   * Setting the output gain.
   * 
   * @param outputGain
   *          The value can only be between 0 to 1.
   */
  public void setOutputGain(float outputGain) {
    gain = (outputGain < 0) ? 0 : ((outputGain > 1) ? 1 : outputGain);
  }
  
  public float getOutputGain() {
    return gain;
  }

  /**
   * {@inheritDoc}
   */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return deviceName;
  }

  /**
   * TODO: There is a problem with dataline.isRunning() / dataline.isActive()!
   * Both doesn't really work, waiting for Java-API update. Alternatively,
   * searching for an existing Source in the Mixer.
   * 
   */
  @Override
  public boolean isConnected() {
    if (isDisposed())
      return false;
    if (!runGuard)
      return false;
    // if (dataline.isRunning()) return false; // TODO: See above
    if (dataline == null)
      return false;
    if (!isSourceDataLineInMixer(deviceName))
      return false;
    return dataline.isOpen();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) {
    // TODO Auto-generated method stub
    return null;
  }

}
