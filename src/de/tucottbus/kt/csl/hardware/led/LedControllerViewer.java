package de.tucottbus.kt.csl.hardware.led;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;

public final class LedControllerViewer extends ALedController 
{
  // -- Singleton implementation --

  private static volatile LedControllerViewer singleton = null;

  public static synchronized LedControllerViewer getInstance()
  {
    if (singleton==null)
      singleton = new LedControllerViewer();
    return singleton;
  }

  private LedControllerViewer()
  {
    super();
  }

  // -- Implementation of ALedController --

  @Override
  protected String getIP()
  {
    return "141.43.71.63";
  }
  
  @Override
  public String getName() 
  {
    return "MAIN VIEWER";
  }
  
  @Override
  public AHardware getParent() 
  {
    return MicArrayViewer.getInstance();
  }

}
