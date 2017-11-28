package de.tucottbus.kt.csl.hardware.micarray3d.trolley;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import gnu.io.NRSerialPort;

/**
 * Hardware wrapper of {@link Trolley}'s motor.
 * 
 * <h3>Remark:</h3>
 * <ul>
 *   <li>FIXME: Connection tracking does not work.
 *     </li>
 * </ul>
 * 
 * @author Matthias Wolff
 * @author Martin Birth
 */
public final class Motor extends AAtomicHardware implements Runnable
{
  private final String PORT = "COM1";
  private final int BAUDRATE = 19200;
  
  /**
   * Motor is calculating in mm and laser in cm
   */
  private final int LASER_MOTOR_FACTOR = 10;
  
  private final double MAX_DISTANCE_TIME = 16; // [s]
  private final double MAX_DISTANCE = 2800; // [steps]
  
  private final double MIN_LASER_POS = -1395; // [mm]
  private final double MAX_LASER_POS = 1055;  // [mm]
  
  private final String MOTOR_PARAMETER = "tp: 1 100 100 0 0 200";
  private final String ENDSWITCH_PARAMETER = "pol: 1 111 000";
  private final String NULLING_CORD_SYSTEM = "null 1";
  
  private final int LOWPASS_FILTER_DELAY = 2500; // [ms]
  
  private DataInputStream pcInputs;
  private DataOutputStream pcOutputs;
  
  private NRSerialPort serial;
  
  private Thread guard = null;
  private boolean runGuard = Boolean.FALSE;
  
  private final Queue<Double> msgQueue;
  
  // -- Singleton implementation --

  private static volatile Motor singleton = null;
  
  /**
   * Returns the singleton instance.
   */
  public static synchronized Motor getInstance()
  {
    if (singleton==null)
      singleton = new Motor();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private Motor() 
  {
    msgQueue = new ConcurrentLinkedQueue<Double>();
    
    // Start the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }

  @Override
  public void run() {
    log("Begin of guard thread");
    runGuard = true;
    final int sleepMillis = 10;
    int ctr = 0;
    
    while (runGuard)
      try 
      {
        // - Initialize connection
        serial = new NRSerialPort(PORT, BAUDRATE); // FIXME: should be an instance class, a second initialization throws errors
        serial.connect();
        pcInputs = new DataInputStream(serial.getInputStream());
        pcOutputs = new DataOutputStream(serial.getOutputStream());
        
        log("Connected to "+PORT+":"+BAUDRATE);
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);

        // - Run connection
        while (runGuard) 
        {
          try { Thread.sleep(sleepMillis); } catch (InterruptedException e) {}
          if (!runGuard)
            break;
          
          if (isConnected() && !msgQueue.isEmpty()){
            runMotor();
          }
          
          ctr+=sleepMillis;
          if (ctr>=1000)
          {
            setChanged();
            notifyObservers(NOTIFY_STATE);
            ctr = 0;
          }
        }

        // - End connection
        
        pcInputs.close();
        pcOutputs.close();
        if(serial.isConnected())
          serial.disconnect();
        log("Disconnected");
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
      } 
      catch (Exception e) 
      {
        logErr(e.getMessage(),e);
        if(serial.isConnected())
          serial.disconnect();
        serial = null;
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try { Thread.sleep(2500); } catch (InterruptedException e1) {}
      }

    log("End of guard thread");
  }
  
  // -- Implementation of AAtomicHardware --
  
  @Override
  public void dispose() 
  {
    cancel();
    
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
    return "Trolley Motor";
  }

  @Override
  public boolean isConnected() 
  {
    if(serial==null)
      return false;
    return serial.isConnected();
  }

  @Override
  public Trolley getParent() 
  {
    return Trolley.getInstance();
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  /**
   * Stops the trolley immediately.
   * 
   * @throws IllegalStateException
   *           If the instance is disposed or the hardware is not connected.
   * @see #setPosition(float)
   */
  public void cancel() throws IllegalStateException {
    msgQueue.clear();
    if (!isConnected())
      return;
    try {
      pcOutputs.write(4);
      pcOutputs.flush();
    } catch (IOException e) {
      logErr("Trolley (-cancel()-) not controllable, reason: " + e.getMessage(),e);
    }
  }
  
  /**
   * Returns the current y-position of the trolley in the speech lab's room
   * coordinate system according to the motor position reading. The returned
   * value is measured in centimeters.
   * 
   * @throws IllegalStateException
   *           If the instance is disposed or the hardware is not connected.
   * @see #getLaserPosition()
   * @see #getMotorReading()
   */
  @SuppressWarnings("unused")
  // TODO: if someone need this method, he had to implement the pcInputs
  private float getMotorPosition() throws IllegalStateException {
    if (!isConnected()) {
      return Float.MIN_VALUE;
    }
    return motorCoordToPos(0);
  }
  
  /**
   * Calculates the position (-200cm to 200cm) from the steps of the motor (0 to
   * 3000)
   * 
   * @param coord
   *          [0,3000]
   * @return position in centimeters, [-200cm, 200cm]
   */
  private static float motorCoordToPos(int coord) {
    return (float) (coord / -30. * 2.48);
  }
  
  /**
   * Sends the command to the motor.
   * 
   * @param command
   *          The command which has to be send to the motor
   */
  private void outPutMotor(String command) {
    if(!isConnected()){
      return;
    }
    
    try {
      for (int i = 0; i < command.length(); i++) {
        pcOutputs.write(command.charAt(i));
        pcOutputs.flush();
      }
      pcOutputs.write(13); // end of line
      pcOutputs.flush();
    } catch (IOException e) {
      logErr("Method outPutMotor() not controllable, reason: "
          + e.getMessage(),e);
    }
  }
  
  /**
   * Adding a new position in the message queue hang which the motor is driving to.
   * @param position
   */
  public void setMotorPosition(double position){
    cancel();
    msgQueue.add(position*LASER_MOTOR_FACTOR);
  }
  
  /**
   * Check if new position was set.
   * @param oldDistanceTarget double
   * @return boolean
   */
  private boolean isPositionChanged(double oldDistanceTarget ){
    if(msgQueue.isEmpty())
      return false;
    
    double newDisTarget = msgQueue.peek();
    
    newDisTarget = (newDisTarget<MIN_LASER_POS) ? MIN_LASER_POS : ((newDisTarget>MAX_LASER_POS) ? MAX_LASER_POS : newDisTarget);
    
    if(newDisTarget==oldDistanceTarget){
      return false;
    } else {
      return true;
    }
  }
  
  /**
   * Drive motor to destination
   * @param distanceTarget
   */
  private void runMotor(){
    final int MAX_POSSIBLE_TRYS = 4;
    final int MIN_DISTANZ = 3;
    final double CONVERSIONS_FACTOR = 1.1976; // [steps/mm]

    double timefactor = MAX_DISTANCE_TIME / MAX_DISTANCE;

    double distanceTarget = msgQueue.poll();
    distanceTarget = (distanceTarget<MIN_LASER_POS) ? MIN_LASER_POS : ((distanceTarget>MAX_LASER_POS) ? MAX_LASER_POS : distanceTarget);
    
    outPutMotor(MOTOR_PARAMETER);
    outPutMotor(ENDSWITCH_PARAMETER);
    outPutMotor(NULLING_CORD_SYSTEM);

    for (int i = 0; i < MAX_POSSIBLE_TRYS; i++) {
      double distanceActual=-1500;
      while(distanceActual<=-1450){
        try {
          distanceActual = getParent().getCeilingPosition()*LASER_MOTOR_FACTOR;
        } catch (HardwareException e) {
          logErr(e.getMessage(), e);
        } 
      }
      
      // exit if target is reached
      if ((Math.abs(distanceActual - distanceTarget)) < MIN_DISTANZ) {
        break;
      }

      // move the new distance
      double distanceDelta = distanceActual - distanceTarget;
      distanceDelta = distanceDelta * CONVERSIONS_FACTOR;

      outPutMotor("pr1 " + (int) Math.round(distanceDelta));

      int time = (int) Math.round(timefactor * Math.abs(distanceDelta) * 1000)+LOWPASS_FILTER_DELAY;
      try {
        Thread.sleep(time);
      } catch (InterruptedException e) {
        logErr("InterruptedException Error: "+e.getMessage(),e);
      }
      
      if(isPositionChanged(distanceTarget))
        return;
    }
  }

}
