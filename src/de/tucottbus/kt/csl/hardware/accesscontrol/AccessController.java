package de.tucottbus.kt.csl.hardware.accesscontrol;

import java.util.Observable;
import java.util.Observer;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.led.ALedController;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EValue;

/**
 * TODO: Write JavaDoc
 * 
 * @author Friedrich Eckert, BTU Cottbus-Senftenberg
 */
public class AccessController extends AAtomicHardware implements Runnable
{

  // -- Fields --
  static int[] PresenceTable = new int[30];
  
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
    // TODO: Initialization
    
    // Start the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }

  @Override
  public void run()
  {
    runGuard = true;
    
    // Initialize guard thread
    System.out.println("BEGIN OF GUARD THREAD");
    setChanged(); notifyObservers(NOTIFY_CONNECTION);
    
    // Run guard thread
    // TODO: Dummy! Run TCI/IP connection to RasPi.
    while (runGuard)
    {
      try
      {
        Thread.sleep(1000);
      } 
      catch (InterruptedException e) {}
    }
    
    // Finish guard thread
    System.out.println("END OF GUARD THREAD");
  }
  
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
    return false;
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
   * The LCARS GUI for this IP LED controller.
   */
  protected volatile LcarsSubPanel subPanel;
  
  public class LcarsSubPanel extends ElementContributor implements Observer
  {
   
    final EValue[] ePresField = new EValue[30];
    final EValue[] eTimeField = new EValue[30];
    final EValue[] eVoltField = new EValue[30];
    
    final int marg = 3; // element margin
    final int hE = 46; // element height
    final int wE = 280; // element width
    final int wV = 180; // Voltage element width
    final int wT = 140; // Time element width
    final int sco = (1220/2); // second coloumn offset (x direction)
    
    final EValue[] eStatusField = new EValue[10];
    final int wSE = (1220/10)-marg; // status element width
    
    
    
    public LcarsSubPanel(int x, int y)
    {
      super(x,y);
      
      for (int c = 0; c < 15; c++)
      {
        // assemble panel left side
        ePresField[c] = new EValue(null,0,c*(hE + marg),wE,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W,"PERSON "+(c+1)+" IS");
        eTimeField[c] = new EValue(null,wE,c*(hE + marg),wT,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"T:");
        eVoltField[c] = new EValue(null,(wE+wT),c*(hE + marg),wV,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"V:");
        
        ePresField[c].setValue("UNKNOWN");
        eVoltField[c].setValue("0mV");
        eTimeField[c].setValue("0min");
        
        add(ePresField[c]);
        add(eVoltField[c]);
        add(eTimeField[c]);
      }
      
      for (int c = 15; c < 30; c++)
      {
        // assemble panel right side
        ePresField[c] = new EValue(null,sco,(c-15)*(hE + marg),wE,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"PERSON "+(c+1)+" IS");
        eTimeField[c] = new EValue(null,sco+wE,(c-15)*(hE + marg),wT,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"T:");
        eVoltField[c] = new EValue(null,sco+(wE+wT),(c-15)*(hE + marg),wV,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_E,"V:");

        ePresField[c].setValue("UNKNOWN");
        eVoltField[c].setValue("0mV");
        eTimeField[c].setValue("0min");
        
        add(ePresField[c]);
        add(eVoltField[c]);
        add(eTimeField[c]);
      }

      // receivers
      /*
      for (int c = 0; c < 10; c++)
      {
        eStatusField[c] = new EValue(null,c*(wSE+marg),15*(hE + marg),wSE,hE,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"RX"+(c+1));
        
        add(eStatusField[c]);
      }
      eStatusField[0].setStyle(LCARS.ES_RECT_RND_W); // make first field round on left side
      eStatusField[9].setStyle(LCARS.ES_RECT_RND_E); // make las field round on right side
*/
      AccessController.this.addObserver(this);

    /*
    // Test working
    final EValue eTest;
    final EValue eTest2;
    
    public LcarsSubPanel(int x, int y)
    {
      super(x,y);
      System.out.println("X: "+x+"\nY: "+y);
      
      eTest = new EValue(null,0,0,(1220/2)-3,50,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W,"PERSON 1 IS");
      eTest2 = new EValue(null,(1220/2),0,(1220/2)-3,50,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_E,"PERSON 2 IS");
//      eTest.setBlinking(true);
      add(eTest);
      add(eTest2);
      
      AccessController.this.addObserver(this);
    }
  */
    }

    @Override
    public void update(Observable o, Object arg)
    {
      System.err.println("notified: "+arg);
      
      for(int c = 0; c < 30; c++)
      {
        switch(PresenceTable[c])
        {
          case 0: ePresField[c].setValue("ABSENT"); {break;}
          case 1: ePresField[c].setValue("NEARBY"); {break;}
          case 2: ePresField[c].setValue("PRESENT"); {break;}
          default: ePresField[c].setValue("UNKNOWN"); {break;}
        }
//        eTimeField[c].setValue((c*3)+" MIN");
//        eVoltField[c].setValue((3000-c)+" mV");
                
      }
      
      /*
       * differentiate between runtimes:
       * if < 1h -> in minutes
       * if < 1d -> in hours
       * if > 1d -> in days
       */
      
      for(int c = 0; c < 10; c++)
      {
        eStatusField[c].setValue("1");
      }

      
//      eTest.setValue("PRESENT");
//      eTest2.setValue("ABSENT");
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
//    System.out.println("BEGIN OF MAIN");
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

    for (int c = 0; c < 30; c++)
    {
      PresenceTable[c] = c % 3;
    }
    
    // TODO: This is the final main method -->
    args = LCARS.setArg(args,"--panel=",AccessControllerPanel.class.getName());
    LCARS.main(args);
    getInstance().dispose();
    // <--
  }

}

// EOF
