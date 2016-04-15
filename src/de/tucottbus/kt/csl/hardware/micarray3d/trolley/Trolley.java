package de.tucottbus.kt.csl.hardware.micarray3d.trolley;

import java.util.ArrayList;
import java.util.Collection;

import de.tucottbus.kt.csl.hardware.ACompositeHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * Hardware wrapper of the trolley carrying the {@linkplain MicArrayCeiling
 * ceiling microphone array part}.
 * 
 * <p><b style="color:red">DRAFT:</b> This class is to replace {@link 
 * DeprecatedPositioningDevice}.</p>
 * 
 * @author Matthias Wolff
 * @author Martin Birth
 */
public final class Trolley extends ACompositeHardware 
{
  private Motor       motor;
  private LaserSensor laserSensor;
  
  // -- Singleton implementation --

  private static volatile Trolley singleton = null;

  /**
   * Returns the singleton instance.
   */
  public static synchronized Trolley getInstance()
  {
    if (singleton==null)
      singleton = new Trolley();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private Trolley() 
  {
    motor       = Motor.getInstance();
    laserSensor = LaserSensor.getInstance();
  }
  
  // -- Implementation of ACompositeHardware --
  
  @Override
  public String getName() 
  {
    return "Trolley";
  }

  @Override
  public MicArrayCeiling getParent() 
  {
    return MicArrayCeiling.getInstance();
  }
  
  @Override
  public Collection<AHardware> getChildren() 
  {
    ArrayList<AHardware> children = new ArrayList<AHardware>();
    motor = Motor.getInstance();
    laserSensor = LaserSensor.getInstance();
    children.add(motor);
    children.add(laserSensor);
    return children;
  }
  
  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Get the current ceiling position of the trolley.
   * @return double
   * @throws HardwareException
   */
  public double getCeilingPosition() throws HardwareException {
    double d = laserSensor.getDistance();
    return d;
  }

  /**
   * Set the ceiling position 
   * @param d
   */
  public void setCeilingPosition(double d) {
   motor.setMotorPosition(d);
  }

  public void cancel() {
    motor.cancel();
  }
  
}
