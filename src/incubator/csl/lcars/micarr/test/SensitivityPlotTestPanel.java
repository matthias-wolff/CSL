package incubator.csl.lcars.micarr.test;

import java.rmi.RemoteException;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.swt.ColorMeta;
import de.tucottbus.kt.lcars.util.LoadStatistics;
import incubator.csl.lcars.micarr.contributors.ESensitivityPlots;

/**
 * -- <i>for testing only</i> --
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class SensitivityPlotTestPanel extends Panel
{
  private ELabel            eGuiLd;
  private ESensitivityPlots eSensPlts;
  
  public SensitivityPlotTestPanel(IScreen iscreen)
  {
    super(iscreen);
  }

  @Override
  public void init()
  {
    super.init();

    ERect eLcars = new ERect(this,1720,120,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,"EXIT");
    eLcars.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        try
        {
          getScreen().exit();
        } 
        catch (RemoteException e)
        {
          e.printStackTrace();
        }
      }
    });
    add(eLcars);
    
    eGuiLd = new ELabel(this,1720,183,170,20,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"000/000");
    eGuiLd.setColor(new ColorMeta(1f,1f,1f,0.25f));
    add(eGuiLd);

    MicArrayState state = MicArrayState.getCurrentState();
    eSensPlts = new ESensitivityPlots(state,150,150);
    eSensPlts.setSlicePositions(new Point3d(0,0,160));
    eSensPlts.addToPanel(this);
  }

  @Override
  protected void fps10()
  {
    LoadStatistics ls1 = getLoadStatistics();
    String s = String.format("%03d-%02d",ls1.getLoad(),ls1.getEventsPerPeriod());
    try
    {
      LoadStatistics ls2 = getScreen().getLoadStatistics();
      s += String.format("/%03d-%02d",ls2.getLoad(),ls2.getEventsPerPeriod());
    }
    catch (Exception e)
    {
      // Whatever...
    }
    eGuiLd.setLabel(s);
  }

  // -- Main method --
  
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",SensitivityPlotTestPanel.class.getName());
    LCARS.main(args);
    CslHardware.getInstance().dispose();
    System.exit(0);
  }
  
}

// EOF
