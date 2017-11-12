package incubator.csl.lcars.micarr.test;

import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
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
import incubator.csl.lcars.micarr.contributors.ESensitivityPlots;

/**
 * -- <i>for testing only</i> --
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class SensitivityPlotTestPanel extends Panel
{
  private ELabel              eGuiLd;
  private ESensitivityPlots   eSensPlts;
  private ElementContributor  eFreqSlider;
  private ElementContributor  eTrolleySlider;
  private ArrayList<EElement> aeButtons;
  
  public SensitivityPlotTestPanel(IScreen iscreen)
  {
    super(iscreen);
  }

  @Override
  public void init()
  {
    super.init();
    aeButtons = new ArrayList<EElement>();

    EEventListenerAdapter buttonListerner = new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        ElementContributor ec = contributorForName((String)ee.el.getData());
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

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eRect = new ERect(this,1720,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,"SENSITIVITY PLOTS");
    eRect.addEEventListener(buttonListerner);
    eRect.setData("eSensPlts");
    add(eRect); 
    aeButtons.add(eRect);

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eRect = new ERect(this,1720,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,"FREQUENCY SLIDER");
    eRect.addEEventListener(buttonListerner);
    eRect.setData("eFreqSlider");
    add(eRect); aeButtons.add(eRect);

    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eRect = new ERect(this,1720,ey,177,60,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,"TROLLEY SLIDER");
    eRect.addEEventListener(buttonListerner);
    eRect.setData("eTrolleySlider");
    add(eRect); aeButtons.add(eRect);
    
    ey += getElements().get(getElements().size()-1).getBounds().height +3;
    eGuiLd = new ELabel(this,1720,ey,170,20,LCARS.ES_STATIC|LCARS.ES_LABEL_E,"000/000");
    eGuiLd.setColor(new ColorMeta(1f,1f,1f,0.25f));
    add(eGuiLd);
    setLoadStatControl(eGuiLd);
    
    LCARS.invokeLater(()->
    {
      fatInit();
    });
  }

  protected void fatInit()
  {
    MicArrayState state = MicArrayState.getCurrentState();
    eSensPlts = new ESensitivityPlots(state,150,150);
    eSensPlts.setSlicePositions(new Point3d(0,0,160));
    eSensPlts.addToPanel(this);
  }
  
  @Override
  protected void fps10()
  {
    for (EElement ee : aeButtons)
    {
      ElementContributor ec = contributorForName((String)ee.getData());

      if (ec==null)
      {
        ee.setSelected(false);
        ee.setDisabled(true);
        ee.setAlpha(0.5f);
      }
      else
      {
        ee.setDisabled(false);
        ee.setSelected(ec.isDisplayed());
        ee.setAlpha(1f);
      }
    }
  }

  protected ElementContributor contributorForName(String name)
  {
    try
    {
      Field f = getClass().getDeclaredField(name);
      return (ElementContributor)f.get(this);
    }
    catch (Exception e)
    {
      e.printStackTrace();
      return null;
    }
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
