package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa;

import javax.vecmath.Point3d;

import Jama.Matrix;

/**
 * The the plausibility of the estimated positions according to the room dimensions.
 * @author Martin Birth
 *
 */
public class PlausibilityChecker {
  
  /**
   * To normalize all Point3d from [cm] to [m]
   */
  private final static double DNORM = 100.0;

  /**
   * Maximum value on the x-axis [m]
   */
  public static final double MAX_X = 2.20;
  
  /**
   * Maximum value on the y-axis [m]
   */
  public static final double MAX_Y = 2.20;
  
  /**
   * Maximum value on the z-axis [m]
   */
  public static final double ROOM_HEIGTH = 2.30;

  /**
   * Get the target of array 1 limited to the room dimensions
   * @param target, Matrix
   * @return Point3d
   * @see Localizers#getTargetVektor(Point3d[], int[], int, de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator.MicArray)
   */
  public Point3d checkArray1(Matrix target) {
    double x = target.get(0, 0);
    x = (x < -MAX_X) ? -MAX_X : ((x > MAX_X) ? MAX_X : x);
    double z = target.get(2, 0);
    z = (z<0) ? 0 : ((z > ROOM_HEIGTH) ? ROOM_HEIGTH : z);
    return new Point3d(x*DNORM,MAX_Y,z*DNORM);
  }

  /**
   * Get the target of array 2 limited to the room dimensions
   * @param target, Matrix
   * @return Point3d
   * @see Localizers#getTargetVektor(Point3d[], int[], int, de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator.MicArray)
   */
  public Point3d checkArray2(Matrix target) {
    double x = target.get(0, 0);
    x = (x < -MAX_X) ? -MAX_X : ((x > MAX_X) ? MAX_X : x);
    double y = target.get(1, 0);
    y = (y < -MAX_Y) ? -MAX_Y : ((y > MAX_Y) ? MAX_Y : y);
    return new Point3d(x*DNORM,y*DNORM,0);
  }

}
