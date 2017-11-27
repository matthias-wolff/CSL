package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb;

import java.util.Arrays;

import javax.vecmath.Point3d;

/**
 * Static methods for computing steering, delay, and gain vectors.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Compute real gain factors in <code>getGains(...)</code>.
 *     </li>
 * </ul>
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public final class Steering 
{

  /**
   * The speed of sound in meters per second.
   */
  private static final double SPEED_OF_SOUND = 343.2;
  
  /**
   * Calculates a steering vector based on an array of acoustic spatial delays.
   * The longest delay is retrieved and adjusted every other. As result all
   * sound waves are so long restrained at the individual microphones until the
   * sound has arrived at the last microphone.
   * 
   * @param delays
   *          The delay array as returned by
   *          {@link #getDelays(Point3d[] micPos, Point3d point)}.
   * @return The steering vector
   * @see #getDelays(Point3d[], Point3d)
   */
  public static float[] getSteeringVectorFromDelays(float[] delays) 
  {
    float dMax = getMaxDelay(delays);

    float[] steeringVector = new float[delays.length];
    for (int i = 0; i < delays.length; i++)
      steeringVector[i] = dMax - delays[i];

    return steeringVector;
  }
  
  /**
   * Returns the maximal value from an array of delays.
   * 
   * @param delays
   *          The delay array.
   */
  private static float getMaxDelay(float[] delays)
  {
    float dMax = 0;

    for (int i = 0; i < delays.length; i++) 
      if (dMax < delays[i])
        dMax = delays[i];

    return dMax;
  }

  /**
   * Calculates all spatial acoustic delays for an array of microphone
   * positions.
   * 
   * @param micPos
   *          Array of microphone positions.
   * @param point
   *          Steering target.
   * @return An array containing the delays, in seconds, for the microphone
   *         positions.
   * @see #getDelayFromMicToPoint(Point3d micPos, Point3d point)
   */
  public static float[] getDelays(Point3d[] micPos, Point3d point) 
  {
    float[] delays = new float[micPos.length];

    for (int i=0; i<micPos.length; i++)
      delays[i] = (float)getDelayFromMicToPoint(micPos[i], point);

    return delays;
  }

  /**
   * Calculates by triangulation the distance between the origin of the room
   * coordinate system and the specific position of a single microphone. This
   * distance corresponds to the spatial acoustic delay that arises at this
   * track.
   * 
   * @param micPos
   *          The microphone position.
   * @param point
   *          The steering target.
   * @return double The delay in seconds.
   */
  public static double getDelayFromMicToPoint(Point3d micPos, Point3d point) 
  {
    return (micPos.distance(point)/100)/SPEED_OF_SOUND;
  }

  /**
   * Calculates microphone gain factors for an array of microphone positions.
   * 
   * @param micPos
   *          Array of microphone positions.
   * @param point
   *          Steering target.
   * @return An array containing the gains for the microphone positions.
   */
  public static float[] getGains(Point3d[] micPos, Point3d point)
  {
    float[] gains = new float[micPos.length];
    Arrays.fill(gains,1f);
    return gains;
  }
  
}

// EOF
