package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb;

import de.tucottbus.kt.csl.hardware.ACompositeHardware;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.audio.output.MAudioDeviceLine34;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;

/**
 * Implementation of a delay and sum beamformer.
 * 
 * @author Martin Birth
 */
public abstract class DSBeamformer extends ACompositeHardware {

  // -- constants --
  /**
   * Record gain to amplify the input volume
   */
  private static final float OUT_GAIN = 70000;

  // -- declarations --
  
  /**
   * The ASIO input device supplying the microphone input channels.
   */
  protected RmeHdspMadi hammerfallAudioDevice;
  
  /**
   * Audio output device
   */
  protected MAudioDeviceLine34 mAudioDeviceLine34;

  private static int numFrames = AudioInputConstants.FRAME_SIZE;
  private static int channels = RmeHdspMadi.CHANNEL_COUNT;
  
  private float outputLevel;

  /**
   * Buffer array for audio output
   */
  protected final float[] outFloatBuffer = new float[numFrames];

  /**
   * Delay buffer for carry over the samples
   */
  private final float[] delayBuffer = new float[numFrames * channels * 2];

  // -- Core of the audio implementation --

  /**
   * Step 1: Delay of the samples in inBuffer of the Steering Vector and writing back
   * the delayed samples.<br>
   * Step 2: Summing all samples to a mono signal and output the data to a byte output
   * stream.
   * 
   * @throws InterruptedException
   */
  protected float[] delayAndSumBeamforming(MicArrayState state, float[] inputBuffer) {
    for (int i = numFrames * channels; i < delayBuffer.length; i++)
      delayBuffer[i] = 0f;

    // delaying
    for (int channel = 0; channel < channels; channel++) {
      int steer = (int) state.delays[channel];
      for (int sample0 = channel, sample1 = channel + steer * channels; (sample1 < numFrames
          * channels * 2)
          && (sample0 < numFrames * channels); sample0 = sample0 + channels, sample1 = sample1
          + channels) {
        delayBuffer[sample1] = inputBuffer[sample0];
      }
    }

    // to pass the audio data on for output
    System.arraycopy(delayBuffer, 0, inputBuffer, 0, inputBuffer.length);
    // carry over for next buffer
    System.arraycopy(delayBuffer, inputBuffer.length, delayBuffer, 0,
        inputBuffer.length);

    float[] calibrationGains = hammerfallAudioDevice.getCalibrationGains();
    
    // summing the float samples to byte buffer
    for (int frame = 0, nout = 0; frame < numFrames; frame++) {
      float sample = 0f;
      float deactivatedCount = 0;
      for (int nin = frame * channels, channel = 0; channel < channels; nin++, channel++) {
        if (state.activeMics[channel] == false) {
          continue;
        }
        if (inputBuffer[nin] == 0)
          deactivatedCount++;
        sample += calibrationGains[channel] * inputBuffer[nin] * state.gains[channel] * state.steerVec[channel];
      }

      sample = 1.5f * (sample / (state.numberOfActiveMics - deactivatedCount));
      outFloatBuffer[nout++] = sample * OUT_GAIN;
    }
    
    setBeamformerOutputLevel(outFloatBuffer);
    
    return outFloatBuffer;
  }
  
  /**
   * Setting the level value of the beamformer output buffer.
   * @param buffer float[]
   */
  private void setBeamformerOutputLevel(float[] buffer) {
    float level=0;
    for (int i = 0; i < buffer.length; i++)
      level+= buffer[i]/buffer.length;
    
    outputLevel=(float)(10*Math.log10(level));
  }
  
  /**
   * Get the output level of the beamformer.
   * @return
   */
  public float getBeamformerOutputLevel() {
    return outputLevel;
  }
  
  /**
   * Sending the beamformer output to hardware ouput interface.
   * @param audioData
   */
  protected abstract void sendAudioToOutput(float[] audioData);
  
  /**
   * Get the beamformer output audio data.
   * @return float[]
   */
  public abstract float[] getAudioOutputData();
}
