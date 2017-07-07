package de.tucottbus.kt.csl.hardware.powerip;

public final class IpPowerSocket_010 extends AIpPowerSocket
{  
  // -- Singleton implementation --

  private static volatile IpPowerSocket_010 singleton = null;

  public synchronized static IpPowerSocket_010 getInstance()
  {
    if (singleton==null)
      singleton = new IpPowerSocket_010();
    return singleton;
  }

  private IpPowerSocket_010()
  {
    super("141.43.71.10", new String[]
      { "MAIN VIEWER", "ILLUMINAITON", "SPEAKERS", "KINECT" }
    );
  }

  //-- Implementation of AIpPowerSocket --
  
  @Override
  public String getName() 
  {
    return "MAIN VIEWER";
  }

}
