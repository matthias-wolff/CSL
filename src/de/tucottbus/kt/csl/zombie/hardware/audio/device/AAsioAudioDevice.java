package de.tucottbus.kt.csl.zombie.hardware.audio.device;

import javax.sound.sampled.AudioFormat;

import com.github.rjeschke.jpa.JPA;
import com.github.rjeschke.jpa.PaSampleFormat;
import com.github.rjeschke.jpa.PaStreamParameters;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * @deprecated Must be either revised to match {@link AHardware} specification 
 * and moved to {@code de.tucottbus.kt.csl.hardware} or deleted!
 */
public abstract class AAsioAudioDevice extends IAudioDevice {
  private final static String CLASSKEY = "Asio Audio Device";
  
  /**
   * PortAudio flags
   */
  private static final int FLAGS = 0;
  
  /**
   * Physical input buffer length
   */
  private static final int FRAMES_PER_BUFFER = 512;
  
  PaStreamParameters streamParameters;
  
  /**
   * Audio format for audio output stream
   */
  private AudioFormat audioFormat = getAudioFormat();
  
  /**
   * Type of stream
   */
  protected static PaSampleFormat sample_param = PaSampleFormat.paFloat32;
  
  /**
   * Asio audio device constructor
   */
  protected AAsioAudioDevice(){
    JPA.initialize();
    findAudioDevice();
  }
  
  /**
   * find the desired device and set all audio parameters
   */
  protected abstract boolean findAudioDevice();
  
  /**
   * Create the audio stream parameters
   * @return
   */
  protected abstract PaStreamParameters createAudioParam();
  
  /**
   * Abstract callback method to get or set audio data from
   * asio audio device. Typical implementation:
   * 
   * public void audioCallback() {
   *   PaCallback callback = new PaCallback() {
   *   @Override
   *   public void paCallback(PaBuffer inputBuffer, PaBuffer outputBuffer,
   *       int arg2) {
   *     try {
   *       // ... do something here with the audio data
   *     } catch (InterruptedException e) {
   *       LCARS.err(CLASSKEY, "Error: " + e.getMessage());
   *     }
   *   }
   *  };
   *  JPA.setCallback(callback);
   * }
   * 
   */
  public abstract void audioCallback();
  
  /**
   * Opens an audio input stream for PortAudio.
   */
  public void startStream() {
    streamParameters = createAudioParam();
    printAudioParam(streamParameters);
    JPA.openStream(streamParameters, null, audioFormat.getSampleRate(),
        FRAMES_PER_BUFFER, FLAGS);

    if (!isConnected()) {
      JPA.startStream();
      Log.info(CLASSKEY, "Audio stream started.");
    }
  }
  
  /**
   * Closes an audio input stream for PortAudio.
   */
  public void stopStream() {
    if (JPA.isStreamActive()) {
      JPA.stopStream();
      JPA.closeStream();
    }
  }
  
  /**
   * Print out all audio parameters for selected set of inParameters.
   * 
   * @param inParameters
   *          Set of input parameters.
   */
  private static void printAudioParam(PaStreamParameters inParameters) {
    Log.info(CLASSKEY + " Parameter",
        "sample format: " + inParameters.getSampleFormat()
            + ", suggested latency: " + inParameters.getSuggestedLatency());
  }
  
  public static int getFramesPerBuffer() {
    return FRAMES_PER_BUFFER;
  }
  
  @Override
  public void dispose() {
    stopStream();
    JPA.terminate();
  }

  @Override
  public boolean isDisposed() {
    return JPA.isStreamStopped();
  }

  @Override
  public boolean isConnected() {
    return JPA.isStreamActive();
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) {
    // TODO Auto-generated method stub
    return null;
  }
  
}
