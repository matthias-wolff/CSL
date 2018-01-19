package de.tucottbus.kt.csl.hardware.accesscontrol;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Observable;
import java.util.Observer;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.led.ALedController;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.swt.ColorMeta;

/**
 * TODO: Write JavaDoc
 * 
 * @author Friedrich Eckert, BTU Cottbus-Senftenberg
 */
public class AccessController extends AAtomicHardware implements Runnable
{

  // -- Fields --

  static volatile int[][] PresenceTable0 = new int[30][4];
  static volatile int[][] PresenceTable1 = new int[30][4];
  static volatile int[][] SystemTable0 = new int[5][6];
  static volatile int[][] SystemTable1 = new int[5][6];
  
  static volatile int SystemTemp0 = 0;
  static volatile int SystemTemp1 = 0;
  
  /*
   * indices for PresenceTable entries
   */ 
  private static final int IND_PRES = 0; // presence status
  private static final int IND_VBAT = 1; // battery voltage
  private static final int IND_TTOT = 2; // total beacon runtime
  private static final int IND_TDUR = 3; // status duration
  
  static volatile int TableToggle = -1;
  static volatile boolean TableLock = false;
  private volatile boolean connectionStatus = false;
  private DatagramSocket UDPsocket = null;
  private static final int SLEEPTIME_PRESET = 1000; // hardware data request interval in ms
  /**
   * The guard thread running the TCP/IP connection to the RasPi.
   */
  private Thread guard = null;
  
  /**
   * Run flag for the guard thread.
   */
  private boolean runGuard = false;
  
  // -- Life cycle --

  /**
   * The access controller hardware wrapper.
   */
  private static volatile AccessController singleton = null;

  /**
   * Returns the access controller hardware wrapper.
   */
  public static synchronized AccessController getInstance()
  {
    if (singleton==null)
      singleton = new AccessController();
    return singleton;
  }

  /**
   * Creates the hardware wrapper singleton for the access controller.
   */
  private AccessController()
  {
    // Initialization is done in thread to avoid delay
    
    // Start the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }
  

  
  /**
   * <p>
   * Start background Thread.
   * </p>
   * <p>
   * periodically reads data from hardware and updates local buffers.
   * </p>
   * <p>
   * Waits for read-out methods to release buffer locks, but can time out to
   * prevent lockup. Received Data will be ignored in this case.
   * </p>
   */

  @Override
  public void run()
  {
    runGuard = true; //

    // initialize arrays
    for (byte n = 0; n < 30; n++)
    {
      PresenceTable0[n][0] = 0;
      PresenceTable0[n][1] = 0;
      PresenceTable0[n][2] = 0;
      PresenceTable0[n][3] = 0;
      
      SystemTemp0 = 0;
      SystemTemp1 = 0;

      PresenceTable1[n][0] = 0;
      PresenceTable1[n][1] = 0;
      PresenceTable1[n][2] = 0;
      PresenceTable1[n][3] = 0;
    }

    for (byte n = 0; n < 5; n++)
    {
      for (byte m = 0; m < 6; m++)
      {
        SystemTable0[n][m] = -1;
        SystemTable1[n][m] = -1;
      }
    }

    TableToggle = 0;

    try
    {
      System.out.println("BEGIN OF GUARD THREAD");
      
      // InetAddress RBPI = InetAddress.getByName("192.168.2.3"); // target ip
      InetAddress RBPI = InetAddress.getByName("141.43.71.15"); // target ip
      // String CLIENT = "192.168.2.1"; // local device ip
      String CLIENT = "143.43.71.42"; // local device ip
      int PORT = 5050; // response receive port
      int PORT_OUT = 5000; // RBPi request port
      String CMD_GET_MSG = "GET_MESSAGE";
      String CMD_SET_THRES = "SET_THRESHOLD=6";
      String CMD_GET_TEMP = "GET_TEMP";

      byte[] txbuffer = new byte[32];
      byte[] rxbuffer = new byte[1700];

      int temp = 0;

      UDPsocket = new DatagramSocket();
      boolean RX_success = false;

      // set receive buffer
      DatagramPacket ResponsePacket = new DatagramPacket(rxbuffer, rxbuffer.length);

      // set transmit buffer -> add .setData when used
      DatagramPacket CommandPacket = new DatagramPacket(txbuffer, txbuffer.length, RBPI, PORT_OUT);
      // RBPI_CMD_packet.setAddress(RBPI);
      // RBPI_CMD_packet.setPort(PORT_OUT);

      UDPsocket.setSoTimeout(1000);
      
      // decoding constants
      
      final int LinePeriode = 48; // length of string including the first
      // LF (\n) terminator
      final int PresenceOffset = 9; // index of first occurrence in
              // receive buffer
      final int VoltageOffset = 14; // -||-
      final int RuntimeOffset = 24; // -||-
      final int asciiOffset = 48; // subtract to get integer
      
      final int BusStatOffset = 1059;
      final int RxConnOffset = 1066;
      final int StatPeriode = 28;
      
      /*
       * Offsets for ID specific data
       */
      
      final int OFS_LLD = 48; // length of ID specific data line
      final int OFS_PR = 9;  // line index of presence status
      final int OFS_VB = 14; // line index of battery voltage (+next 3 values)
      final int OFS_TR = 24; // line index of total runtime
      final int OFS_TD = 37; // line index of status duration
      
      /*
       * Offsets for system data
       */
      final int OFS_LLS = 28;    // length of system specific data line
      final int OFS_BS = 1440 + 9; // offset to first receiver bus status
//      final int OFS_RX = 3;    // index of receiver connection status   
      final int OFS_RT = 1440 + 16; // index of receiver selftest
      
      final int OFS_TMP = 1606; // index of system temperature
      
      
      final int OFS_ASCII = 48; // subtract to get integer
      
      // ---------------------------------------------------

      // other variables
      byte LOCK_TIMEBASE_PRESET = 100; // time/ms to sleep before retry
      byte LOCK_TIMEOUT_PRESET = 80; // retries before timeout
      

      while (runGuard) // inner loop: aquire data from hardware
      {
        try
        {
          // send request
          txbuffer = CMD_GET_MSG.getBytes();
          CommandPacket.setData(txbuffer, 0, CMD_GET_MSG.length());
          UDPsocket.send(CommandPacket);

          // ---------------------------------------------------
          // wait for response

          try
          {
            UDPsocket.receive(ResponsePacket);
            RX_success = true;
            System.out.println("Receive successful");
            connectionStatus = true;
          }
          catch (SocketTimeoutException e1)
          {
            System.out.println("Timeout " + e1);
            // UDPsocket.close();

            RX_success = false;
            connectionStatus = false;
          }

          
          if (RX_success)
          {
//            print received data to console
            
            String receivedString = new String(rxbuffer);
            System.out.println(receivedString);

            // decode received data
            
//TEST
            TableLock = false;
            
            switch (TableToggle)
            {
              case 0:
                {
                  // Table 0 active -> write to inactive Table 1
                  for (int m = 0; m < 30; m++)
                  {
                    temp = 0;
                    PresenceTable1[m][IND_PRES] = (rxbuffer[OFS_PR + (m * OFS_LLD)] - OFS_ASCII); // presence status
                    for (int sc = 0; sc < 4; sc++)
                      temp += (rxbuffer[VoltageOffset + (m * OFS_LLD) + sc] - OFS_ASCII) * Math.pow(10, (3 - sc));
                    PresenceTable1[m][IND_VBAT] = temp; // voltage
    
                    temp = 0;
                    for (int sc = 0; sc < 6; sc++)
                      temp += (rxbuffer[OFS_TR + (m * OFS_LLD) + sc] - OFS_ASCII) * Math.pow(10, (5 - sc));
                    PresenceTable1[m][IND_TTOT] = temp; // runtime
                    
                    temp = 0;
                    for (int sc = 0; sc < 6; sc++)
                      temp += (rxbuffer[OFS_TD + (m * OFS_LLD) + sc] - OFS_ASCII) * Math.pow(10, (5 - sc));
                    PresenceTable1[m][IND_TDUR] = temp; // runtime
                  }

                  for (int m = 0; m < 5; m++)
                  {
                    SystemTable1[m][0] = (rxbuffer[OFS_BS + (m * StatPeriode)] - OFS_ASCII); // Bus on/off
                    for (int n = 0; n < 5; n++)
                      SystemTable1[m][n + 1] = (rxbuffer[OFS_RT + (n * 2) + (m * OFS_LLS)] - OFS_ASCII);  // interconnection status
                  }
                  
                  temp = 0;
                  temp += (rxbuffer[OFS_TMP] - OFS_ASCII) * Math.pow(10, 2);
                  temp += (rxbuffer[OFS_TMP+1] - OFS_ASCII) * Math.pow(10, 1);
                  temp += (rxbuffer[OFS_TMP+3] - OFS_ASCII);
                  SystemTemp1 = temp;
                  
                  int LockTimeout = LOCK_TIMEOUT_PRESET;
                  while (TableLock == true)
                  {
                    Thread.sleep(LOCK_TIMEBASE_PRESET); // wait for 100ms, then
                                                        // check again
                    if ((LockTimeout--) == 0)
                    {
                      // handle timeout
                      System.out.println("AccessController ERROR: Data toggle is blocked");
                      break;
                    }
                  }
                  if (TableLock == false)
                  {
                    TableToggle = 1; // switch over to updated Buffer
                  }
    
                  break;
                }
              case 1:
                {
                  // Table 1 active -> write to inactive Table 0
                  for (int m = 0; m < 30; m++)
                  {
                    temp = 0;
                    PresenceTable0[m][IND_PRES] = (rxbuffer[OFS_PR + (m * OFS_LLD)] - OFS_ASCII); // presence status
                    for (int sc = 0; sc < 4; sc++)
                      temp += (rxbuffer[VoltageOffset + (m * OFS_LLD) + sc] - OFS_ASCII) * Math.pow(10, (3 - sc));
                    PresenceTable0[m][IND_VBAT] = temp; // voltage
    
                    temp = 0;
                    for (int sc = 0; sc < 6; sc++)
                      temp += (rxbuffer[OFS_TR + (m * OFS_LLD) + sc] - OFS_ASCII) * Math.pow(10, (5 - sc));
                    PresenceTable0[m][IND_TTOT] = temp; // runtime
                    
                    temp = 0;
                    for (int sc = 0; sc < 6; sc++)
                      temp += (rxbuffer[OFS_TD + (m * OFS_LLD) + sc] - OFS_ASCII) * Math.pow(10, (5 - sc));
                    PresenceTable0[m][IND_TDUR] = temp; // runtime              
                  }

                  System.out.println(rxbuffer[OFS_BS]+" "+rxbuffer[OFS_BS+1]+" "+rxbuffer[OFS_BS+2]);
                  
                  for (int m = 0; m < 5; m++)
                  {
                    SystemTable0[m][0] = (rxbuffer[OFS_BS + (m * StatPeriode)] - OFS_ASCII); // Bus on/off
                    for (int n = 0; n < 5; n++)
                      SystemTable0[m][n + 1] = (rxbuffer[OFS_RT + (n * 2) + (m * OFS_LLS)] - OFS_ASCII);  // interconnection status
                  }
                  
                  temp = 0;
                  temp += (rxbuffer[OFS_TMP] - OFS_ASCII) * 100;
                  temp += (rxbuffer[OFS_TMP+1] - OFS_ASCII) * 10;
                  temp += (rxbuffer[OFS_TMP+3] - OFS_ASCII);
                  SystemTemp0 = temp;
                  
                  int LockTimeout = LOCK_TIMEOUT_PRESET;
                  while (TableLock == true)
                  {
                    Thread.sleep(LOCK_TIMEBASE_PRESET); // wait for 100ms, then
                                                        // check again
                    if ((LockTimeout--) == 0)
                    {
                      // handle timeout
                      System.out.println("AccessController ERROR: Data toggle is blocked");
                      break;
                    }
                  }
                  if (TableLock == false)
                  {
                    TableToggle = 0; // switch over to updated Buffer                   
                  }
                  break;
                }
              default:
                {
                  // out of bounds -> reset to 0
                  TableToggle = 0;
                  // exeption, error, ...
                  break;
                }
            }
          }
          else
          {
            // RX unsuccessful
          }
          
          try
          {
            setChanged();
            notifyObservers(NOTIFY_STATE);
          }
          catch(Exception e)
          {
            System.out.println("ERROR: "+e);
          }

          Thread.sleep(SLEEPTIME_PRESET);
        }
        catch (InterruptedException e1)
        {
          // socket error
          connectionStatus = false;
          System.out.println("AccessControl UDP Socket Error " + e1);
        }
        catch (SocketTimeoutException e2)
        {
          // timeout
          connectionStatus = false;
          System.out.println("AccessControl UDP Socket Timeout " + e2);
        }
      }
    }
    catch (Exception e)
    {
      System.out.println("Thread Error: " +e);
      // Thread error
      // reset everything
    }
  }

/*  
  @Override
  public void run()
  {
    runGuard = true;
    
    // Initialize guard thread
    System.out.println("BEGIN OF GUARD THREAD");
//    setChanged(); notifyObservers(NOTIFY_CONNECTION);
    
    // Run guard thread
    // TODO: Dummy! Run TCI/IP connection to RasPi.
    while (runGuard)
    {
      try
      {
        setChanged();
        notifyObservers(NOTIFY_STATE);
      }
      catch(Exception e)
      {
        System.out.println("ERROR: "+e);
      }
      try
      {
        Thread.sleep(2000);
      } 
      catch (InterruptedException e) {}
    }
    
    // Finish guard thread
    System.out.println("END OF GUARD THREAD");
  }
*/
  
  @Override
  public void dispose() throws IllegalStateException
  {
    // Gracefully terminate guard thread
    if (guard!=null)
    {
      runGuard = false;
      try 
      {
        guard.interrupt();
        guard.join();
      } 
      catch (Exception e) 
      { 
        logErr("Error disposing AccessController",e);
      }
    }
    guard = null;
    
    super.dispose();
  }
  
  // -- ...
  
  @Override
  public String getName()
  {
    return "ACCESS CONTROL";
  }

  @Override
  public boolean isConnected()
  {
    // TODO: Return current connection status with RasPi
    
    return connectionStatus;
  }

  @Override
  public AHardware getParent()
  {
    // This is a stand-alone device
    return null;
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y)
  {
    if (subPanel==null)
      subPanel = new LcarsSubPanel(x, y);
    return subPanel;
  }
  
  // -- LCARS Element Contributor --

  /**
   * The LCARS GUI for this access controller.
   */
  protected volatile LcarsSubPanel subPanel;
  
  public class LcarsSubPanel extends ElementContributor implements Observer
  {
    final private ColorMeta COLOR_ERROR = LCARS.getColor(LCARS.CS_REDALERT,LCARS.EC_SECONDARY|LCARS.ES_SELECTED);
    final private ColorMeta COLOR_NORMAL = LCARS.getColor(LCARS.CS_SECONDARY,LCARS.ES_NONE|LCARS.ES_STATIC);
    final private ColorMeta COLOR_ACTIVE = LCARS.getColor(LCARS.CS_SECONDARY,LCARS.ES_SELECTED|LCARS.ES_STATIC);
    
    // dynamic text fields
    final private EValue[] ePresField = new EValue[30];
    final private EValue[] eTimeField = new EValue[30];
    final private EValue[] eVoltField = new EValue[30];
    
    // static fill fields (Labels)
    final private ERect[]  eFillPres = new ERect[30]; // identifier swapped to avoid confusion
    final private ERect[]  eFillTime = new ERect[30];
    final private ERect[]  eFillVolt = new ERect[30];
    
    final private EValue[] eStatusField = new EValue[10];
    final private ERect [] eFillField = new ERect[5]; 
    
    // general parameters
    final int W_ABS = 1219; // absolute panel width
    final int OVLP = 1; // overlap to prevent 1px gaps
    final int MARG = 3; // element margin
    final int H_E = 46; // element height
    final int H_LINE = H_E + MARG; // height of line
    
    // element width
    final int W_FILL1 = 120;
    final int W_PRES  = 138;
    final int W_FILL2 = 40;
    final int W_TIME  = 160;
    final int W_FILL3 = 30;
    final int W_VOLT  = 120;
    // sum has to be (W_ABS-MARG)/2 for SCO to be correct!
    
    final int SCO = ((W_ABS - 3)/ 2) + MARG; // 608 + 3 // second coloumn offset (x direction)

    final int wSES = 60; // width of RX status Element
    
    
    
    public LcarsSubPanel(int x, int y)
    {
      super(x,y);
      
      int d = 15;
      for (int c = 0; c < 15; c++)
      {
        // assemble panel left side
        eFillPres[c]  = new  ERect(null,0                                     ,c*H_LINE,W_FILL1+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W,"PERSON "+(c+1)+" IS");
        ePresField[c] = new EValue(null,W_FILL1                               ,c*H_LINE,W_PRES+OVLP,H_E,LCARS.ES_STATIC, "");
        eFillTime[c]  = new  ERect(null,W_FILL1+W_PRES                        ,c*H_LINE,W_FILL2+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E," for ");
        eTimeField[c] = new EValue(null,W_FILL1+W_PRES+W_FILL2                ,c*H_LINE,W_TIME+OVLP,H_E,LCARS.ES_STATIC, "");
        eFillVolt[c]  = new  ERect(null,W_FILL1+W_PRES+W_FILL2+W_TIME         ,c*H_LINE,W_FILL3+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"V:");
        eVoltField[c] = new EValue(null,W_FILL1+W_PRES+W_FILL2+W_TIME+W_FILL3 ,c*H_LINE,W_VOLT+OVLP,H_E,LCARS.ES_STATIC, "");
        
        d = c + 15;
     // assemble panel right side
        eFillPres[d]  = new  ERect(null,SCO                                       ,c*H_LINE,W_FILL1+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"PERSON "+(d+1)+" IS");
        ePresField[d] = new EValue(null,SCO+W_FILL1                               ,c*H_LINE,W_PRES+OVLP,H_E,LCARS.ES_STATIC, "");
        eFillTime[d]  = new  ERect(null,SCO+W_FILL1+W_PRES                        ,c*H_LINE,W_FILL2+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E," for ");
        eTimeField[d] = new EValue(null,SCO+W_FILL1+W_PRES+W_FILL2                ,c*H_LINE,W_TIME+OVLP,H_E,LCARS.ES_STATIC, "");
        eFillVolt[d]  = new  ERect(null,SCO+W_FILL1+W_PRES+W_FILL2+W_TIME         ,c*H_LINE,W_FILL3+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"V:");
        eVoltField[d] = new EValue(null,SCO+W_FILL1+W_PRES+W_FILL2+W_TIME+W_FILL3 ,c*H_LINE,W_VOLT+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_RECT_RND_E, "");
        
        ePresField[c].setValue("UNKNOWN");
        eVoltField[c].setValue("0,00V");
        eTimeField[c].setValue("0min");

        add(eFillPres[c]);
        add(eFillTime[c]);
        add(eFillVolt[c]);
        
        add(ePresField[c]);
        add(eVoltField[c]);
        add(eTimeField[c]);
        
        ePresField[d].setValue("UNKNOWN");
        eVoltField[d].setValue("0,00V");
        eTimeField[d].setValue("0min");

        add(eFillPres[d]);
        add(eFillTime[d]);
        add(eFillVolt[d]);
        
        add(ePresField[d]);
        add(eVoltField[d]);
        add(eTimeField[d]);
      }

      eFillField[0]   = new ERect (null,0,15*H_LINE,150+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_RECT_RND_W,"RECEIVER:");
      eStatusField[0] = new EValue(null,150,15*H_LINE,wSES+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"");
      eStatusField[1] = new EValue(null,210,15*H_LINE,wSES+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"");
      eStatusField[2] = new EValue(null,270,15*H_LINE,wSES+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"");
      eStatusField[3] = new EValue(null,330,15*H_LINE,wSES+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"");
      eStatusField[4] = new EValue(null,390,15*H_LINE,wSES+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"");
      eStatusField[5] = new EValue(null,450,15*H_LINE,220+OVLP,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"");
      eFillField[1]   = new ERect (null,670,15*H_LINE,137, H_E, LCARS.ES_STATIC , "");
      /*3pt gap*/
      eFillField[2]   = new ERect (null,810,15*H_LINE,250+OVLP,H_E,LCARS.ES_STATIC, "RASPBERRY PI TEMPERATURE ");
      eStatusField[6] = new EValue(null,1060,15*H_LINE,159,H_E,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_E,"");

      for (int c = 0; c < 7; c++)
      {
        add(eStatusField[c]);
      }
      
      for (int c = 0; c < 3; c++)
      {
        add(eFillField[c]);
      }

      AccessController.this.addObserver(this);
    }

    @Override
    public void update(Observable o, Object arg)
    {
//      System.err.println("notified: "+arg);

      /*
       * differentiate between runtimes:
       * if < 3h -> in minutes
       * if < 2d -> in hours
       * if > 2d -> in days
       */
      
      TableLock = true; // Acquire Lock
      
      switch (TableToggle)
      {
      case 0: // Read table 0
        {
          for(int c = 0; c < 30; c++)
          {
            switch(PresenceTable0[c][IND_PRES])
            {
              case 0:
              {
                ePresField[c].setValue("ABSENT");
                ePresField[c].setColor(COLOR_NORMAL);
                eTimeField[c].setColor(COLOR_NORMAL);
                eVoltField[c].setColor(COLOR_NORMAL);
                eFillPres[c].setColor(COLOR_NORMAL);
                eFillTime[c].setColor(COLOR_NORMAL);
                eFillVolt[c].setColor(COLOR_NORMAL);
                break;
              }
              case 1:
              {
                ePresField[c].setValue("NEARBY");
                ePresField[c].setColor(COLOR_ACTIVE);
                eTimeField[c].setColor(COLOR_ACTIVE);
                eVoltField[c].setColor(COLOR_ACTIVE);
                eFillPres[c].setColor(COLOR_ACTIVE);
                eFillTime[c].setColor(COLOR_ACTIVE);
                eFillVolt[c].setColor(COLOR_ACTIVE);
                
                break;
              }
              case 2:
              {
                ePresField[c].setValue("PRESENT");
                ePresField[c].setColor(COLOR_ACTIVE);
                eTimeField[c].setColor(COLOR_ACTIVE);
                eVoltField[c].setColor(COLOR_ACTIVE);
                eFillPres[c].setColor(COLOR_ACTIVE);
                eFillTime[c].setColor(COLOR_ACTIVE);
                eFillVolt[c].setColor(COLOR_ACTIVE);
                break;
              }
              default:
              {
                ePresField[c].setValue("UNKNOWN");
                ePresField[c].setColor(COLOR_NORMAL);
                eTimeField[c].setColor(COLOR_NORMAL);
                eVoltField[c].setColor(COLOR_NORMAL);
                eFillPres[c].setColor(COLOR_NORMAL);
                eFillTime[c].setColor(COLOR_NORMAL);
                eFillVolt[c].setColor(COLOR_NORMAL);
                break;
              }
            }

            
            /*
             * display total runtime
             */
            /*
            if((PresenceTable0[c][IND_TTOT] >= 0) && (PresenceTable0[c][IND_TTOT] < 180)) // if < 180min -> display in min
            {
              eTimeField[c].setValue(PresenceTable0[c][IND_TTOT]+" MIN");
            }
            else if ((PresenceTable0[c][IND_TTOT] >= 180) && (PresenceTable0[c][IND_TTOT] <= 2880)) // if between 3 to 42h -> display in h
            {
              eTimeField[c].setValue((PresenceTable0[c][IND_TTOT]/60)+" H");
            }
            else// if more than 48h -> display in d
            {
              eTimeField[c].setValue((PresenceTable0[c][IND_TTOT]/1440)+" D");
            }
            */
            
            if((PresenceTable0[c][IND_TDUR] >= 0) && (PresenceTable0[c][IND_TDUR] < 180)) // if < 180min -> display in min
            {
              eTimeField[c].setValue(PresenceTable0[c][IND_TDUR]+" MIN");
            }
            else if ((PresenceTable0[c][IND_TDUR] >= 180) && (PresenceTable0[c][IND_TDUR] <= 2880)) // if between 3 to 42h -> display in h
            {
              eTimeField[c].setValue((PresenceTable0[c][IND_TDUR]/60)+" H");
            }
            else// if more than 48h -> display in d
            {
              eTimeField[c].setValue((PresenceTable0[c][IND_TDUR]/1440)+" D");
            }

            eVoltField[c].setValue(((PresenceTable0[c][IND_VBAT]/10)/100.0)+" V");
          }
          
          /*########## system status ###########*/
          
          for (int n = 0; n <= 4; n++)
          {
            if (SystemTable0[n][0] == 0)
            {
              eStatusField[n].setValue(""+(n+1));
              eStatusField[n].setColor(COLOR_ERROR);
            }
            else
            {
              if (isConnected() == false) eStatusField[n].setColor(COLOR_ERROR);
              else
              {
                eStatusField[n].setValue(""+(n+1)); 
                eStatusField[n].setColor(COLOR_NORMAL);
              }
            }
          }
          
          // Temperature
          eStatusField[6].setValue((SystemTemp0 / 10)+"."+(SystemTemp0 - (SystemTemp0 / 10)*10)+"°C");
          
          break;
        }
      case 1: // Read table 1
        {
          for(int c = 0; c < 30; c++)
          {
            switch(PresenceTable1[c][0])
            {
              case 0:
              {
                ePresField[c].setValue("ABSENT");
                ePresField[c].setColor(COLOR_NORMAL);
                eTimeField[c].setColor(COLOR_NORMAL);
                eVoltField[c].setColor(COLOR_NORMAL);
                eFillPres[c].setColor(COLOR_NORMAL);
                eFillTime[c].setColor(COLOR_NORMAL);
                eFillVolt[c].setColor(COLOR_NORMAL);
                break;
              }
              case 1:
              {
                ePresField[c].setValue("NEARBY");
                ePresField[c].setColor(COLOR_ACTIVE);
                eTimeField[c].setColor(COLOR_ACTIVE);
                eVoltField[c].setColor(COLOR_ACTIVE);
                eFillPres[c].setColor(COLOR_ACTIVE);
                eFillTime[c].setColor(COLOR_ACTIVE);
                eFillVolt[c].setColor(COLOR_ACTIVE);
                
                break;
              }
              case 2:
              {
                ePresField[c].setValue("PRESENT");
                ePresField[c].setColor(COLOR_ACTIVE);
                eTimeField[c].setColor(COLOR_ACTIVE);
                eVoltField[c].setColor(COLOR_ACTIVE);
                eFillPres[c].setColor(COLOR_ACTIVE);
                eFillTime[c].setColor(COLOR_ACTIVE);
                eFillVolt[c].setColor(COLOR_ACTIVE);
                break;
              }
              default:
              {
                ePresField[c].setValue("UNKNOWN");
                ePresField[c].setColor(COLOR_NORMAL);
                eTimeField[c].setColor(COLOR_NORMAL);
                eVoltField[c].setColor(COLOR_NORMAL);
                eFillPres[c].setColor(COLOR_NORMAL);
                eFillTime[c].setColor(COLOR_NORMAL);
                eFillVolt[c].setColor(COLOR_NORMAL);
                break;
              }
            }

            /*
             * display total runtime
             */
            /*
            if((PresenceTable1[c][IND_TTOT] >= 0) && (PresenceTable1[c][IND_TTOT] < 180)) // if < 180min -> display in min
            {
              eTimeField[c].setValue(PresenceTable1[c][IND_TTOT]+" MIN");
            }
            else if ((PresenceTable1[c][IND_TTOT] >= 180) && (PresenceTable1[c][IND_TTOT] <= 2880)) // if between 3 to 42h -> display in h
            {
              eTimeField[c].setValue((PresenceTable1[c][IND_TTOT]/60)+" H");
            }
            else// if more than 48h -> display in d
            {
              eTimeField[c].setValue((PresenceTable1[c][IND_TTOT]/1440)+" D");
            }
            */
            
            /*
             * display total runtime
             */
            
            if((PresenceTable1[c][IND_TDUR] >= 0) && (PresenceTable1[c][IND_TDUR] < 180)) // if < 180min -> display in min
            {
              eTimeField[c].setValue(PresenceTable1[c][IND_TDUR]+" MIN");
            }
            else if ((PresenceTable1[c][IND_TDUR] >= 180) && (PresenceTable1[c][IND_TDUR] <= 2880)) // if between 3 to 42h -> display in h
            {
              eTimeField[c].setValue((PresenceTable1[c][IND_TDUR]/60)+" H");
            }
            else// if more than 48h -> display in d
            {
              eTimeField[c].setValue((PresenceTable1[c][IND_TDUR]/1440)+" D");
            }
            
            
            eVoltField[c].setValue(((PresenceTable1[c][IND_VBAT]/10)/100.0)+" V");
          }
          
          /*########## system status ###########*/
          
          for (int n = 0; n <= 4; n++)
          {
            if (SystemTable1[n][0] == 0)
            {
              eStatusField[n].setValue(""+(n+1));
              eStatusField[n].setColor(COLOR_ERROR);
            }
            else
            {
              if (isConnected() == false) eStatusField[n].setColor(COLOR_ERROR);
              else
              {
                eStatusField[n].setValue(""+(n+1));   
                eStatusField[n].setColor(COLOR_NORMAL);
              }
              
            }
          }
          
       // Temperature
          eStatusField[6].setValue((SystemTemp1 / 10)+"."+(SystemTemp1 - (SystemTemp1 / 10)*10)+"°C");
          break;
        }
      default:
        {
          // out of  bounds
          break;
        }
      }

      // global connection status
      if (isConnected() == true)
      {
        eStatusField[5].setValue("CONNECTED");
        eStatusField[5].setColor(COLOR_NORMAL);
        eFillField[0].setColor(COLOR_NORMAL);
        eFillField[1].setColor(COLOR_NORMAL);
      }
      else
      {
        eStatusField[5].setValue("DISCONNECTED");
        eStatusField[5].setColor(COLOR_ERROR);
        eFillField[0].setColor(COLOR_ERROR);
        eFillField[1].setColor(COLOR_ERROR);
      }

      TableLock = false; // release Lock
      
//      System.out.println("update complete");

    }
  }
  
  // -- LCARS Low-level Hardware Access Panel --

  public static class AccessControllerPanel extends HardwareAccessPanel
  {
    public AccessControllerPanel(IScreen iscreen) 
    {
      super(iscreen, ALedController.class);
    }

    @Override
    protected void createSubPanels() 
    {
      AccessController.getInstance().getLcarsSubpanel(677,140).addToPanel(this);
    }
  }

  // == Main method ==
  
  /**
   * Starts LCARS with the {@link AccessControllerPanel}.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    System.out.println("BEGIN OF MAIN");
//
//    AccessController ac = AccessController.getInstance();
//    
//    System.out.println("Press Enter!");
//    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//    try
//    {
//      in.readLine();
//    } 
//    catch (IOException e)
//    {
//      e.printStackTrace();
//    }
//    
//    ac.dispose();
//    
//    System.out.println("END OF MAIN");
    
    // TODO: This is the final main method -->
    args = LCARS.setArg(args,"--panel=",AccessControllerPanel.class.getName());
    LCARS.main(args);
    getInstance().dispose();
    // <--
  }

}

// EOF
