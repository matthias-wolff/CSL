package de.tucottbus.kt.csl.hardware.powerip;

public final class IpPowerSocket_012 extends AIpPowerSocket
{
  // -- Singleton implementation --

  private static volatile IpPowerSocket_012 singleton = null;

  public static synchronized IpPowerSocket_012 getInstance()
  {
    if (singleton==null)
      singleton = new IpPowerSocket_012();
    return singleton;
  }
  
  private IpPowerSocket_012()
  {
    super("141.43.71.12",  new String[]
      { "CLIENT 1", "CLIENT 2", "SCREENS", "SPOTLIGHT" } 
    );
  }

  //-- Implementation of AIpPowerSocket --
  
  @Override
  public String getName() 
  {
    return "MIC ARRAY CONSOLE";
  }

}
