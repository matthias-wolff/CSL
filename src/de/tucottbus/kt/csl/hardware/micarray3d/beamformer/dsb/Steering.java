package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb;

import javax.vecmath.Point3d;

/**
 * Static class for calculating steering and delay vectors.
 * 
 * These static class is 
 * 
 * @author Martin Birth
 */
public final class Steering {

  /**
   * Speed of sound (in m/s).
   */
  private final static double SPEED_OF_SOUND = 343.2;
  
  /**
   * Singleton class.
   */
  private Steering() {}

  /**
   * This method calculates the steering vectors based on all acoustic 
   * spatial delays. The longest delay is retrieved and adjusted every other.
   * As result all sound waves are so long restrained at the
   * individual microphones until the sound has arrived at 
   * the last microphone.
   * 
   * @param delayVector - The delay vectors are returned by {@link #getDelays(Point3d[] micPos, Point3d point)}.
   * @return float[]
   * @see #getDelays(Point3d[], Point3d)
   */
  public static float[] getSteeringVectorFromDelays(float[] delayVector) {
    float dMax = findMaxDelay(delayVector);

    float[] steeringVector = new float[delayVector.length];
    for (int i = 0; i < delayVector.length; i++){
      steeringVector[i] = dMax - delayVector[i];
    }

    return steeringVector;
  }
  
  /**
   * Finding maximal delay in float[] array of N channel delays.
   * @param delayVector - float[] array
   * @return float
   */
  private static float findMaxDelay(float[] delayVector){
    float dMax = 0;
    for (int i = 0; i < delayVector.length; i++) {
      if (dMax < delayVector[i]){
        dMax = delayVector[i];
      }
    }
    return dMax;
  }

  /**
   * This method calculates all spatial acoustic delay for a array 
   * of microphone positions.
   * @param micPos - Specific microphone position.
   * @param point - Specific room point.
   * @return float[] - with the delays.
   * @see #getDelayFromMicToPoint(Point3d micPos, Point3d point)    
   */
  public static float[] getDelays(Point3d[] micPos, Point3d point) {
    float[] delayVector = new float[micPos.length];

    for (int i = 0; i < micPos.length; i++) {
      delayVector[i] = (float) getDelayFromMicToPoint(micPos[i], point);
    }

    return delayVector;
  }

  /**
   * This method calculates by triangulation the distance between the origin of the
   * room coordinate system and the specific position of a single microphone. 
   * This distance corresponds to the spatial acoustic delay that arises at
   * this track.
   * @param micPos - Specific microphone position.
   * @param point - Specific room point.
   * @return double - delay.
   * @see #getDelays(Point3d[], Point3d)      
   */
  public static double getDelayFromMicToPoint(Point3d micPos, Point3d point) {
    return (micPos.distance(point)/100)/SPEED_OF_SOUND;
  }
  
}
