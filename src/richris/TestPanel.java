package richris;

import java.awt.Dimension;
import java.rmi.RemoteException;

import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_010;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_011;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_012;
import de.tucottbus.kt.csl.hardware.powerip.IpPowerSocket_013;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.EElementArray;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;

public class TestPanel extends Panel
{
  protected ERect ePanelLock;
  
  public TestPanel(IScreen iscreen)
  {
    super(iscreen);
  }

  @Override
  public void init()
  {
    super.init();
    setTitle("IP SOCKET PANEL");
    setTitleLabel(null);
  
    ERect eRect;
    EValue eValue;
    EElbo eElbo;
    
    
    // additional button by Chris
//    eRect = new ERect(this,236,174,150,49,LCARS.EC_ELBOUP|LCARS.ES_LABEL_W|LCARS.ES_RECT_RND_E,"POWER");
//    eRect.addEEventListener(new EEventListenerAdapter()
//    {
//      @Override
//      public void touchDown(EEvent ee)
//      {
//        try { getScreen().exit(); } catch (RemoteException e) {}
//      }
//    });
//    add (eRect);

    
    //left wing by Chris
    eRect =  new ERect(this,27,23,70,70,LCARS.ES_STATIC|LCARS.EC_ELBOUP|LCARS.ES_RECT_RND_W,null);
    add(eRect);

    eElbo = new EElbo(null,102,23,435,115,LCARS.EC_PRIMARY|LCARS.ES_STATIC|LCARS.ES_SELECTED|LCARS.ES_SHAPE_NE|LCARS.ES_LABEL_SE,"L01");
    eElbo.setArmWidths(85, 70); eElbo.setArcWidths(130, 50);
    add(eElbo);
   
    // The hardware sub-panels list
    ERect ePrev = new ERect(this,452,141,85,330,LCARS.EC_ELBOUP|LCARS.ES_LABEL_SW,"PREV");   
    add (ePrev);   

    ERect eLock = new ERect(this,452,474,85,132,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_SW,"LOCK");   
    add (eLock); 

    ERect eNext = new ERect(this,452,609,85,330,LCARS.EC_ELBOUP|LCARS.ES_LABEL_NW,"NEXT");   
    add (eNext);
    
    EElementArray eSubpanelList = new EElementArray(100,108,ERect.class,new Dimension(336,59),14,1,LCARS.EC_ELBOUP|LCARS.ES_RECT_RND|LCARS.ES_LABEL_W,null);
    eSubpanelList.setPageControls(ePrev,eNext); eSubpanelList.setLockControl(eLock);
    eSubpanelList.add("POWER");
    for (int i=1; i<=20; i++)
      eSubpanelList.add("BUTTON "+i).setDisabled(true);;
    eSubpanelList.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        if ("POWER".equals(ee.el.getLabel()))
          panelSelectionDialog();
      }
    });
    eSubpanelList.addToPanel(this);
    
    
    eElbo = new EElbo(null,102,942,435,115,LCARS.EC_PRIMARY|LCARS.ES_STATIC|LCARS.ES_SELECTED|LCARS.ES_SHAPE_SE|LCARS.ES_LABEL_NE,"L00");
    eElbo.setArmWidths(85, 70); eElbo.setArcWidths(130, 50);
    add(eElbo);
    
    
    eRect = new ERect(this,27,987,70,70,LCARS.ES_STATIC|LCARS.EC_ELBOUP|LCARS.ES_RECT_RND_W,null);
    add(eRect);
    
    // right wing by Chris
    eValue = new EValue(this,1666,23,231,70,LCARS.ES_STATIC|LCARS.ES_SELECTED|LCARS.ES_VALUE_W|LCARS.EC_PRIMARY|LCARS.ES_RECT_RND_E,null);
    eValue.setValue("CSL CORE"); eValue.setValueWidth(195); eValue.setValueMargin(56);
    add(eValue);
    
    
    
    eElbo = new EElbo(null,549,23,1083,115,LCARS.EC_PRIMARY|LCARS.ES_STATIC|LCARS.ES_SELECTED|LCARS.ES_SHAPE_NW|LCARS.ES_LABEL_SE,"R01");
    eElbo.setArmWidths(170, 70); eElbo.setArcWidths(130, 50);
    add(eElbo);
   
    
    
    eRect = new ERect(this,549,141,170,230,LCARS.EC_ELBOUP|LCARS.ES_LABEL_SW,null);   
    add (eRect);   
    
    
        
    eRect = new ERect(this,549,374,170,332,LCARS.EC_SECONDARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_SW,null);   
    add (eRect); 
 
    
    
    eRect = new ERect(this,549,709,170,230,LCARS.EC_ELBOUP|LCARS.ES_LABEL_SW,null);   
    add (eRect);
    
    
    
    eElbo = new EElbo(null,549,942,800,115,LCARS.EC_PRIMARY|LCARS.ES_STATIC|LCARS.ES_SELECTED|LCARS.ES_SHAPE_SW|LCARS.ES_LABEL_SE,"R00");
    eElbo.setArmWidths(170, 70); eElbo.setArcWidths(130, 50);
    add(eElbo);
    
    
    
    ePanelLock = new ERect(this,1352,987,280,70,LCARS.ES_NOLOCK|LCARS.EC_PRIMARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_SW,"LOCK");
    ePanelLock.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        setLocked(!isLocked());
      }
    });
    add(ePanelLock);
    
    
    eRect = new ERect(this,1635,987,200,70,LCARS.EC_PRIMARY|LCARS.ES_SELECTED|LCARS.ES_LABEL_SW,"EXIT");
    eRect.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        try { getScreen().exit(); } catch (RemoteException e) {}
      }
    });
    add (eRect);
    
    
    eValue = new EValue(this,1790,987,101,70,LCARS.ES_STATIC|LCARS.EC_ELBOUP|LCARS.ES_RECT_RND_E,null);
    eValue.setValueWidth(60); eValue.setValueMargin(50);
    add(eValue);
    
    
    // Create IP power socket controls
    IpPowerSocket_010.getInstance().getLcarsSubpanel(815,180).addToPanel(this);
    IpPowerSocket_011.getInstance().getLcarsSubpanel(815,360).addToPanel(this);
    IpPowerSocket_012.getInstance().getLcarsSubpanel(815,540).addToPanel(this);
    IpPowerSocket_013.getInstance().getLcarsSubpanel(815,720).addToPanel(this);

    // Enable automatic panel re-locking
    //setAutoRelockTime(10);
  }

  @Override
  public void stop()
  {
    super.stop();
    IpPowerSocket_010.getInstance().dispose();
    IpPowerSocket_011.getInstance().dispose();
    IpPowerSocket_012.getInstance().dispose();
    IpPowerSocket_013.getInstance().dispose();
  }

  @Override
  protected void fps10() 
  {
    ePanelLock.setBlinking(isLocked());
    if (!isLocked() && getAutoRelockTime()>0)
      ePanelLock.setLabel(String.format("PANEL LOCK (%02d)",getAutoRelock()));
    else
      ePanelLock.setLabel("PANEL LOCK");
  }

  /**
   * Convenience method: Runs the test panel.
   * 
   * @param args
   *          The command line arguments, see {@link LCARS#main(String[])}.
   */
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",TestPanel.class.getName());
    LCARS.main(args);
  }

}
