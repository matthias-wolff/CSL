package de.tucottbus.kt.csl.zombie.hardware.audio.device;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * @deprecated Must be either revised to match {@link AHardware} specification 
 * and moved to {@code de.tucottbus.kt.csl.hardware} or deleted!
 */
public class AudioOutput extends IAudioDevice{
  private final static String CLASSKEY = "Audio Output Device";

  /**
   * Array of available output audio channels
   */
  private final static String[] OUTPUT_DEVICES = {
      "Lautsprecher (High Definition",
      "Ausgang (Scarlett 8i6 USB)", "Lautsprecher (RME Hammerfall DSP MADI)",
      "Line 3/4 (M-Audio M-Track Eight)" };
  
  /**
   * Selected audio output channel of availableOutputs[] array
   */
  private final static int SELECTED_OUTPUT = 3;
  
  /**
   * Audio format for audio output stream
   */
  private AudioFormat audioFormat = getAudioFormat();

  /**
   * Data line stream for sound output
   */
  private SourceDataLine dataline;
  
  /**
   * Boolean to check output state
   */
  private boolean outputAvailableBool = false;
  
  private static AudioOutput instance;
  
  private AudioOutput(){
      findAudioDevice();
  }
  
  public static AudioOutput getInstance () {
    if (instance == null) {
      instance = new AudioOutput();
    }
    return instance;
  }
  
  /**
   * This method finds the selected audio output device of the array
   * availableOutputDevices[] and initialize it as a SourceDataLine.
   * 
   * However, should it not find a matching device, it takes some one.
   * 
   * @throws LineUnavailableException
   * 
   */
  protected boolean findAudioDevice(){
    try {
      dataline = getOutputSourceDataLine(OUTPUT_DEVICES[SELECTED_OUTPUT]);
      return true;
    } catch (LineUnavailableException e) {
      Log.err(CLASSKEY,"LineUnavailableException error: "+e.getMessage(),e);
      return false;
    }
  }
  
  /**
   * 
   * @param deviceName
   * @return
   * @throws LineUnavailableException
   */
  public SourceDataLine getOutputSourceDataLine(String deviceName) throws LineUnavailableException{
    boolean defaultMixer = true;
    SourceDataLine dataline = null;
    Mixer.Info[] mixers = AudioSystem.getMixerInfo();
    for (int i = 0; i < mixers.length; i++) {
      Mixer mixer = AudioSystem.getMixer(mixers[i]);
      Line.Info lineInfo = new Line.Info(defaultMixer ? SourceDataLine.class
          : TargetDataLine.class);
      
      if (mixer.isLineSupported(lineInfo)) {
        if (mixers[i].getName().contains(deviceName)) {         
          Log.info(CLASSKEY, "Initilized: " + mixers[i].getName()+ ", Description: ("+ mixers[i].getDescription()+")");
          dataline = AudioSystem.getSourceDataLine(audioFormat, mixers[i]);
          defaultMixer = false;
          break;
        }
      }
    }

    if (defaultMixer) {
      Log.err(CLASSKEY,"Using default ouput device (whatever that is).");
      dataline = AudioSystem.getSourceDataLine(audioFormat);
    }
    
    return dataline;
  }
  
  /**
   * Enable the audio output stream for headphones
   * 
   * @throws LineUnavailableException
   */
  public void startOutputStream() throws LineUnavailableException {
    dataline.open(audioFormat);
    if (dataline.isRunning() == false)
      dataline.start();
    outputAvailableBool = true;
    Log.info(CLASSKEY, "Output stream started");
  }

  /**
   * Disable the audio output stream for muting the headphones
   * 
   * @throws LineUnavailableException
   */
  private void stopOutputStream() {
      dataline.flush();
      dataline.stop();
      dataline.close();
      outputAvailableBool = false;
      Log.info(CLASSKEY,"Asio output device "+OUTPUT_DEVICES[SELECTED_OUTPUT]+" stopped.");
  }
  
  public void sendAudioData(byte[] buffer){
    if (outputAvailableBool == true)
      dataline.write(buffer, 0, buffer.length);
  }

  @Override
  public void dispose() {
    stopOutputStream();
    dataline = null;
  }

  @Override
  public boolean isDisposed() {
    if(dataline == null)
      return true;
    return false;
  }

  @Override
  public String getName() {
    return CLASSKEY;
  }

  @Override
  public boolean isConnected() {
    return outputAvailableBool;
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) {
    // TODO Auto-generated method stub
    return null;
  }

}
