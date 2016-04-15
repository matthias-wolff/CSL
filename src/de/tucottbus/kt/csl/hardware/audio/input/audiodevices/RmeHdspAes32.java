package de.tucottbus.kt.csl.hardware.audio.input.audiodevices;

import java.util.Arrays;

import com.github.rjeschke.jpa.PaBuffer;

import de.tucottbus.kt.csl.hardware.audio.input.AHammerfallAudioDevice;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;

public class RmeHdspAes32 extends AHammerfallAudioDevice {

  /**
   * Number of physical input audio lines
   */
  public static final int CHANNEL_COUNT = 16;

  private static RmeHdspAes32 singleton = null;

  private RmeHdspAes32() {
    super(CHANNEL_COUNT);

    Arrays.fill(volumeMixer, 1.0f);
  }

  /**
   * Singleton method to get the Hammerfall HDSP AES-32 object
   * 
   * @return Hammerfall HDSP AES-32
   */
  public static synchronized RmeHdspAes32 getInstance() {
    if (singleton == null)
      singleton = new RmeHdspAes32();
    return singleton;
  }

  @Override
  protected void callback(PaBuffer inputBuffer, PaBuffer outputBuffer,
      int numFrames) {

    if (inputBuffer.getSampleSize() == 0)
      return;

    inputBuffer.getFloatBuffer().get(inBuffer);

    // Calculate all MS (mean of squares) values
    Arrays.fill(levelsMs, 0f);
    for (int sample = 0, channel = 0; sample < AudioInputConstants.FRAME_SIZE
        * CHANNEL_COUNT; sample++, channel++) {
      levelsMs[channel] += (inBuffer[sample] * inBuffer[sample]);
      if (channel == CHANNEL_COUNT - 1)
        channel = -1;
    }

    for (int i = 0; i < CHANNEL_COUNT; i++)
      levelsMs[i] = levelsMs[i] / AudioInputConstants.FRAME_SIZE;

    // Split inBuffer into n (CHANNEL_COUNT) outBuffers
    int n = 0;
    for (int s = 0; s < AudioInputConstants.FRAME_SIZE; s++) {
      for (int c = 0; c < CHANNEL_COUNT; c++) {
        outBuffer[c][s] = inBuffer[n] * volumeMixer[c];
        n++;
      }
    }

    notifyListeners();

  }

}
