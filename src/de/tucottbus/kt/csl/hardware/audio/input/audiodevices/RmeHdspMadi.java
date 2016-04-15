package de.tucottbus.kt.csl.hardware.audio.input.audiodevices;

import java.util.Arrays;

import com.github.rjeschke.jpa.PaBuffer;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.audio.RmsLevelingCalibrator;
import de.tucottbus.kt.csl.hardware.audio.input.AHammerfallAudioDevice;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.Beamformer3D;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.MicrophoneGCCTester;

public class RmeHdspMadi extends AHammerfallAudioDevice{

  /**
   * Number of physical audio lines
   */
  public static final int CHANNEL_COUNT = 64;

  public static final String NOTIFY_RMS = "NOTIFY_RMS";
  
  private float[] calibrationGains = new float[CHANNEL_COUNT];
  
  private boolean calibrated = false;
  
  private final RmsLevelingCalibrator calibrator;
  
  private static RmeHdspMadi singleton = null;
  
  /**
   * Private constructor -> please use singleton method {@link #getInstance()}
   */
  private RmeHdspMadi() {
    super(CHANNEL_COUNT);
    calibrator = new RmsLevelingCalibrator(CHANNEL_COUNT,100);
    Arrays.fill(calibrationGains,1f);
    setDefaultAudioFormat();
  }
  
  /**
   * Singleton method to get the {@link HammerfallAudioDevice_Zombie} object
   * @return HammerfallAudioDevice
   */
  public static synchronized RmeHdspMadi getInstance() {
    if (singleton == null)
      singleton = new RmeHdspMadi();
    return singleton;
  }

  
//-- Implementation of APortAudioInputDevice --

 @Override
 public AHardware getParent() {
   return Beamformer3D.getInstance();
 }
 
 /**
  * This method is called by the JPA and fills the temporary audio data buffer,
  * calculates the audio levels for the calibration and for the RMS value {@link #getRMS(int)}.
  * 
  * @param inputBuffer
  *          PaBuffer - Reads audio data from the JPA.
  * @param outputBuffer
  *          PaBuffer - Writes audio data to the JPA.
  * @param numFrames
  *          length of a audio output frame
  *          
  * @see #getRMS(int)
  * @see #getUnsortedAudioBuffer()
  * @see #getSortedAudioBuffer()
  */
  @Override
  protected void callback(PaBuffer inputBuffer, PaBuffer outputBuffer, int numFrames) {
    if (inputBuffer.getSampleSize() == 0) return;
    
    inputBuffer.getFloatBuffer().get(inBuffer);
    
    // Calculate all MS (mean of squares) values
    Arrays.fill(levelsMs,0f);
    for (int sample = 0, channel = 0; sample < AudioInputConstants.FRAME_SIZE * CHANNEL_COUNT; sample++, channel++) {
      levelsMs[channel] += (inBuffer[sample] * inBuffer[sample]);
      if (channel == CHANNEL_COUNT - 1)
        channel = -1;
    }
    
    for (int i = 0; i < CHANNEL_COUNT; i++)
      levelsMs[i] = levelsMs[i]/AudioInputConstants.FRAME_SIZE;
    calibrator.putMs(levelsMs);
    
    for (int i = 0; i < CHANNEL_COUNT; i++)
      levelsMs[i]*=(calibrationGains[i]*calibrationGains[i]);
    setChanged();
    notifyObserversAsync(NOTIFY_RMS);
    
    // Split inBuffer into 64 outBuffers and calibrate samples
    int n=0;
    for(int s=0;s<AudioInputConstants.FRAME_SIZE;s++){
      for(int c=0;c<CHANNEL_COUNT;c++){
        outBuffer[c][s] = calibrationGains[c] * inBuffer[n] * volumeMixer[c];
        n++;
      }
    }
    
    // notify all listers, new data available
    notifyListeners();
    
    // "don't call us, we call u"
    Beamformer3D.getInstance().sendAudioToBeamformer(inBuffer);
    DoAEstimator.getInstance().addAudioDataToQueue(outBuffer);
    
    if(MicrophoneGCCTester.dataAddingAllowed)
      MicrophoneGCCTester.getInstance().addAudioDataToQueue(outBuffer);
  }
  
  /**
   * Performs an {@linkplain RmsLevelingCalibrator RMS leveling calibration} of 
   * the microphone input channels.
   * 
   * @returns An array of calibration gain factors, one per channel.
   * @throws IllegalStateException
   *           if the calibrator has not yet collected sufficient data for 
   *           calibration.
   * @see #isCalibrated()
   */
  public float[] calibrate() throws IllegalStateException {
    calibrationGains = calibrator.getGains();
    calibrated = true;
    return calibrationGains;
  }
  
  /**
   * Determines if the microphone input channels are calibrated.
   * 
   * @see #calibrate()
   */
  public boolean isCalibrated() {
    return calibrated;
  }
  
  /**
   * Get the calibration gains
   * @return float[] of calibration factors
   */
  public float[] getCalibrationGains(){
    return calibrationGains;
  }
  
  public static void main(String[] args) {
    // TODO Auto-generated method stub

  }

}
