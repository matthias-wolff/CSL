package de.tucottbus.kt.csl.hardware.micarray3d;

import java.util.Arrays;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.led.LedControllerViewer;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * The main viewer microphone array hardware wrapper.
 * 
 * @author Matthas Wolff
 * @author Martin Birth
 */
public final class MicArrayViewer extends AMicArray3DPart 
{
  /**
   * CSL coordinates of the center point of the microphone array in cm
   */
  static final Point3d POS_OFFSET = new Point3d(0,220,152.5);
  
  /**
   * Positions of the microphones relative to {@link #POS_OFFSET}, in cm.
   */
  static final Point3d[] micPos = new Point3d[] 
  {
    new Point3d(-85.6, -2.5, +52.6), // 00
    new Point3d(-38.3, -1.8, +52.7), // 01
    new Point3d(-16.7, -2.0, +52.8), // 02
    new Point3d( -6.6, -1.9, +52.8), // 03
    new Point3d( -2.0, -1.6, +52.8), // 04
    new Point3d( +2.0, -1.0, +52.7), // 05
    new Point3d( +6.6, -1.2, +52.6), // 06
    new Point3d(+16.7, -1.2, +52.6), // 07
    new Point3d(+38.3, -1.5, +52.5), // 08
    new Point3d(+85.6, -1.2, +52.3), // 09
    new Point3d(-85.6, +0.1, -49.3), // 10
    new Point3d(-38.3, -1.4, -49.6), // 11
    new Point3d(-16.7, -1.1, -49.8), // 12
    new Point3d( -6.6, -0.8, -49.8), // 13
    new Point3d( -2.0, -0.8, -49.8), // 14
    new Point3d( +2.0, -0.9, -49.9), // 15
    new Point3d( +6.6, -0.6, -49.9), // 16
    new Point3d(+16.7, -0.5, -50.0), // 17
    new Point3d(+38.3, +0.0, -50.2), // 18
    new Point3d(+85.6, +0.6, -50.0), // 19
    new Point3d(-85.6, -2.4, -18.5), // 20
    new Point3d(-85.6, -2.5,  -5.6), // 21
    new Point3d(-85.6, -2.5,  -0.1), // 22
    new Point3d(-85.6, -2.7,  +3.6), // 23
    new Point3d(-85.6, -2.7,  +9.1), // 24
    new Point3d(-85.6, -2.7, +22.0), // 25
    new Point3d(+85.6, +0.5, -19.3), // 26
    new Point3d(+85.6, +0.8,  -6.4), // 27
    new Point3d(+85.6, +0.9,  -1.0), // 28
    new Point3d(+85.6, +0.6,  +2.8), // 29
    new Point3d(+85.6, +0.6,  +8.4), // 30
    new Point3d(+85.6, +0.3, +21.2)  // 31
  };
  
  // -- Singleton implementation --

  private static volatile MicArrayViewer singleton = null;

  /**
   * Returns the singleton instance.
   */
  public static synchronized MicArrayViewer getInstance()
  {
    if (singleton==null)
      singleton = new MicArrayViewer();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private MicArrayViewer() 
  {
    Arrays.fill(activeMics, true);
  }

  // -- Implementation of AHardware --

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() 
  {
    return "MAIN VIEWER";
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

  // -- Implementation of AMicArray2D --
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int getMinMicId()
  {
    return 0;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public int getMaxMicId()
  {
    return 31;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public LedControllerViewer getLedController() 
  {
    return LedControllerViewer.getInstance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Point3d getPosition()
  {
    return POS_OFFSET;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public Point3d getMicPosition(int micId) throws IllegalArgumentException 
  {
    checkMicId(micId);
    return micPos[micId-getMinMicId()];
  }
  
  /**
   * {@inheritDoc}
   * @throws HardwareException 
   */
  @Override
  public void setMicArrayPreset(int presetId) throws HardwareException {
    boolean[] selectionA1 = new boolean[32];
    Arrays.fill(selectionA1, Boolean.FALSE);

    switch (presetId) {
      case 0:
        selectionA1[0] = Boolean.TRUE;
        selectionA1[4] = Boolean.TRUE;
        selectionA1[9] = Boolean.TRUE;
        selectionA1[10] = Boolean.TRUE;
        selectionA1[14] = Boolean.TRUE;
        selectionA1[19] = Boolean.TRUE;
        selectionA1[23] = Boolean.TRUE;
        selectionA1[29] = Boolean.TRUE;
        break;
      case 1:
        selectionA1[0] = Boolean.TRUE;
        selectionA1[4] = Boolean.TRUE;
        selectionA1[9] = Boolean.TRUE;
        selectionA1[10] = Boolean.TRUE;
        selectionA1[14] = Boolean.TRUE;
        selectionA1[19] = Boolean.TRUE;
        selectionA1[23] = Boolean.TRUE;
        selectionA1[29] = Boolean.TRUE;
        selectionA1[2] = Boolean.TRUE;
        selectionA1[7] = Boolean.TRUE;
        selectionA1[31] = Boolean.TRUE;
        selectionA1[26] = Boolean.TRUE;
        selectionA1[20] = Boolean.TRUE;
        selectionA1[25] = Boolean.TRUE;
        selectionA1[12] = Boolean.TRUE;
        selectionA1[17] = Boolean.TRUE;
        break;
      case 2:
        Arrays.fill(selectionA1, Boolean.TRUE);
        selectionA1[3] = Boolean.FALSE;
        selectionA1[6] = Boolean.FALSE;
        selectionA1[30] = Boolean.FALSE;
        selectionA1[27] = Boolean.FALSE;
        selectionA1[16] = Boolean.FALSE;
        selectionA1[13] = Boolean.FALSE;
        selectionA1[21] = Boolean.FALSE;
        selectionA1[24] = Boolean.FALSE;
        break;
      case 3:
        Arrays.fill(selectionA1, Boolean.TRUE);
        break;
      default:
        break;
    }
    setActiveSelection(selectionA1);
  }
}
