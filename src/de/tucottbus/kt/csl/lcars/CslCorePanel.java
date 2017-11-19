package de.tucottbus.kt.csl.lcars;

import java.rmi.RemoteException;

import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_010;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_011;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_012;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ERect;

public class CslCorePanel extends Panel
{
  protected ERect eLock;

  public CslCorePanel(IScreen iscreen)
  {
    super(iscreen);
  }

  @Override
  public void init()
  {
    super.init();
    setTitle("CSL CORE");
  
    ERect eRect = new ERect(this,25,22,208,49,LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W,"EXIT");
    eRect.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        try { getScreen().exit(); } catch (RemoteException e) {}
      }
    });
    add(eRect);
    eLock = new ERect(this,236,22,208,49,LCARS.ES_NOLOCK|LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_E,"PANEL LOCK");
    eLock.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setLocked(!isLocked());
      }
    });
    add(eLock);

    // Create IP power socket controls
    IpPowerSocket_010.getInstance().getLcarsSubpanel(677,180).addToPanel(this);
    IpPowerSocket_011.getInstance().getLcarsSubpanel(677,360).addToPanel(this);
    IpPowerSocket_012.getInstance().getLcarsSubpanel(677,540).addToPanel(this);

    // Enable automatic panel re-locking
    setAutoRelockTime(10);
  }

  @Override
  public void stop()
  {
    super.stop();
    IpPowerSocket_010.getInstance().dispose();
    IpPowerSocket_011.getInstance().dispose();
    IpPowerSocket_012.getInstance().dispose();
  }

  @Override
  protected void fps10() 
  {
    eLock.setBlinking(isLocked());
    if (!isLocked() && getAutoRelockTime()>0)
      eLock.setLabel(String.format("PANEL LOCK (%02d)",getAutoRelock()));
    else
      eLock.setLabel("PANEL LOCK");
  }

  /**
   * Convenience method: Runs the test panel.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",CslCorePanel.class.getName());
    LCARS.main(args);
  }

}
