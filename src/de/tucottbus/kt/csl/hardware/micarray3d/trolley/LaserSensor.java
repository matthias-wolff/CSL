package de.tucottbus.kt.csl.hardware.micarray3d.trolley;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * Low-level hardware wrapper of the ceiling trolley's laser distance sensor.
 * 
 * <p><b>Note:</b> This class is not yet in use as its parent ({@link Trolley})
 * is not yet in use.</p>
 * 
 * @author Matthias Wolff
 * @author Felix Hesse
 */
public final class LaserSensor extends AAtomicHardware implements Runnable
{
  /**
   * Hint to {@link #notifyObservers(String)} indicating that the measured 
   * distance has changed.
   */
  public static final String NOTIFY_DISTANCE = "NOTIFY_DISTANCE";
  
  /**
   * The IP address of the laser sensor's controller.
   */
  private static final String IP = "141.43.71.7";

  /**
   * The port of the laser sensor's controller.
   */
  private static final int PORT = 64321;
  
  /** 
   * The distance value hysteresis.
   */
  private static final float HYSTERESIS = 1.8f;
  
  /**
   * Minimum laser position to normalize the positon (in [cm]).
   */
  private final float LASER_MID_POS = 169.5f;
  
  /**
   * The guard thread.
   */
  private Thread guard = null;
  
  /**
   *  The run flag of the {@link #guard} thread.
   */
  private boolean runGuard = false;
 
  /**
   * The connected flag.
   * 
   * @see #isConnected()
   */
  private boolean connected = false;
  
  /**
   * The laser sensor temperature in °C.
   */
  private float temperature = 25f;
  
  /**
   * The measured distance in millimeters.
   */
  private volatile float distance = 0f; 
  
  // -- Singleton implementation --

  private static volatile LaserSensor singleton = null;

  /**
   * Returns the singleton instance.
   */
  public static synchronized LaserSensor getInstance()
  {
    if (singleton==null)
      singleton = new LaserSensor();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private LaserSensor() 
  {
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }

  // -- Implementation of Runnable --
  
  @Override
  public void run()
  {
    log("Begin of guard thread");
    
    runGuard = true;
    distance = 0f;
    while (runGuard) 
    {
      Socket socket = null;
      try
      {
        // Create client socket
        log("Connecting to "+IP+":"+PORT);
        socket = new Socket(IP,PORT);
        
        // Send request
        String msg = String.valueOf(Math.round(temperature));
        log("Sending request \""+msg+"\"");
        OutputStream os = socket.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os));
        writer.write(msg);
        writer.flush();
  
        // Read reply
        InputStream is = socket.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        msg = reader.readLine();
        log("Received reply \""+msg+"\"");
        if (!connected)
        {
          setChanged(); notifyObserversAsync(NOTIFY_CONNECTION);
        }
        connected = true;
        
        // Parse reply and store received distance
        float l = (float)Double.parseDouble(msg);
        if (Math.abs(distance-l)>=HYSTERESIS)
        {
          distance = l;
          log("New distance "+distance+" mm");
          setChanged(); notifyObserversAsync(NOTIFY_DISTANCE);
        }
        
        // Close client socket
        log("Disconnecting");
        socket.close();
        log("OK");
      }
      catch (Exception e)
      {
        logErr("Communication failure",e);
        if (socket!=null)
          try { socket.close(); } catch (Exception e2) {}
        if (connected)
        {
          setChanged(); notifyObserversAsync(NOTIFY_CONNECTION);
        }
        connected = false;
      }
      
      // Wait a little
      try { Thread.sleep(10); } catch (InterruptedException e) {}
    }

    log("End of guard thread");
  }
  
  // -- Implementation of AAtomicHardware --
  
  @Override
  public void dispose() 
  {
    if (guard!=null)
    {
      runGuard = false;
      guard.interrupt();
      try { guard.join(); } catch (Exception e) {}
    }
    super.dispose();
  }

  @Override
  public String getName() 
  {
    return "Trolley Laser Sensor";
  }

  @Override
  public boolean isConnected() 
  {
    return connected;
  }

  @Override
  public Trolley getParent() 
  {
    return Trolley.getInstance();
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // NOTE: GUI supplied by Trolley class
    return null;
  }

  // -- Getters and setters --
  
  /**
   * Returns the measured distance in centimeters.
   * 
   * @throws HardwareException
   *           if the sensor is not connected.
   */
  public float getDistance()
  throws HardwareException
  {
    if (!isConnected())
      throw new HardwareException("Laser sensor is not connected");
    return (distance/10)-LASER_MID_POS;
  }

  /**
   * Retrieves the laser sensor temperature. The temperature is used to 
   * compensate the drift of the sensor.
   * 
   * <p><b>Note:</b> The sensor does not measure the temperature itself. It must
   * be set through the {@link #setTemperature(float)} method. If no temperature
   * is set, a default value is 25 °C is assumed.</p>
   * 
   * @param temperature
   *          The temperature in °C.
   * @see #getTemperature()
   */
  public float getTemperature()
  {
    return this.temperature;
  }
  
  /**
   * Sets the laser sensor temperature. The temperature is used to compensate
   * the drift of the sensor.
   * 
   * @param temperature
   *          The temperature in °C.
   * @see #getTemperature()
   */
  public void setTemperature(float temperature)
  {
    this.temperature = temperature;
  }

  // -- Main method (only for testing) --
  
  public static void main(String[] args)
  {
    LaserSensor laserSensor = LaserSensor.getInstance();
    //laserSensor.setVerbose(1);
    
    laserSensor.printTree("");
    laserSensor.addObserver(new Observer() 
    {
      @Override
      public void update(Observable o, Object arg) 
      {
        if (NOTIFY_CONNECTION.equals(arg))
          System.out.println("Connection: "+laserSensor.isConnected());
        else if (NOTIFY_DISTANCE.equals(arg))
          try
          {
            System.out.println("Distance: "+laserSensor.getDistance()+" cm");
          }
          catch (HardwareException e)
          {
            System.out.println("Distance: [ERROR], "+e.getClass().getSimpleName());
          }
        else
          System.err.println("Unknown observer message "+arg);
      }
    });
    System.out.println("\nCommands:\n- d: read distance\n- t: read temperature\n- q: quit");
    while (true)
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try 
      { 
        String input = in.readLine();
        try
        {
          if ("d".equals(input))
          {
            System.out.print("Distance: ");
            try
            {
              System.out.println(laserSensor.getDistance()+" cm");
            }
            catch (Exception e)
            {
              System.out.println("[ERROR] "+e.getClass().getSimpleName()
                + ", \""+e.getMessage()+"\"");
            }
          }
          else if ("t".equals(input))
            System.out.println("Temperature: "+laserSensor.getTemperature()+" °C");
          else if ("q".equals(input))
            break;
          else if (!("".equals(input)))
            System.err.println("Unknown command \""+input+"\"");
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
      catch (IOException e) { break; }
    }
    
    laserSensor.dispose();
  }
  
}
