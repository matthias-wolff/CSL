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
    final EValue eTest;
    
    public LcarsSubPanel(int x, int y)
    {
      super(x,y);
      
      eTest = new EValue(null,0,0,400,48,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W,"PERSON 1 IS");
      eTest.setBlinking(true);
      add(eTest);
      
      AccessController.this.addObserver(this);
    }

    @Override
    public void update(Observable o, Object arg)
    {
      System.err.println("notified: "+arg);
      eTest.setValue("PRESENT");
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

    // TODO: This is the final main method -->
    args = LCARS.setArg(args,"--panel=",AccessControllerPanel.class.getName());
    LCARS.main(args);
    // <--
  }

}

// EOF
