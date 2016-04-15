package de.tucottbus.kt.csl.hardware.kinect.devices;

import de.tucottbus.kt.csl.hardware.kinect.room.Object3D;
import edu.ufl.digitalworlds.j4k.J4KSDK;

/**
 * 
 * This class is representing the kinect v2. In the moment there is only one 
 * v2 in the cognitive system lab, therefore the deviceID is zero. The second 
 * sensor has the one as deviceID etc.
 * 
 * @author Thomas Jung
 *
 */
public final class KinectSensor2_000 extends AKinectSensor
{
  private static volatile KinectSensor2_000 singleton = null;
  
  public synchronized static KinectSensor2_000 getInstance()
  {
    if(singleton == null) singleton = new KinectSensor2_000();
    return singleton;
  }
  
  private KinectSensor2_000()
  {
    super(J4KSDK.MICROSOFT_KINECT_2, 0, Object3D.SENSOR2_000);
  }
  
  /**
   * this method checks if the kinect can send data or is sending data
   * @return the running status
   */
  public synchronized boolean isRunning()
  {
    if(!isConnected()) return false;
    kinect.setRunning(false);
    if(!(isColorStreamOpen() || isDepthStreamOpen() || isInfraredStreamOpen()))
      super.startInfraredStream();
    try { Thread.sleep(1000); } catch (InterruptedException e) { }
    return kinect.isRunning();
  }
}
