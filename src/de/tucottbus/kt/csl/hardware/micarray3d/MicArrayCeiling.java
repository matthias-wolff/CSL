package de.tucottbus.kt.csl.hardware.micarray3d;

import java.util.Arrays;
import java.util.Collection;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.led.LedControllerCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.trolley.Trolley;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * <b>Incubating:</b> The ceiling microphone array composite hardware wrapper. 
 * 
 * @author Matthas Wolff
 * @author Martin Birth
 */
public final class MicArrayCeiling extends AMicArray3DPart 
{ 
  /**
   * angle of inclination (in degrees).
   */
  final float ANGLE = -8.47f;
  
  /**
   * CSL coordinates of the center point of the microphone array (in cm).
   */
  static final Point3d POS_OFFSET = new Point3d(+5.3, 0, +235.0);

  /**
   * Positions of the microphones relative to {@link #POS_OFFSET}, in cm.
   */
  static final Point3d[] micPos = new Point3d[] 
  {
    new Point3d( +2.3,   1.9, 0.0), // 32
    new Point3d(+11.8,  -2.7, 0.0), // 33
    new Point3d( +9.7, -20.0, 0.0), // 34
    new Point3d(-14.6, -30.3, 0.0), // 35
    new Point3d(-45.1, -10.3, 0.0), // 36
    new Point3d(-47.4,  37.8, 0.0), // 37
    new Point3d( +0.0,  76.7, 0.0), // 38
    new Point3d(+74.3,  59.2, 0.0), // 39
    new Point3d( +1.9,  -2.3, 0.0), // 40
    new Point3d( -2.7, -11.8, 0.0), // 41
    new Point3d(-20.0,  -9.7, 0.0), // 42
    new Point3d(-30.3,  14.6, 0.0), // 43
    new Point3d(-10.3,  45.1, 0.0), // 44
    new Point3d(+37.8,  47.4, 0.0), // 45
    new Point3d(+76.7,   0.0, 0.0), // 46
    new Point3d(+59.2, -74.3, 0.0), // 47
    new Point3d( -2.3,  -1.9, 0.0), // 48
    new Point3d(-11.8,   2.7, 0.0), // 49
    new Point3d( -9.7,  20.0, 0.0), // 50
    new Point3d(+14.6,  30.3, 0.0), // 51
    new Point3d(+45.1,  10.3, 0.0), // 52
    new Point3d(+47.4, -37.8, 0.0), // 53
    new Point3d( +0.0, -76.7, 0.0), // 54
    new Point3d(-74.3, -59.2, 0.0), // 55
    new Point3d( -1.9,   2.3, 0.0), // 56
    new Point3d( +2.7,  11.8, 0.0), // 57
    new Point3d(+20.0,   9.7, 0.0), // 58
    new Point3d(+30.3, -14.6, 0.0), // 59
    new Point3d(+10.3, -45.1, 0.0), // 60
    new Point3d(-37.8, -47.4, 0.0), // 61
    new Point3d(-76.7,   0.0, 0.0), // 62
    new Point3d(-59.2, +74.3, 0.0)  // 63
  };
  
  // -- Singleton implementation --

  private static volatile MicArrayCeiling singleton = null;
  
  // -- non-static fields --
  
  private Trolley trolley;
  
  private final double rad = Math.toRadians(ANGLE);
  private final double sin = Math.sin(rad);
  private final double cos = Math.cos(rad);
  
  private double[] deltaPosition;
  
  /**
   * Returns the singleton instance. 
   */
  public static synchronized MicArrayCeiling getInstance()
  {
    if (singleton==null)
      singleton = new MicArrayCeiling();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private MicArrayCeiling() 
  {
    trolley = Trolley.getInstance();
    Arrays.fill(activeMics, true);
    setDeltaPosition();
  }
  
  /**
   * Rotation by angle {@link #ANGLE} of the microphone array
   * 
   * @param point - Point3d
   * @return Point3d
   */
  private Point3d doRotation(Point3d point){
    Point3d out = new Point3d();
    out.setX(point.x);
    out.setY(cos * point.y - sin * point.z);
    out.setZ(sin * point.y + cos * point.z);
    return out;
  }

  // -- Implementation of AHardware --

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() 
  {
    return "CEILING";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }

  // -- Implementation of ACompositeHardware --

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<AHardware> getChildren() 
  {
    Collection<AHardware> children = super.getChildren();
    trolley = Trolley.getInstance();
    children.add(trolley);
    return children;
  }
  
  // -- Implementation of AMicArray2D --
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int getMinMicId()
  {
    return 32;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxMicId()
  {
    return 63;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public LedControllerCeiling getLedController() 
  {
    return LedControllerCeiling.getInstance();
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public Point3d getPosition()
  {
    Point3d p = POS_OFFSET;
    try {
      p.y = trolley.getCeilingPosition();
    } catch (HardwareException e) {
      logErr(e.getMessage(), e);
    }
    return p;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public Point3d getMicPosition(int micId) 
  throws IllegalArgumentException 
  {
    checkMicId(micId);
    return doRotation(micPos[micId-getMinMicId()]);
  }
  
  /**
   * Get the relative microphone position without the rotation of {@link #ANGLE}
   * @param micId
   * @return Point3d
   */
  public Point3d getRelativeMicPosition(int micId){
    checkMicId(micId);
    return micPos[micId-getMinMicId()];
  }
  
  private void setDeltaPosition(){
    deltaPosition = new double[micPos.length];
    
    for (int i = 0; i < micPos.length; i++) {
      Point3d pOriginal = doRotation(micPos[i]);
      pOriginal.add(POS_OFFSET);
      pOriginal.scale(1/100.0);
      
      Point3d pNew = new Point3d(micPos[i]);
      pNew.add(POS_OFFSET);
      pNew.scale(1/100.0);
      
      deltaPosition[i]=pOriginal.distance(pNew);
      if(pOriginal.y>0)
        deltaPosition[i]*=-1;
        
    }
  }
  
  /**
   * 
   * @param micId
   * @return
   */
  public double getDeltaPosition(int micId){
    checkMicId(micId);
    return deltaPosition[micId-getMinMicId()];
  }
  
  /**
   * Get the relative angle of the array inclination.
   * @return
   */
  public float getInclinationAngle(){
    return ANGLE;
  }

  // -- Getters and setters --
  
  public void setPosition(Point3d position)
  throws IllegalArgumentException, HardwareException
  {
    // TODO: Implement MicArrayCeiling.setPosition(Point3d)!
    trolley.setCeilingPosition(position.y);
  }
  
  // -- Operation --
  
  public void cancelPositioning()
  throws HardwareException
  {
    trolley.cancel();
  }

  /**
   * {@inheritDoc}
   * @throws HardwareException 
   */
  @Override
  public void setMicArrayPreset(int presetId) throws HardwareException {
    boolean[] selectionA2 = new boolean[32];
    Arrays.fill(selectionA2, Boolean.FALSE);
      switch (presetId)
      {
      case 0:
        selectionA2[0] = Boolean.TRUE;
        selectionA2[7] = Boolean.TRUE;
        selectionA2[8] = Boolean.TRUE;
        selectionA2[15] = Boolean.TRUE;
        selectionA2[16] = Boolean.TRUE;
        selectionA2[23] = Boolean.TRUE;
        selectionA2[24] = Boolean.TRUE;
        selectionA2[31] = Boolean.TRUE;
        break;
      case 1:
        selectionA2[0] = Boolean.TRUE;
        selectionA2[7] = Boolean.TRUE;
        selectionA2[8] = Boolean.TRUE;
        selectionA2[15] = Boolean.TRUE;
        selectionA2[16] = Boolean.TRUE;
        selectionA2[23] = Boolean.TRUE;
        selectionA2[24] = Boolean.TRUE;
        selectionA2[31] = Boolean.TRUE;
        selectionA2[5] = Boolean.TRUE;
        selectionA2[12] = Boolean.TRUE;
        selectionA2[4] = Boolean.TRUE;
        selectionA2[29] = Boolean.TRUE;
        selectionA2[28] = Boolean.TRUE;
        selectionA2[21] = Boolean.TRUE;
        selectionA2[20] = Boolean.TRUE;
        selectionA2[13] = Boolean.TRUE;
        break;
      case 2:
        selectionA2[0] = Boolean.TRUE;
        selectionA2[1] = Boolean.TRUE;
        selectionA2[3] = Boolean.TRUE;
        selectionA2[5] = Boolean.TRUE;
        selectionA2[7] = Boolean.TRUE;
        selectionA2[8] = Boolean.TRUE;
        selectionA2[9] = Boolean.TRUE;
        selectionA2[11] = Boolean.TRUE;
        selectionA2[13] = Boolean.TRUE;
        selectionA2[15] = Boolean.TRUE;
        selectionA2[16] = Boolean.TRUE;
        selectionA2[17] = Boolean.TRUE;
        selectionA2[19] = Boolean.TRUE;
        selectionA2[21] = Boolean.TRUE;
        selectionA2[23] = Boolean.TRUE;
        selectionA2[24] = Boolean.TRUE;
        selectionA2[25] = Boolean.TRUE;
        selectionA2[27] = Boolean.TRUE;
        selectionA2[29] = Boolean.TRUE;
        selectionA2[31] = Boolean.TRUE;
        break;
      case 3:
        Arrays.fill(selectionA2, Boolean.TRUE);
        break;
      default:
        break;
    }
    setActiveSelection(selectionA2);
  }
    
}
