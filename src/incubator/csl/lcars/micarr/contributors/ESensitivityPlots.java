package incubator.csl.lcars.micarr.contributors;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListener;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.GArea;
import incubator.csl.lcars.micarr.elements.ESensitivityPlot;
import incubator.csl.lcars.micarr.geometry.GSensitivityScale;
import incubator.csl.lcars.micarr.geometry.rendering.CpuSensitivityRenderer;

/**
 * This class contributes 2D sensitivity plots of CLS's microphone array to an
 * LCARS panel.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Show outlines of microphones array over grids of plots.</li>
 *   <li>TODO: Add animation (Random trajectory of slice positions attracted to steering focus and repelled from room borders).</li>
 * </ul></p>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class ESensitivityPlots extends ElementContributor
{
  // -- Constants --
  protected static final int CURSOR_WIDTH = 3;
  protected static final int KNOB_GAP     = 3;
  protected static final int KNOB_SIZE    = 44;
  
  // -- Fields --
  
  private       MicArrayState     mas;
  private final ESensitivityPlot  eSpxy;
  private final CCursor           gSpxyCursorH;
  private final CCursor           gSpxyCursorV;
  private final ESensitivityPlot  eSpyz;
  private final CCursor           gSpyzCursorH;
  private final CCursor           gSpyzCursorV;
  private final ESensitivityPlot  eSpxz;
  private final CCursor           gSpxzCursorH;
  private final CCursor           gSpxzCursorV;
  private final EEventListener    plotSelectionListener;
  private final EValue            eXPos;
  private final EValue            eYPos;
  private final EValue            eZPos;
  private final EElbo             eXyYz;
  private final EElement          eXyYzArrow;
  private final EElbo             eXyXz1;
  private final EElbo             eXyXz2;
  private final EElement          eXyXzArrow;
  private final CSensitivityScale gSensScale;
  private final ECslSlider        eFreqSlider;
  private final EValue            eFreq;

  // -- Life cycle --
  
  /**
   * Creates a new 2D sensitivity plots contributors.
   * 
   * @param x
   *          The x-coordinate of the top-left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the top-left corner (in LCARS panel pixels).
   */
  public ESensitivityPlots(int x, int y)
  {
    super(x, y);
    this.mas = MicArrayState.getDummy();
    
    EElbo eElbo;
    EValue eValue;
    
    // Create common event listeners
    plotSelectionListener = new EEventListenerAdapter()
    {
      @Override
      public void touchDrag(EEvent ee)
      {
        if (((ESensitivityPlot)ee.el).usesCL())
          touchDown(ee);
      }
      
      @Override
      public void touchDown(EEvent ee)
      {
        Point3d point = ((ESensitivityPlot)ee.el).elementToCsl(ee.pt);
        setSelecion(point);
        fireSelectionChanged(point);
      }
    };

    int ex;
    int ey;
    int ew;
    
    // XY-plot
    eSpxy = new ESensitivityPlot(null,this.x,this.y,-1,-1,ESensitivityPlot.SLICE_XY,this.mas);
    eSpxy.addEEventListener(plotSelectionListener);
    add(eSpxy);
    
    // - XY-plot: Frame
    ex = -KNOB_SIZE*7/8-KNOB_GAP;
    ey = -KNOB_GAP-KNOB_SIZE/2;
    eValue = new EValue(null, ex, ey, KNOB_SIZE*7/8+KNOB_GAP, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
    eValue.setValueMargin(0); eValue.setValue("XY");
    add(eValue);
    
    // - XY-plot: Cursors, grid and scales
    gSpxyCursorH = new CCursor(eSpxy,true,true);
    gSpxyCursorH.addScaleTick( 20, "200",true);
    gSpxyCursorH.addScaleTick( 40,"y/cm",false);
    gSpxyCursorH.addScaleTick(120, "100",true);
    gSpxyCursorH.addScaleTick(220,   "0",true);
    gSpxyCursorH.addScaleTick(320,"-100",true);
    gSpxyCursorH.addScaleTick(420,"-200",true);
    gSpxyCursorV = new CCursor(eSpxy,false,true);
    gSpxyCursorV.addScaleTick( 20,"-200",true);
    gSpxyCursorV.addScaleTick(120,"-100",true);
    gSpxyCursorV.addScaleTick(220,   "0",true);
    gSpxyCursorV.addScaleTick(320, "100",true);
    gSpxyCursorV.addScaleTick(390,"x/cm",false);
    gSpxyCursorV.addScaleTick(420, "200",true);
    addAll(gSpxyCursorH);
    addAll(gSpxyCursorV);

    // XZ-plot
    ex = 490;
    ey = 0;
    eSpxz = new ESensitivityPlot(null,this.x+ex,this.y+ey,-1,-1,ESensitivityPlot.SLICE_XZ,this.mas);
    eSpxz.addEEventListener(plotSelectionListener);
    add(eSpxz);

    // - XZ-plot: Frame
    ey += -KNOB_SIZE/2-KNOB_GAP;
    int eh = eSpxz.getBounds().height/2 + KNOB_SIZE/2 + KNOB_GAP;
    eElbo = new EElbo(null,ex-13,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    eElbo = new EElbo(null,ex-13,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SW,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    ex += eSpxz.getBounds().width;
    eValue = new EValue(null, ex+3, ey, KNOB_GAP + KNOB_SIZE-2, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
    eValue.setValueMargin(0); eValue.setValue("XZ");
    add(eValue);
    ex += KNOB_GAP + KNOB_SIZE;
    eElbo = new EElbo(null,ex,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NE,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    eElbo = new EElbo(null,ex,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    ey += 2*eh - KNOB_SIZE /2;
    eValue = new EValue(null, ex-160, ey, 157, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.EC_ELBOUP, null);
    eValue.setValueMargin(KNOB_SIZE-KNOB_GAP); eValue.setValue("REAR>FRONT");
    add(eValue);

    // - XZ-plot: Cursors, grid and scales
    gSpxzCursorH = new CCursor(eSpxz,true,false);
    gSpxzCursorH.addScaleTick(250,   "0",true);
    gSpxzCursorH.addScaleTick(150, "100",true);
    gSpxzCursorH.addScaleTick( 50, "200",true);
    gSpxzCursorH.addScaleTick( 30,"z/cm",false);
    gSpxzCursorV = new CCursor(eSpxz,false,true);
    gSpxzCursorV.addScaleTick( 20,"-200",true);
    gSpxzCursorV.addScaleTick(120,"-100",true);
    gSpxzCursorV.addScaleTick(220,   "0",true);
    gSpxzCursorV.addScaleTick(320, "100",true);
    gSpxzCursorV.addScaleTick(390,"x/cm",false);
    gSpxzCursorV.addScaleTick(420, "200",true);
    addAll(gSpxzCursorH);
    addAll(gSpxzCursorV);

    // YZ-plot
    ex = eSpxz.getBounds().x - this.x;
    ey = 303;
    eSpyz = new ESensitivityPlot(null,this.x+ex,this.y+ey,-1,-1,ESensitivityPlot.SLICE_YZ,this.mas);
    eSpyz.addEEventListener(plotSelectionListener);
    add(eSpyz);

    // - YZ-plot: Frame
    ey += -KNOB_SIZE/2-KNOB_GAP;
    eh = eSpyz.getBounds().height/2 + KNOB_SIZE/2 + KNOB_GAP;
    eElbo = new EElbo(null,ex-13,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    eElbo = new EElbo(null,ex-13,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SW,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    ex += eSpxz.getBounds().width + KNOB_GAP + KNOB_SIZE;
    eValue = new EValue(null, ex-160, ey, 157, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.EC_ELBOUP, null);
    eValue.setValueMargin(KNOB_SIZE-KNOB_GAP); eValue.setValue("WINDOW>DOOR");
    add(eValue);
    eElbo = new EElbo(null,ex,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NE,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    eElbo = new EElbo(null,ex,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    ey += 2*eh - KNOB_SIZE/2;
    ex -= KNOB_GAP + KNOB_SIZE;
    eValue = new EValue(null, ex+3, ey, KNOB_GAP + KNOB_SIZE-2, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
    eValue.setValueMargin(0); eValue.setValue("YZ");
    add(eValue);

    // - YZ-plot: Cursors, grid and scales    
    gSpyzCursorH = new CCursor(eSpyz,true,false);
    gSpyzCursorH.addScaleTick(250,   "0",true);
    gSpyzCursorH.addScaleTick(150, "100",true);
    gSpyzCursorH.addScaleTick( 50, "200",true);
    gSpyzCursorH.addScaleTick( 30,"z/cm",false);
    gSpyzCursorV = new CCursor(eSpyz,false,false);
    gSpyzCursorV.addScaleTick( 20,"-200",true);
    gSpyzCursorV.addScaleTick(120,"-100",true);
    gSpyzCursorV.addScaleTick(220,   "0",true);
    gSpyzCursorV.addScaleTick(320, "100",true);
    gSpyzCursorV.addScaleTick(390,"y/cm",false);
    gSpyzCursorV.addScaleTick(420, "200",true);
    addAll(gSpyzCursorH);
    addAll(gSpyzCursorV);
    
    // Slices position display
    ex = eSpxz.getBounds().x - this.x;
    ey = eSpyz.getBounds().y - this.y - KNOB_SIZE - KNOB_GAP;
    eXPos = new EValue(null,ex,ey-3,92,KNOB_SIZE+3,LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"X");
    eXPos.setValueMargin(0); eXPos.setValueWidth(63); eXPos.setValue("000");
    eXPos.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        Point3d point = getSelection();
        point.x = 0;
        setSelecion(point);
        fireSelectionChanged(point);
      }
    });
    add(eXPos);
    eYPos = new EValue(null,ex+95,ey-3,92,KNOB_SIZE+3,LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"Y");
    eYPos.setValueMargin(0); eYPos.setValueWidth(63); eYPos.setValue("000");
    eYPos.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        Point3d point = getSelection();
        point.y = 0;
        setSelecion(point);
        fireSelectionChanged(point);
      }
    });
    add(eYPos);
    eZPos = new EValue(null,ex+190,ey-3,92,KNOB_SIZE+3,LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"Z");
    eZPos.setValueMargin(0); eZPos.setValueWidth(63); eZPos.setValue("000");
    eZPos.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        Point3d point = getSelection();
        point.z = 160;
        setSelecion(point);
        fireSelectionChanged(point);
      }
    });
    add(eZPos);
    add(new ERect(null,ex+285,ey-3,39,KNOB_SIZE+3,LCARS.ES_STATIC|LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"cm"));
    
    // Connecting lines
    // - XY <-> YZ plots (layout done by repositionConnectingLines)
    eXyYz = new EElbo(null,0,0,0,0,LCARS.ES_STATIC|LCARS.ES_SHAPE_SW,null);
    eXyYz.setArmWidths(CURSOR_WIDTH,CURSOR_WIDTH);
    eXyYz.setArcWidths(30,27);
    add(eXyYz);
    eXyYzArrow = new EElement(null,0,0,12,12,LCARS.ES_STATIC,null)
    {
      @Override
      protected ArrayList<AGeometry> createGeometriesInt()
      {
        ArrayList<AGeometry> geos = new ArrayList<AGeometry>();

        Rectangle b = getBounds();
        int[] ax = new int[] { b.x, b.x+b.width, b.x+b.width };
        int[] ay = new int[] { b.y+b.height/2, b.y+b.height, b.y };
        geos.add(new GArea(new Area(new Polygon(ax,ay,3)),false));

        return geos;
      }
    };
    add(eXyYzArrow);
    
    // - XY <-> XZ plots
    eXyXz1 = new EElbo(null,0,0,0,0,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
    eXyXz1.setArmWidths(CURSOR_WIDTH,CURSOR_WIDTH);
    eXyXz1.setArcWidths(30,27);
    add(eXyXz1);
    eXyXz2 = new EElbo(null,0,0,0,0,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
    eXyXz2.setArmWidths(CURSOR_WIDTH,CURSOR_WIDTH);
    eXyXz2.setArcWidths(30,27);
    add(eXyXz2);
    eXyXzArrow = new EElement(null,0,0,12,12,LCARS.ES_STATIC,null)
    {
      @Override
      protected ArrayList<AGeometry> createGeometriesInt()
      {
        ArrayList<AGeometry> geos = new ArrayList<AGeometry>();

        Rectangle b = getBounds();
        int[] ax = new int[] { b.x+b.width/2, b.x+b.width, b.x };
        int[] ay = new int[] { b.y, b.y+b.height, b.y+b.height };
        geos.add(new GArea(new Area(new Polygon(ax,ay,3)),false));

        return geos;
      }
    };
    add(eXyXzArrow);
    
    // Sensitivity scale
    Rectangle bSpxy = eSpxy.getBounds();
    Rectangle bSpyz = eSpyz.getBounds();
    gSensScale = new CSensitivityScale(bSpxy.x-this.x,bSpyz.y-this.y+bSpyz.height-40,bSpxy.width,40);
    addAll(gSensScale);

    // Frequency slider
    ex = eSpxz.getBounds().x + eSpxz.getBounds().width - this.x + 66;
    eh = eSpyz.getBounds().y + eSpyz.getBounds().height - eSpxz.getBounds().y - KNOB_SIZE/2;
    eFreqSlider = new ECslSlider(ex,0,2*KNOB_SIZE,eh,ECslSlider.ES_LOGARITHMIC,10);
    eFreqSlider.eKnob.setColorStyle(LCARS.EC_SECONDARY);
    eFreqSlider.setMinMaxValue(10,10000);
    float[] ticks = new float[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
    for (float dec = 10; dec<9999; dec*=10)
      for (float tick : ticks)
      {
        if (dec>99 && tick>6.9)
          break;
        
        float value = dec*tick;
        String label = null;
        if (tick==1 || tick==2 || tick==5 )
          label = String.format("%.0f",value);
        eFreqSlider.addScaleTick((float)Math.round(value),label,LCARS.EF_TINY);
      }
    eFreqSlider.addScaleLabel(25,"f/Hz",LCARS.EF_TINY).eLabel.setAlpha(0.75f);;
    eFreqSlider.setValue(1000); eFreqSlider.eKnob.setLabel("1000");
    eFreqSlider.addSelectionListener(new ECslSlider.SelectionListener()
    {
      @Override
      public void selectionChanged(float value)
      {
        setFrequency(value);
      }
    });
    eFreqSlider.forAllElements((el)->{ add(el); });

    // - Frequency slider: Frame
    ex = eFreqSlider.eBack.getBounds().x - this.x;
    ey = -KNOB_SIZE-2*KNOB_GAP;
    ew = eFreqSlider.eBack.getBounds().width - 20;
    eFreq = new EValue(null, ex-3, ey, ew, KNOB_SIZE/2, LCARS.EF_SMALL|LCARS.ES_SELECTED, null);
    eFreq.setValueMargin(0); eFreq.setValueWidth(ew);
    eFreq.setValue("1000");
    eFreq.addEEventListener(new EEventListenerAdapter()
    {     
      @Override
      public void touchDown(EEvent ee)
      {
        try
        {
          EElement el = (EElement)ee.el.getData();
          el.setSelected(!el.isSelected());
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        setFrequency(1000);
      }

      @Override
      public void touchUp(EEvent ee)
      {
        try
        {
          EElement el = (EElement)ee.el.getData();
          el.setSelected(!el.isSelected());
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });
    add(eFreq);
    ERect eRect = new ERect(null, ex+ew-4, ey, 24, KNOB_SIZE/2, LCARS.ES_SELECTED|LCARS.ES_LABEL_C|LCARS.EF_SMALL, "Hz");
    eRect.addEEventListener(new EEventListenerAdapter()
    {     
      @Override
      public void touchDown(EEvent ee)
      {
        eFreq.setSelected(!eFreq.isSelected());
        setFrequency(1000);
      }

      @Override
      public void touchUp(EEvent ee)
      {
        eFreq.setSelected(!eFreq.isSelected());
      }
    });
    eFreq.setData(eRect);
    add(eRect);
    
    ex = eFreqSlider.eBack.getBounds().x + eFreqSlider.eBack.getBounds().width - this.x + 3;
    ew = eFreqSlider.eBack.getBounds().width/8 + KNOB_GAP + 10;
    eh = eFreqSlider.eBack.getBounds().height/2 - ey;
    eElbo = new EElbo(null,ex,ey,ew,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NE|LCARS.ES_LABEL_NW,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    ey += eh;
    eh = eFreqSlider.eBack.getBounds().y - this.y + eFreqSlider.eBack.getBounds().height - ey + KNOB_SIZE + KNOB_GAP;
    eElbo = new EElbo(null,ex,ey,ew,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
    eElbo.setArmWidths(6,KNOB_SIZE/2);
    add(eElbo);
    ex -= eFreqSlider.eKnob.getBounds().width;
    ey += eh - KNOB_SIZE/2;
    eValue = new EValue(null, ex, ey, eFreqSlider.eKnob.getBounds().width, KNOB_SIZE/2, LCARS.ES_STATIC, null);
    eValue.setValueMargin(0); eValue.setValueWidth(eFreqSlider.eKnob.getBounds().width);
    eValue.setValue("FREQUENCY");
    add(eValue);
    
    // Main frame
    ex = -KNOB_SIZE*7/8-KNOB_GAP;
    ey = -KNOB_SIZE-2*KNOB_GAP;
    eElbo = new EElbo(null, ex, ey, KNOB_SIZE*7/8, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_SHAPE_NW, null);
    eElbo.setArmWidths(KNOB_SIZE, KNOB_SIZE/2);
    eElbo.setArcWidths(KNOB_SIZE, 0);
    add(eElbo);
    ex = -KNOB_GAP;
    ew = eSpyz.getBounds().x + eSpyz.getBounds().width - eSpxy.getBounds().x -200;
    eValue = new EValue(null, 0, ey, ew, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_VALUE_W, null);
    eValue.setValueMargin(0); eValue.setValue("CEILING>FLOOR");
    add(eValue);
    ex += ew+3;
    ew = eFreqSlider.eBack.getBounds().x - this.x - ex -3; 
    eValue = new EValue(null,ex, ey, ew, KNOB_SIZE/2, LCARS.EC_HEADLINE|LCARS.ES_STATIC|LCARS.ES_VALUE_W, null);
    eValue.setValueMargin(0); eValue.setValue("MICARR SPATIAL SENSITIVITY");
    add(eValue);

    // Initialization
    gSpxyCursorH.setAlterEgo(gSpyzCursorV); gSpyzCursorV.setAlterEgo(gSpxyCursorH);
    gSpxyCursorV.setAlterEgo(gSpxzCursorV); gSpxzCursorV.setAlterEgo(gSpxyCursorV);
    gSpyzCursorH.setAlterEgo(gSpxzCursorH); gSpxzCursorH.setAlterEgo(gSpyzCursorH);
   
    setSelecion(CSL.ROOM.DEFAULT_POS);
  }
  
  // -- Getters and setters --
  
  /**
   * Sets the microphone array state to display.
   * 
   * @param state
   *          The state. If <code>null</code>, a dummy state will be displayed.
   */
  public void setMicArrayState(MicArrayState state)
  {
    if (state==null)
      state = MicArrayState.getDummy();
    if (state.equals(this.mas))
      return;
    this.mas = state;
    eSpxy.setMicArrayState(this.mas);
    eSpyz.setMicArrayState(this.mas);
    eSpxz.setMicArrayState(this.mas);
    
    updateInt();
  }
  
  /**
   * Returns the microphone array state currently displayed. Note that this is
   * not necessarily the current state of the hardware.
   */
  public MicArrayState getMicArrayState()
  {
    return this.mas;
  }
  
  /**
   * Repositions the 2D sensitivity plot slices (CSL room coordinates in cm).
   * 
   * @param point
   *          The new slice positions.
   */
  public void setSelecion(Point3d point)
  {
    point.x = Math.max(CSL.ROOM.MIN_X,Math.min(CSL.ROOM.MAX_X,point.x));
    point.y = Math.max(CSL.ROOM.MIN_Y,Math.min(CSL.ROOM.MAX_Y,point.y));
    point.z = Math.max(CSL.ROOM.MIN_Z,Math.min(CSL.ROOM.MAX_Z,point.z));
   
    eXPos.setValue(String.format("% 04.0f",point.x));
    eYPos.setValue(String.format("% 04.0f",point.y));
    eZPos.setValue(String.format("% 04.0f",point.z));
    
    eSpxy.setSlicePos(point.z);
    gSpxyCursorH.setSelection(eSpxy.cslToElement(point).y,point.y);
    gSpxyCursorV.setSelection(eSpxy.cslToElement(point).x,point.x);

    eSpyz.setSlicePos(point.x);
    gSpyzCursorH.setSelection(eSpyz.cslToElement(point).y,point.z);
    gSpyzCursorV.setSelection(eSpyz.cslToElement(point).x,point.y);
    
    eSpxz.setSlicePos(point.y);
    gSpxzCursorH.setSelection(eSpxz.cslToElement(point).y,point.z);
    gSpxzCursorV.setSelection(eSpxz.cslToElement(point).x,point.x);
    
    updateInt();
  }

  /**
   * Retrieves the current 2D sensitivity plot slice positions.
   * 
   * @return
   *   The positions (CSL room coordinates in cm).
   */
  public Point3d getSelection()
  {
    int x = gSpxyCursorV.getSelection();
    int y = gSpxyCursorH.getSelection();
    return eSpxy.elementToCsl(new Point(x,y));
  }
  
  /**
   * Sets the frequency for which the spatial sensitivity is to be plotted.
   * 
   * @param freq
   *          The frequency in Hz, must be positive.
   */
  public void setFrequency(float freq)
  {
    eSpxy.setFrequency(freq);
    eSpyz.setFrequency(freq);
    eSpxz.setFrequency(freq);
    String label = String.format("%.0f",freq);
    eFreqSlider.setValue(freq);
    eFreqSlider.eKnob.setLabel(label);
    eFreq.setValue(label);
  }
  
  /**
   * Returns the frequency for the spatial sensitivity is plotted. 
   */
  public float getFrequency()
  {
    return eSpxy.getFrequency();
  }
  
  // -- Listener implementation --
  
  /**
   * Interface for selection change listeners of {@link ESensitivityPlots}.
   */
  public interface SelectionListener
  {

    /**
     * Called when the selection, i.e. the plot slice positions, has changed.
     * 
     * @param point
     *          The new plot slice position in cm (CSL room coordinates).
     */
    public void selectionChanged(Point3d point);

  }

  private ArrayList<SelectionListener> listeners;
  
  public void addSelectionListener(SelectionListener listener)
  {
    if (listener==null)
      return;
    if (listeners==null)
      listeners = new ArrayList<SelectionListener>();
    if (listeners.contains(listener))
      return;
    listeners.add(listener);
  }
  
  public void removeListener(SelectionListener listener)
  {
    if (listener==null || listeners==null)
      return;
    listeners.remove(listener);
  }
  
  public void removeAllListeners()
  {
    listeners.clear();
  }
  
  protected void fireSelectionChanged(Point3d point)
  {
    if (listeners==null)
      return;
    for (SelectionListener listener : listeners)
      listener.selectionChanged(point);
  }
  
  // -- Workers and event handlers --
  
  protected void cursorDragged(CCursor cur, int pos)
  {
    Point3d point = new Point3d();

    if (cur==gSpxyCursorH)
      point = eSpxy.elementToCsl(new Point(gSpxyCursorV.getSelection(),pos));
    else if (cur==gSpxyCursorV)
      point = eSpxy.elementToCsl(new Point(pos,gSpxyCursorH.getSelection()));
    else if (cur==gSpyzCursorH)
      point = eSpyz.elementToCsl(new Point(gSpyzCursorV.getSelection(),pos));
    else if (cur==gSpyzCursorV)
      point = eSpyz.elementToCsl(new Point(pos,gSpyzCursorH.getSelection()));
    else if (cur==gSpxzCursorH)
      point = eSpxz.elementToCsl(new Point(gSpxzCursorV.getSelection(),pos));
    else if (cur==gSpxzCursorV)
      point = eSpxz.elementToCsl(new Point(pos,gSpxzCursorH.getSelection()));
    
    setSelecion(point);
    fireSelectionChanged(point);
  }
  
  protected void updateInt()
  {
    // Reposition connection lines between XY and XZ/YZ plots
    repositionConnectingLines();
    
    // Set value on sensitivity scale
    float   freq  = getFrequency();
    Point3d point = getSelection();
    float   db    = CpuSensitivityRenderer.getDB(this.mas,freq,point.x,point.y,point.z);
    gSensScale.setSelection(db);
  }
  
  protected void repositionConnectingLines()
  {
    Rectangle b1;
    Rectangle b2;
    Rectangle b = new Rectangle();
    Point     p1;
    Point     p2;
    Point     pm;

    // Reposition line from XY to YZ plot
    b1 = eSpxy.getBounds();
    b2 = eSpyz.getBounds();
    p1 = new Point(b1.x+gSpxyCursorV.getSelection()-CURSOR_WIDTH/2,b1.y+b1.height);
    p2 = new Point(b2.x-13,p1.y+30);
    eXyYz.setBounds(new Rectangle(p1.x,p1.y,p2.x-p1.x,p2.y-p1.y));
    
    b = eXyYzArrow.getBounds();
    b.x = p1.x+CURSOR_WIDTH+KNOB_GAP;
    b.y = p1.y+KNOB_GAP;
    eXyYzArrow.setBounds(b);
    
    // Reposition line from XY to XZ plot
    b2 = eSpxz.getBounds();
    p1 = new Point(b1.x+b1.width,b1.y+gSpxyCursorH.getSelection()-CURSOR_WIDTH/2);
    p2 = new Point(b2.x-13,b2.y+b2.height/2-CURSOR_WIDTH/2);
    pm = new Point(p1.x+(p2.x-p1.x)/2,p1.y+(p2.y-p1.y)/2);
    b1 = new Rectangle(p1.x,p1.y,pm.x-p1.x+CURSOR_WIDTH/2,pm.y-p1.y);
    int s1 = LCARS.ES_SHAPE_NE;
    if (b1.height<0)
    {
      b1 = new Rectangle(b1.x,b1.y+b1.height+CURSOR_WIDTH/2-1,b1.width,-b1.height+CURSOR_WIDTH);
      s1 = LCARS.ES_SHAPE_SE;
    }
    int s2 = LCARS.ES_SHAPE_SW;
    b2 = new Rectangle(b1.x+b1.width-CURSOR_WIDTH,pm.y,p2.x-pm.x+CURSOR_WIDTH,p2.y-pm.y);
    if (b2.height<0)
    {
      b2 = new Rectangle(b2.x,b2.y+b2.height,b2.width,-b2.height);
      s2 = LCARS.ES_SHAPE_NW;
    }
    b1.height = Math.max(b1.height,CURSOR_WIDTH);
    b2.height = Math.max(b2.height,CURSOR_WIDTH);
    eXyXz1.setBounds(b1); eXyXz1.setStyle(s1); 
    eXyXz1.setArcWidths(Math.min(b1.height,30),Math.min(b1.height-3,27));
    eXyXz2.setBounds(b2); eXyXz2.setStyle(s2);
    eXyXz2.setArcWidths(Math.min(b2.height,30),Math.min(b2.height-3,27));

    b = eXyXzArrow.getBounds();
    b.x = p1.x+KNOB_GAP;
    b.y = p1.y+CURSOR_WIDTH+KNOB_GAP;
    eXyXzArrow.setBounds(b);
  }
  
  // -- Nested classes --
  
  private class CCursor extends ArrayList<EElement>
  {
    private static final long serialVersionUID = 1L;
    
    protected final ESensitivityPlot sp;
    protected final boolean          horiz;
    protected final boolean          knobLeftOrTop;
    protected final ERect            eSclBk;
    protected final ERect            eLine;
    protected final ERect            eKnob;
    protected       CCursor          alterEgo;
   
    protected CCursor(ESensitivityPlot sp, boolean horizonal, boolean knobLeftOrTop)
    { 
      this.sp = sp;
      this.horiz = horizonal;
      this.knobLeftOrTop = knobLeftOrTop;
      
      // Drag listener
      EEventListenerAdapter dragListener = new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          setHighlighted(true);
          if (alterEgo!=null)
            alterEgo.setHighlighted(true);
          int pos;
          if (ee.el!=eSclBk)
            return;
          if (horiz)
            pos = ee.el.getBounds().y-sp.getBounds().y+ee.pt.y;
          else
            pos = ee.el.getBounds().x-sp.getBounds().x+ee.pt.x;
          ESensitivityPlots.this.cursorDragged(CCursor.this,pos);
        }

        @Override
        public void touchUp(EEvent ee)
        {
          setHighlighted(false);
          if (alterEgo!=null)
            alterEgo.setHighlighted(false);
          int pos;
          if (horiz)
            pos = ee.el.getBounds().y-sp.getBounds().y+ee.pt.y;
          else
            pos = ee.el.getBounds().x-sp.getBounds().x+ee.pt.x;
          ESensitivityPlots.this.cursorDragged(CCursor.this,pos);
        }
        
        @Override
        public void touchDrag(EEvent ee)
        {
          int pos;
          if (horiz)
            pos = ee.el.getBounds().y-sp.getBounds().y+ee.pt.y;
          else
            pos = ee.el.getBounds().x-sp.getBounds().x+ee.pt.x;
          ESensitivityPlots.this.cursorDragged(CCursor.this,pos);
        }
      }; 
      
      // Add scale background
      Rectangle b = getInitialScaleBounds();
      eSclBk = new ERect(null,b.x,b.y,b.width,b.height,LCARS.EB_OVERDRAG,null);
      eSclBk.addEEventListener(dragListener);

      eSclBk.setAlpha(0.2f);
      add(eSclBk);

      // Add cursor line
      b = getInitialCursorLineBounds();
      eLine = new ERect(null,b.x,b.y,b.width,b.height,LCARS.ES_STATIC|LCARS.EC_SECONDARY,null);
      add(eLine);
      
      // Add cursor knob
      if (knobLeftOrTop)
        if (horiz) { b.x-=KNOB_SIZE; b.y-=KNOB_SIZE/4; }
        else      { b.x-=KNOB_SIZE/2; b.y-=KNOB_SIZE/2; }
      else
        if (horiz) { b.x+=b.width; b.y-=KNOB_SIZE/4; }
        else      { b.x-=KNOB_SIZE/2; b.y+=b.height; }
      
      int style = LCARS.ES_RECT_RND|LCARS.EB_OVERDRAG|LCARS.ES_LABEL_C|LCARS.EF_SMALL|LCARS.EC_SECONDARY;
      eKnob = new ERect(null,b.x,b.y,KNOB_SIZE,KNOB_SIZE/2,style,"000");
      eKnob.addEEventListener(dragListener);
      add(eKnob);
      
      // Initialize
      setHighlighted(false);
    }
    
    protected void addScaleTick(int pos, String label, boolean gridLine)
    {
      Rectangle b = getInitialCursorLineBounds();
      if (horiz)
      {
        b.y = sp.getBounds().y - ESensitivityPlots.this.y + pos;
        b.height = 1;
        b.width += KNOB_SIZE*7/8;
        if (knobLeftOrTop)
          b.x -= KNOB_SIZE*7/8;
      }
      else
      {
        b.x = sp.getBounds().x - ESensitivityPlots.this.x + pos;
        b.width = 1;
        b.height += KNOB_SIZE/2;
        if (knobLeftOrTop)
          b.y -= KNOB_SIZE/2;
      }
      
      if (gridLine)
      {
        ERect eGridLine = new ERect(null,b.x,b.y,b.width,b.height,LCARS.ES_STATIC,null); 
        eGridLine.setAlpha(0.3f);
        add(eGridLine);
      }
      
      int s = LCARS.ES_STATIC|LCARS.EF_TINY;
      if (knobLeftOrTop)
        if (horiz) { b.x-=3; b.y-=KNOB_SIZE/2-3; s |= LCARS.ES_LABEL_SE; }
        else      { b.x+=3; s |= LCARS.ES_LABEL_W; }
      else
        if (horiz) { b.y-=KNOB_SIZE/2-3; b.x += b.width-KNOB_SIZE*3/4-3; s |= LCARS.ES_LABEL_SE; }
        else      { b.x+=3; b.y += b.height-KNOB_SIZE/2; s |= LCARS.ES_LABEL_W; }
      
      ELabel eLabel = new ELabel(null,b.x,b.y,KNOB_SIZE*3/4,KNOB_SIZE/2,s,label);
      eLabel.setAlpha(0.5f);
      add(eLabel);
    
    }
    
    protected void setAlterEgo(CCursor alterEgo)
    {
      this.alterEgo = alterEgo;
    }
    
    protected void setSelection(int pos, double value)
    {
      // Move cursor line
      Rectangle b = eLine.getBounds();
      if (horiz)
        b.y = sp.getBounds().y+pos-b.height/2;
      else
        b.x = sp.getBounds().x+pos-b.width/2;
      eLine.setBounds(b);

      // Move cursor value
      b = eKnob.getBounds();
      if (horiz)
        b.y = sp.getBounds().y+pos-b.height/2;
      else
        b.x = sp.getBounds().x+pos-b.width/2;
      eKnob.setBounds(b);
      eKnob.setLabel(String.format("%.0f",value));
    }
    
    protected int getSelection()
    {
      Rectangle b = eKnob.getBounds();
      if (horiz)
        return b.y - sp.getBounds().y + b.height/2;
      else
        return b.x - sp.getBounds().x + b.width/2;
    }
    
    protected void setHighlighted(boolean highlight)
    {
      int ec = highlight?LCARS.EC_PRIMARY:LCARS.EC_SECONDARY;
      eLine.setColorStyle(ec);
      eLine.setSelected(highlight);
      eLine.setAlpha(highlight?1f:0.5f);
      eKnob.setColorStyle(ec);
      eKnob.setSelected(highlight);
    }
  
    private Rectangle getInitialCursorLineBounds()
    {
      Rectangle b = sp.getBounds(); 
      int x = b.x+(horiz?0:b.width/2-CURSOR_WIDTH/2) - ESensitivityPlots.this.x;
      int y = b.y+(horiz?b.height/2-CURSOR_WIDTH/2:0) - ESensitivityPlots.this.y;
      int w = horiz?b.width:CURSOR_WIDTH;
      int h = horiz?CURSOR_WIDTH:b.height;
      
      if (knobLeftOrTop)
      {
        if (horiz) { x-=KNOB_GAP; w+=KNOB_GAP; }
        else      { y-=KNOB_GAP; h+=KNOB_GAP; }
      }
      else
      {
        if (horiz) { w+=KNOB_GAP; }
        else      { h+=KNOB_GAP; }
      }

      return new Rectangle(x,y,w,h);
    }
    
    private Rectangle getInitialScaleBounds()
    {
      Rectangle b = sp.getBounds(); 
      int x = b.x - ESensitivityPlots.this.x;
      int y = b.y - ESensitivityPlots.this.y;
      int w = horiz?KNOB_SIZE*3/4:b.width;
      int h = horiz?b.height:KNOB_SIZE/2;
      
      if (knobLeftOrTop)
      {
        if (horiz) { x-=KNOB_GAP+KNOB_SIZE*7/8; }
        else      { y-=KNOB_GAP+KNOB_SIZE/2; }
      }
      else
      {
        if (horiz) { x+=b.width+KNOB_GAP+KNOB_SIZE/8; }
        else      { y+=b.height+KNOB_GAP; }
      }

      return new Rectangle(x,y,w,h);
    }
  }

  private class CSensitivityScale extends ArrayList<EElement>
  {
    private static final long serialVersionUID = 1L;

    private final int      x;
    private final int      y;
    private final int      w;
    private final int      h;
    private final EElement eImage;
    private final ERect    eLine;
    private final ERect    eKnob;
    
    private static final float MIN_DB = -36;
    private static final float MAX_DB = 0;
    
    protected CSensitivityScale(int x, int y, int w, int h)
    {
      this.x = x;
      this.y = y;
      this.h = h;
      this.w = w;

      boolean alternativeLayout = false;
      
      ERect  eRect;
      EElbo  eElbo;
      EValue eValue;
      ELabel eLabel;
      int    ex;
      int    ey;
      int    eh;
      
      // Frame
      if (!alternativeLayout)
      {
        ex = x-KNOB_GAP-KNOB_SIZE*7/8;
        ey = y-70;
        eh = y-ey-KNOB_GAP;
        eElbo = new EElbo(null,ex,ey,411,eh,LCARS.ES_SHAPE_SW,null);
        eElbo.setArmWidths(KNOB_SIZE*3/4,KNOB_SIZE/2);
        eElbo.setArcWidths(Math.round(KNOB_SIZE*1.5f),KNOB_SIZE/2);
        add(eElbo);
      }
      ey = y-KNOB_SIZE/2-KNOB_GAP;
      eh = h/2+KNOB_SIZE/2+KNOB_GAP;
      if (alternativeLayout)
      {
        eElbo = new EElbo(null,x-10,ey,10,eh,LCARS.ES_SHAPE_NW,null);
        eElbo.setArmWidths(4,KNOB_SIZE/2);
        add(eElbo);
      }
      eElbo = new EElbo(null,x+w+KNOB_GAP,ey,10,eh,LCARS.ES_SHAPE_NE,null);
      eElbo.setArmWidths(4,KNOB_SIZE/2);
      add(eElbo);
      eValue = new EValue(null,x+w-120+KNOB_GAP,ey-1,120,KNOB_SIZE/2,LCARS.ES_STATIC,null);
      eValue.setValueMargin(0); eValue.setValueWidth(eValue.getBounds().width);
      eValue.setValue("SENSITIVTY");
      add(eValue);
      ey += h+KNOB_GAP;
      if (alternativeLayout)
      {
        eElbo = new EElbo(null,x-10,ey,10,eh,LCARS.ES_SHAPE_SW,null);
        eElbo.setArmWidths(4,KNOB_SIZE/2);
        add(eElbo);
      }
      eElbo = new EElbo(null,x+w+KNOB_GAP,ey,10,eh,LCARS.ES_SHAPE_SE,null);
      eElbo.setArmWidths(4,KNOB_SIZE/2);
      add(eElbo);
      
      // Scale background
      eRect = new ERect(null,x,y+h+KNOB_GAP,w,KNOB_SIZE/2,LCARS.ES_NONE,null);
      eRect.setAlpha(0.2f);
      add(eRect);
      
      // Scale image
      this.eImage = new EElement(null,x,y,w,h,LCARS.ES_NONE,null)
      {
        @Override
        protected ArrayList<AGeometry> createGeometriesInt()
        {
          ArrayList<AGeometry> geos = new ArrayList<AGeometry>();
          Rectangle b = getBounds();
          geos.add(new GSensitivityScale(b.x,b.y,b.width,b.height));    
          return geos;
        }
      };
      add(eImage);
      
      // Scale ticks and unit
      addScaleTick(-36);
      addScaleTick(-30);
      addScaleTick(-24);
      addScaleTick(-18);
      addScaleTick(-12);
      addScaleTick(-6);
      ex = x+w-50-KNOB_GAP;
      ey = y+h+KNOB_GAP;
      eLabel = new ELabel(null,ex,ey,50,KNOB_SIZE/2,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.EF_TINY,"dB");
      eLabel.setAlpha(0.5f);
      add(eLabel);
      
      // Cursor
      ex = x+dbToPos(-18)-CURSOR_WIDTH/2;
      eLine = new ERect(null,ex,y,CURSOR_WIDTH,h+KNOB_GAP,LCARS.ES_STATIC,null);
      eLine.setAlpha(0.7f);
      add(eLine);
      ex -= KNOB_SIZE/2-CURSOR_WIDTH/2;
      eKnob = new ERect(null,ex,ey,KNOB_SIZE,KNOB_SIZE/2,LCARS.ES_STATIC|LCARS.ES_LABEL_C|LCARS.EF_SMALL|LCARS.ES_RECT_RND,"-18");
      add(eKnob);
      
      // Initialize
      setSelection(0);
    }
    
    protected void setSelection(float db)
    {
      db = Math.round(Math.max(-36,Math.min(0,db)));
      Rectangle b = eKnob.getBounds();
      b.x = eImage.getBounds().x+dbToPos(db)-KNOB_SIZE/2;
      eKnob.setBounds(b);
      eKnob.setLabel(String.format("%.0f",db));
      b = eLine.getBounds();
      b.x = eImage.getBounds().x+dbToPos(db)-CURSOR_WIDTH/2;
      eLine.setBounds(b);
    }
    
    @SuppressWarnings("unused")
    protected float getSelection()
    {
      int x0 = eImage.getBounds().x;
      int x1 = eLine.getBounds().x+CURSOR_WIDTH/2;
      return posToDb(x1-x0);
    }
    
    private void addScaleTick(float db)
    {
      int ex = this.x + dbToPos(db);
      ERect eRect = new ERect(null,ex,y,1,this.h+KNOB_SIZE/2+KNOB_GAP,LCARS.ES_NONE,null);
      eRect.setAlpha(0.3f);
      add(eRect);
      
      ex += KNOB_GAP;
      int ey = this.y+this.h+KNOB_GAP;
      String s = String.format("%.0f",db);
      ELabel eLabel = new ELabel(null,ex,ey,30,KNOB_SIZE/2,LCARS.ES_STATIC|LCARS.ES_LABEL_W|LCARS.EF_TINY,s);
      eLabel.setAlpha(0.5f);
      add(eLabel);
    }
    
    private float posToDb(int pos)
    {
      return MIN_DB + (MAX_DB-MIN_DB)*pos/this.w;
    }
    
    private int dbToPos(float db)
    {
      db = Math.min(0,Math.max(-36,db));
      return Math.round((db-MIN_DB)/(MAX_DB-MIN_DB)*this.w);
    }
  }
  
}

// EOF
