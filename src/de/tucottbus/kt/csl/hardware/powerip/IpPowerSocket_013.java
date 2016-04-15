package de.tucottbus.kt.csl.hardware.powerip;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public final class IpPowerSocket_013 extends AIpPowerSocket
{
  // -- Singleton implementation --

  private static volatile IpPowerSocket_013 singleton = null;

  public static synchronized IpPowerSocket_013 getInstance()
  {
    if (singleton==null)
      singleton = new IpPowerSocket_013();
    return singleton;
  } 

  private IpPowerSocket_013()
  {
    super("141.43.71.13", null);
  }

  // -- MAIN METHOD --
  
  /**
   * Main method; just for debugging.
   * 
   * @param args
   *          -- not used --
   */
  public static void main(String[] args)
  {
    final AIpPowerSocket ips = new IpPowerSocket_013();
    ips.setVerbose(1);

    final Random random = new Random();
    Timer timer = new Timer(true);
    timer.schedule(new TimerTask()
    {
      @Override
      public void run()
      {
        int port = random.nextInt(4)+1;
        boolean on = random.nextBoolean();
        try
        {
          ips.setPortOn(port, on);
        } 
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    }, 2500, 2500);
    System.out.println("hit a key...");
    try
    {
      System.in.read();
    } 
    catch (IOException e1)
    {
    }
    ips.dispose();
  }

}
