package de.tucottbus.kt.csl.hardware.kinect.devices;

import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;
import edu.ufl.digitalworlds.j4k.J4KSDK;

/**
 * This class is representing the kinect v1. In the moment there is only one 
 * v1 in the cognitive system lab, therefore the deviceID is zero. The second 
 * sensor has the one as deviceID etc. 
 * <p> The kinect v1 can only send a color or an infrared stream at the same time, 
 * therefore is it necessary to toggle the streams.</p>
 * 
 * @author Thomas Jung
 *
 */
public final class KinectSensor1_000 extends AKinectSensor
{
  private static volatile KinectSensor1_000 singleton = null;
  
  public synchronized static KinectSensor1_000 getInstance()
  {
    if(singleton == null) singleton = new KinectSensor1_000();
    return singleton;
  }
  
  private KinectSensor1_000()
  {
    super(J4KSDK.MICROSOFT_KINECT_1, 0, Object3D.SENSOR1_000);
  }
  
  /**
   * starts the infrared stream and stops the color stream
   */
  @Override
  public void startInfraredStream()
  {
    stopColorStream();
    super.startInfraredStream();
  }
  
  /**
   * starts the color stream and stoops the infrared stream
   */
  @Override
  public void startColorStream()
  {
    stopInfraredStream();
    super.startColorStream();
  }
  
}