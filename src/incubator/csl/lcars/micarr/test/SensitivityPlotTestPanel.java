package incubator.csl.lcars.micarr.test;

import java.io.File;
import java.io.FileNotFoundException;
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
import de.tucottbus.kt.lcars.elements.EImage;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.swt.ColorMeta;
import de.tucottbus.kt.lcars.swt.ImageMeta;
import incubator.csl.lcars.micarr.contributors.ECslSlider;
import incubator.csl.lcars.micarr.contributors.ECslSliderCursor;
import incubator.csl.lcars.micarr.contributors.ESensitivityPlots;

/**
 * -- <i>for testing only</i> --
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
  private ESliderCursorTester      eSct;
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

    // Slider cursor test
    eSct = new ESliderCursorTester(400,170);
    //eSct.addToPanel(this);
    aeButtons.get(BTN_TSTSL).setData(eSct);

    // Fat initialization
    LCARS.invokeLater(()->
    {
      fatInit();
    });
  }

  protected void fatInit()
  {
    eSensPlts = new ESensitivityPlots(120,170);
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
    
//    ECslSlider cs = new ECslSlider(120,1000,400,20,ECslSlider.ES_HORIZONTAL,10);
//    cs.addSelectionListener((value)->
//    {
//    });
//    cs.addToPanel(this);
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
  
  // == Nested classes ==
  
  protected class ESliderCursorTester extends ElementContributor
  {

    public ESliderCursorTester(int x, int y)
    {
      super(x, y);

      EImage eImg;
      try
      {
        File imgFile;
        imgFile = LCARS.getResourceFile("incubator.csl.lcars.micarr.test","plot.png");
        eImg = new EImage(null,0,0,LCARS.ES_STATIC,new ImageMeta.File(imgFile.getAbsolutePath())); 
        eImg.setAlpha(0.1f);
        add(eImg);
      } 
      catch (FileNotFoundException e)
      {
        e.printStackTrace();
        return;
      }

      // TODO: Add ECslSliderCursors at all sides of the image
      int w = 440;
      int h = 440;
      final int CURSOR_WIDTH = 3; 
      final int KNOB_GAP = 3; 
      final int KNOB_SIZE = 48;
      ECslSliderCursor eCsc;

      // - Right
      eCsc = new ECslSliderCursor(w+KNOB_GAP,0,KNOB_SIZE,h,LCARS.EC_SECONDARY|ECslSliderCursor.ES_VERT_LINE_W,0,w+KNOB_GAP,CURSOR_WIDTH);
      eCsc.eKnob.setLabel("000");
      eCsc.setStatic(true);
      eCsc.addScaleTick(0.45f,"0.45",LCARS.EF_SMALL);
      add(eCsc);

      // - Left
      eCsc = new ECslSliderCursor(-KNOB_GAP-KNOB_SIZE,0,KNOB_SIZE,h,ECslSliderCursor.ES_VERT_LINE_E,0,w+KNOB_GAP,CURSOR_WIDTH);
      eCsc.eKnob.setLabel("000");
      eCsc.addScaleTick(0.55f,"0.55",LCARS.EF_SMALL);
      add(eCsc);

      // - Top
      eCsc = new ECslSliderCursor(0,-KNOB_GAP-KNOB_SIZE*3/4,w,KNOB_SIZE,LCARS.EC_SECONDARY|ECslSliderCursor.ES_HORIZ_LINE_S|ECslSliderCursor.ES_ROTATE_KNOB,0,h+KNOB_GAP,CURSOR_WIDTH);
      eCsc.eKnob.setLabel("000");
      eCsc.addScaleTick(0.45f,"0.45",LCARS.EF_SMALL);
      add(eCsc);

      // - Bottom
      eCsc = new ECslSliderCursor(0,h+KNOB_GAP-KNOB_SIZE/4,w,KNOB_SIZE,ECslSliderCursor.ES_HORIZ_LINE_N|ECslSliderCursor.ES_ROTATE_KNOB,0,h+KNOB_GAP,CURSOR_WIDTH);
      eCsc.eKnob.setLabel("000");
      eCsc.addScaleTick(0.55f,"0.55",LCARS.EF_SMALL);
      add(eCsc);
    }
  }
  
  // == Main method ==
  
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
