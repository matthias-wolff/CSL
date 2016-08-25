package de.tucottbus.kt.csl.speech;

/**
 * 
 * @author Peter Gessler
 *
 * @task Interface to get all possible FVR permutations.
 */
public interface IArticulateSpeech {

  /**
   * Return all possible FVR permutations based on
   * algorithm in {@link SpeechArticulator#getArticulate}.
   * 
   * @param feedback
   * @return
   */
  public ArticulateNodeList articulateSpeech(Fvr feedback);
}
