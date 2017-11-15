package incubator.csl.lcars.micarr.test;

import java.rmi.RemoteException;
import java.util.HashMap;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.swt.ColorMeta;
import incubator.csl.lcars.micarr.contributors.ECslSlider;
import incubator.csl.lcars.micarr.contributors.ESensitivityPlots;

/**
 * -- <i>for testing only</i> --
 * 
 * <h3>Remarks</h3>
 * <ul>
 *   <li>TODO: Create slider contributor.</li>
 * </ul>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class SensitivityPlotTestPanel extends Panel
{
  private static final String BTN_PLOTS = "SENSITIVITY PLOTS";
  private static final String BTN_TLYSL = "TROLLEY SLIDER";
  private static final String BTN_TSTSL = "TEST SLIDER";

  private ELabel                   eGuiLd;
  private ELabel                   eColorScheme;
  private ESensitivityPlots        eSensPlts;
  private ElementContributor       eTrolleySlider;
  private HashMap<String,EElement> aeButtons;
  
  private boolean linkSteering = true;
  
  public SensitivityPlotTestPanel(IScreen iscreen)
  {
    super(iscreen);
  }

  @Override
  public void init()
  {
    super.init();
    aeButtons = new HashMap<String,EElement>();

    EEventListenerAdapter buttonListerner = new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        ElementContributor ec = (ElementContributor)ee.el.getData();
        if (ec==null)
          return;
        if (ec.isDisplayed())
          ec.removeFromPanel();
        else
          ec.addToPanel(SensitivityPlotTestPanel.this);
      }
    };

    int ex = 1720;
    int ey = 120;
    ERect eRect = new ERect(this,ex,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,"EXIT");
    eRect.addEEventListener(new EEventListenerAdapter()
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
    add(eRect);

    ey += getElements().get(getElements().size()-1).getBounds().height +23;
    eRect = new ERect(this,1720,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,BTN_PLOTS);
    eRect.addEEventListener(buttonListerner);
    add(eRect); aeButtons.put(eRect.getLabel(),eRect);

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eRect = new ERect(this,1720,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,BTN_TLYSL);
    eRect.addEEventListener(buttonListerner);
    add(eRect); aeButtons.put(eRect.getLabel(),eRect);

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eRect = new ERect(this,1720,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,BTN_TSTSL);
    eRect.addEEventListener(buttonListerner);
    add(eRect); aeButtons.put(eRect.getLabel(),eRect);

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eGuiLd = new ELabel(this,1720,ey,170,26,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"000/000");
    eGuiLd.setColor(new ColorMeta(1f,1f,1f,0.25f));
    add(eGuiLd);
    setLoadStatControl(eGuiLd);

    ey += getElements().get(getElements().size()-1).getBounds().height +23;
    ERect eDim = add(new ERect(this,ex,ey,85,60,LCARS.ES_RECT_RND_W|LCARS.EC_SECONDARY|LCARS.ES_LABEL_SE,"DIM"));
    ERect eLight = add(new ERect(this,ex+88,ey,85,60,LCARS.ES_RECT_RND_E|LCARS.EC_SECONDARY|LCARS.ES_LABEL_SE,"LIGHT"));
    setDimContols(eLight, eDim);

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eRect = new ERect(this,ex,ey,177,60,LCARS.ES_RECT_RND|LCARS.EC_SECONDARY|LCARS.ES_LABEL_SE,"COLOR SCHEME");
    eRect.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        int cs = getColorScheme() +1;
        if (cs>LCARS.CS_MAX)
          cs = 0;
        setColorScheme(cs);
        switch (cs)
        {
        case LCARS.CS_KT       : eColorScheme.setLabel("CS_KT"       ); break;
        case LCARS.CS_PRIMARY  : eColorScheme.setLabel("CS_PRIMARY"  ); break;
        case LCARS.CS_SECONDARY: eColorScheme.setLabel("CS_SECONDARY"); break;
        case LCARS.CS_ANCILLARY: eColorScheme.setLabel("CS_ANCILLARY"); break;
        case LCARS.CS_DATABASE : eColorScheme.setLabel("CS_DATABASE" ); break;
        case LCARS.CS_MULTIDISP: eColorScheme.setLabel("CS_MULTIDISP"); break;
        case LCARS.CS_REDALERT : eColorScheme.setLabel("CS_REDALERT" ); break;
        default                : eColorScheme.setLabel("CS_???"      ); break;
        }
      }
    });
    add(eRect);
    
    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eColorScheme = new ELabel(this,1720,ey,170,26,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"CS_MULIDISP");
    eColorScheme.setColor(new ColorMeta(1f,1f,1f,0.25f));
    add(eColorScheme);
    
    ECslSlider slider = new ECslSlider(170,800,440,40,ECslSlider.ES_HORIZONTAL,20);
    aeButtons.get(BTN_TSTSL).setData(slider);
    
    LCARS.invokeLater(()->
    {
      fatInit();
    });
  }

  protected void fatInit()
  {
    eSensPlts = new ESensitivityPlots(170,170);
    eSensPlts.addSelectionListener(new ESensitivityPlots.SelectionListener()
    {
      @Override
      public void selectionChanged(Point3d point)
      {
        if (linkSteering)
          DoAEstimator.getInstance().setTargetSource(point);
      }
    });
    eSensPlts.addToPanel(this);
    aeButtons.get(BTN_PLOTS).setData(eSensPlts);
  }
  
  @Override
  protected void fps10()
  {
    if (eSensPlts!=null && linkSteering && LCARS.getArg("--nohardware")==null)
      eSensPlts.setMicArrayState(MicArrayState.getCurrent());
    
    for (EElement e : aeButtons.values())
    {
      ElementContributor ec = (ElementContributor)e.getData();

      if (ec==null)
      {
        e.setSelected(false);
        e.setDisabled(true);
        e.setAlpha(0.5f);
      }
      else
      {
        e.setDisabled(false);
        e.setSelected(ec.isDisplayed());
        e.setAlpha(1f);
      }
    }
  }
  
  // -- Main method --
  
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",SensitivityPlotTestPanel.class.getName());
    LCARS.main(args);
    if (LCARS.getArg("--nohardware")==null)
    {
      CslHardware.getInstance().dispose();
      System.exit(0);
    }
  }
  
}

// EOF
