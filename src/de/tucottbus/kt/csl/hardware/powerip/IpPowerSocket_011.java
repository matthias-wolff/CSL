package de.tucottbus.kt.csl.hardware.powerip;

public final class IpPowerSocket_011 extends AIpPowerSocket
{
  // -- Singleton implementation --

  private static volatile IpPowerSocket_011 singleton = null;

  public static synchronized IpPowerSocket_011 getInstance()
  {
    if (singleton==null)
      singleton = new IpPowerSocket_011();
    return singleton;
  }

  private IpPowerSocket_011()
  {
    super("141.43.71.11", new String[]
      { "IPC", "PERIPHALS", "AUDIO HARDWARE", "TROLLEY" }
    );
  }

  //-- Implementation of AIpPowerSocket --
  
  @Override
  public String getName() 
  {
    return "MAIN CONSOLE";
  }

}
