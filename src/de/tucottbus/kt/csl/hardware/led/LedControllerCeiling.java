package de.tucottbus.kt.csl.hardware.led;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;

public final class LedControllerCeiling extends ALedController 
{
  // -- Singleton implementation --

  private static volatile LedControllerCeiling singleton = null;

  public static synchronized LedControllerCeiling getInstance()
  {
    if (singleton==null)
      singleton = new LedControllerCeiling();
    return singleton;
  }

  private LedControllerCeiling()
  {
    super();
  }

  // -- Implementation of ALedController --

  @Override
  protected String getIP()
  {
    return "141.43.71.64";
  }
  
  @Override
  public String getName() 
  {
    return "CEILING";
  }
  
  @Override
  public AHardware getParent() 
  {
    return MicArrayCeiling.getInstance();
  }

}
