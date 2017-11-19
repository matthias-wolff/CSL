package de.tucottbus.kt.csl.lcars.contributors;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.csl.lcars.elements.ESensitivityPlot;
import de.tucottbus.kt.csl.lcars.geometry.GSensitivityScale;
import de.tucottbus.kt.csl.lcars.geometry.rendering.CpuSensitivityRenderer;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ESlider;
import de.tucottbus.kt.lcars.contributors.ESliderCursor;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListener;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.GArea;
import de.tucottbus.kt.lcars.test.ATestPanel;

/**
 * This class contributes 2D sensitivity plots of CLS's microphone array to an
 * LCARS panel.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Show outlines of microphones array over grids of plots.
 *     </li>
 *   <li>TODO: Add animation (Random trajectory of slice positions attracted to steering focus and repelled from room borders).
 *     </li>
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
  
  private       MicArrayState    mas;
  private final ESensitivityPlot eSpxy;
  private final ECursor          eSpxyCursorH;
  private final ECursor          eSpxyCursorV;
  private final ESensitivityPlot eSpyz;
  private final ECursor          eSpyzCursorH;
  private final ECursor          eSpyzCursorV;
  private final ESensitivityPlot eSpxz;
  private final ECursor          eSpxzCursorH;
  private final ECursor          eSpxzCursorV;
  private final EEventListener   plotSelectionListener;
  private final EValue           eXPos;
  private final EValue           eYPos;
  private final EValue           eZPos;
  private final EElbo            eXyYz;
  private final EElement         eXyYzArrow;
  private final EElbo            eXyXz1;
  private final EElbo            eXyXz2;
  private final EElement         eXyXzArrow;
  private final ESliderCursor    eSensitivitySlider;
  private final EElement         eSensitivityScale;
  private final ESlider          eFreqSlider;
  private final EValue           eFreq;

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
        setSelection(point);
        fireSelectionChanged((listener)->listener.slicePositionsChanged(point));
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
    ex = -KNOB_SIZE*7/8-KNOB_GAP-1;
    ey = -KNOB_GAP-KNOB_SIZE/2;
    eValue = new EValue(null, ex, ey, KNOB_SIZE*7/8+KNOB_GAP, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
    eValue.setValueMargin(0); eValue.setValue("XY");
    add(eValue);
    
    // - XY-plot: Cursors, grid and scales    
    eSpxyCursorH = new ECursor(this,eSpxy,ESliderCursor.ES_VERT_LINE_E);
    eSpxyCursorH.xScale();
    add(eSpxyCursorH,false);
    eSpxyCursorV = new ECursor(this,eSpxy,ESliderCursor.ES_HORIZ_LINE_S);
    eSpxyCursorV.yScale();
    add(eSpxyCursorV,false);

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
    eSpxzCursorH = new ECursor(this,eSpxz,ECursor.ES_VERT_LINE_W);
    eSpxzCursorH.zScale();
    add(eSpxzCursorH,false);
    eSpxzCursorV = new ECursor(this,eSpxz,ESliderCursor.ES_HORIZ_LINE_S);
    eSpxzCursorV.xScale();
    add(eSpxzCursorV,false);
    
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
    eSpyzCursorH = new ECursor(this,eSpyz,ECursor.ES_VERT_LINE_W);
    eSpyzCursorH.zScale();
    add(eSpyzCursorH,false);
    eSpyzCursorV = new ECursor(this,eSpyz,ESliderCursor.ES_HORIZ_LINE_N);
    eSpyzCursorV.yScale();
    add(eSpyzCursorV,false);

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
        setSelection(point);
        fireSelectionChanged((listener)->listener.slicePositionsChanged(point));
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
        setSelection(point);
        fireSelectionChanged((listener)->listener.slicePositionsChanged(point));
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
        setSelection(point);
        fireSelectionChanged((listener)->listener.slicePositionsChanged(point));
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
    ex = bSpxy.x-this.x;
    ey = bSpyz.y-this.y+bSpyz.height-40;
    ew = bSpxy.width;
    eSensitivityScale = new EElement(null,ex,ey,ew,40,LCARS.ES_NONE,null)
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
    add(eSensitivityScale);
    
    // - Sensitivity scale: Frame
    ex = bSpxy.x-this.x-KNOB_GAP-KNOB_SIZE*7/8-1;
    ey = eSpxy.getBounds().y + eSpxy.getBounds().height -this.y +3;
    eh = eSensitivityScale.getBounds().y - this.y -ey -KNOB_GAP;
    eElbo = new EElbo(null,ex,ey,412,eh,LCARS.ES_SHAPE_SW,null);
    eElbo.setArmWidths(KNOB_SIZE*3/4,KNOB_SIZE/2);
    eElbo.setArcWidths(Math.round(KNOB_SIZE*1.5f),KNOB_SIZE/2);
    add(eElbo);
    ex += eElbo.getBounds().width;
    ey += eElbo.getBounds().height - KNOB_SIZE/2;
    ew = eSensitivityScale.getBounds().width - eElbo.getBounds().width + KNOB_GAP+KNOB_SIZE*7/8 +3;
    eValue = new EValue(null,ex,ey-1,ew,KNOB_SIZE/2,LCARS.ES_STATIC,null);
    eValue.setValueMargin(0); eValue.setValueWidth(eValue.getBounds().width);
    eValue.setValue("SENSITIVTY");
    add(eValue);
    ex += eValue.getBounds().width;
    eh = eValue.getBounds().height + KNOB_GAP + eSensitivityScale.getBounds().height/2;
    eElbo = new EElbo(null,ex,ey,10,eh,LCARS.ES_SHAPE_NE,null);
    eElbo.setArmWidths(4,KNOB_SIZE/2);
    add(eElbo);
    ey += eh;
    eh = eSensitivityScale.getBounds().y+eSensitivityScale.getBounds().height+KNOB_SIZE/2+KNOB_GAP-ey-this.y+1;
    eElbo = new EElbo(null,ex,ey,10,eh-1,LCARS.ES_SHAPE_SE,null);
    eElbo.setArmWidths(4,KNOB_SIZE/2);
    add(eElbo);
    
    // Sensitivity scale: Static slider
    ex = bSpxy.x-this.x;
    ey = eSensitivityScale.getBounds().y + eSensitivityScale.getBounds().height + KNOB_GAP - this.y;
    ew = eSensitivityScale.getBounds().width;
    eSensitivitySlider = new ESliderCursor(ex,ey,ew,KNOB_SIZE/2,LCARS.ES_STATIC|ESliderCursor.ES_ROTATE_KNOB|ESliderCursor.ES_HORIZ_LINE_N,0,40+KNOB_GAP,CURSOR_WIDTH);
    eSensitivitySlider.setMinMaxValue(-36f,0f);
    eSensitivitySlider.addScaleTick(-36f,"-36",LCARS.EF_TINY);
    eSensitivitySlider.addScaleTick(-30f,"-30",LCARS.EF_TINY);
    eSensitivitySlider.addScaleTick(-24f,"-24",LCARS.EF_TINY);
    eSensitivitySlider.addScaleTick(-18f,"-18",LCARS.EF_TINY);
    eSensitivitySlider.addScaleTick(-12f,"-12",LCARS.EF_TINY);
    eSensitivitySlider.addScaleTick( -6f, "-6",LCARS.EF_TINY);
    eSensitivitySlider.addScaleLabel(eSensitivitySlider.valueToPos(-1.3f),"dB",LCARS.EF_TINY);
    eSensitivitySlider.eLine.setAlpha(0.5f);
    add(eSensitivitySlider);

    // Frequency slider
    ex = eSpxz.getBounds().x + eSpxz.getBounds().width - this.x + 66;
    eh = eSpyz.getBounds().y + eSpyz.getBounds().height - eSpxz.getBounds().y - KNOB_SIZE/2;
    eFreqSlider = new ESlider(ex,0,2*KNOB_SIZE,eh,ESlider.ES_LOGARITHMIC,10);
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
    eFreqSlider.addSelectionListener(new ESlider.SelectionListener()
    {
      @Override
      public void selectionChanged(float value)
      {
        setFrequency(value);
      }
    });
    add(eFreqSlider);

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
    ex = -KNOB_SIZE*7/8-KNOB_GAP-1;
    ey = -KNOB_SIZE-2*KNOB_GAP;
    eElbo = new EElbo(null, ex, ey, KNOB_SIZE*7/8+1, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_SHAPE_NW, null);
    eElbo.setArmWidths(KNOB_SIZE, KNOB_SIZE/2);
    eElbo.setArcWidths(KNOB_SIZE, 0);
    add(eElbo);
    ex = -3;
    ew = eSpyz.getBounds().x + eSpyz.getBounds().width - eSpxy.getBounds().x -200;
    eValue = new EValue(null, ex, ey, ew, KNOB_SIZE/2, LCARS.ES_STATIC|LCARS.ES_VALUE_W, null);
    eValue.setValueMargin(0); eValue.setValue("CEILING>FLOOR");
    add(eValue);
    ex += ew+3;
    ew = eFreqSlider.eBack.getBounds().x - this.x - ex -3; 
    eValue = new EValue(null,ex, ey, ew, KNOB_SIZE/2, LCARS.EC_HEADLINE|LCARS.ES_STATIC|LCARS.ES_VALUE_W, null);
    eValue.setValueMargin(0); eValue.setValue("MICARR SPATIAL SENSITIVITY");
    add(eValue);

    // Initialization
    eSpxyCursorH.setAlterEgo(eSpyzCursorV); eSpyzCursorV.setAlterEgo(eSpxyCursorH);
    eSpxyCursorV.setAlterEgo(eSpxzCursorV); eSpxzCursorV.setAlterEgo(eSpxyCursorV);
    eSpyzCursorH.setAlterEgo(eSpxzCursorH); eSpxzCursorH.setAlterEgo(eSpyzCursorH);
   
    setSelection(CSL.ROOM.DEFAULT_POS);
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
  public void setSelection(Point3d point)
  {
    point.x = Math.max(CSL.ROOM.MIN_X,Math.min(CSL.ROOM.MAX_X,point.x));
    point.y = Math.max(CSL.ROOM.MIN_Y,Math.min(CSL.ROOM.MAX_Y,point.y));
    point.z = Math.max(CSL.ROOM.MIN_Z,Math.min(CSL.ROOM.MAX_Z,point.z));
   
    eXPos.setValue(String.format("% 04.0f",(float)Math.round(point.x)));
    eYPos.setValue(String.format("% 04.0f",(float)Math.round(point.y)));
    eZPos.setValue(String.format("% 04.0f",(float)Math.round(point.z)));
    
    eSpxy.setSlicePos(point.z);
    eSpxyCursorV.setValue((float)point.x);
    eSpxyCursorH.setValue((float)point.y);

    eSpyz.setSlicePos(point.x);
    eSpyzCursorV.setValue((float)point.y);
    eSpyzCursorH.setValue((float)point.z);
    
    eSpxz.setSlicePos(point.y);
    eSpxzCursorV.setValue((float)point.x);
    eSpxzCursorH.setValue((float)point.z);
    
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
    float x = eSpxyCursorV.getValue();
    float y = eSpxyCursorH.getValue();
    float z = eSpxzCursorH.getValue();
    return new Point3d(x,y,z);
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

  /**
   * Sets this contributor disabled or enabled.
   * 
   * @param disabled
   *         The new disabled state.
   */
  public void setDisabled(boolean disabled)
  {
    forAllElements((el)-> { el.setDisabled(disabled); });
  }
  
  /**
   * Determines if this contributor is disabled or enabled.
   */
  public boolean isDisabled()
  {
    return eSpxy.isDisabled();
  }
  
  // -- Listener implementation --
  
  /**
   * Interface for selection change listeners of {@link ESensitivityPlots}.
   */
  public interface SelectionListener
  {

    /**
     * Called when the selected plot slice positions have changed.
     * 
     * @param point
     *          The new plot slice position in cm (CSL room coordinates).
     */
    public default void slicePositionsChanged(Point3d point)
    {
    }

    /**
     * Called when the selected frequency has changed.
     * 
     * @param freq
     *          The new frequency in Hz.
     */
    public default void frequencyChanged(float freq)
    {
    }

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
  
  protected void fireSelectionChanged(Consumer<SelectionListener> action)
  {
    if (listeners==null)
      return;
    for (SelectionListener listener : listeners)
      action.accept(listener);
  }
  
  // -- Workers and event handlers --
  
  private void cursorDragged(ECursor cur, float pos)
  {
    float x = eSpxyCursorV.getValue();
    float y = eSpxyCursorH.getValue();
    float z = eSpxzCursorH.getValue();
    Point3d point = new Point3d(x,y,z);

    if (cur==eSpxyCursorH)
      point.y = pos;
    else if (cur==eSpxyCursorV)
      point.x = pos;
    else if (cur==eSpyzCursorH)
      point.z = pos;
    else if (cur==eSpyzCursorV)
      point.y = pos;
    else if (cur==eSpxzCursorH)
      point.z = pos;
    else if (cur==eSpxzCursorV)
      point.x = pos;
    
    setSelection(point);
    fireSelectionChanged((listener)->listener.slicePositionsChanged(point));
  }
  
  protected void updateInt()
  {
    // Reposition connection lines between XY and XZ/YZ plots
    repositionConnectingLines();
    
    // Set value on sensitivity scale
    float   freq  = getFrequency();
    Point3d point = getSelection();
    float   db    = CpuSensitivityRenderer.getDB(this.mas,freq,point.x,point.y,point.z);
    eSensitivitySlider.setValue(db);
    eSensitivitySlider.eKnob.setLabel(String.format("%.0f",(float)Math.round(db)));
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
    p1 = new Point(b1.x+eSpxyCursorV.valueToPos(eSpxyCursorV.getValue())-CURSOR_WIDTH/2,b1.y+b1.height);
    p2 = new Point(b2.x-13,p1.y+30);
    eXyYz.setBounds(new Rectangle(p1.x,p1.y,p2.x-p1.x,p2.y-p1.y));
    
    b = eXyYzArrow.getBounds();
    b.x = p1.x+CURSOR_WIDTH+KNOB_GAP;
    b.y = p1.y+KNOB_GAP;
    eXyYzArrow.setBounds(b);
    
    // Reposition line from XY to XZ plot
    b2 = eSpxz.getBounds();
    p1 = new Point(b1.x+b1.width,b1.y+eSpxyCursorH.valueToPos(eSpxyCursorH.getValue())-CURSOR_WIDTH/2);
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
  
  private static class ECursor extends ESliderCursor
  {
    private static enum CA { STYLE, X, Y, W, H, CL};

    protected ECursor eAlterEgo;
    
    protected ECursor(ESensitivityPlots sps, ESensitivityPlot sp, int style)
    {
      super
      (
        getConstrArg(CA.X,sp,style),
        getConstrArg(CA.Y,sp,style),
        getConstrArg(CA.W,sp,style),
        getConstrArg(CA.H,sp,style),
        getConstrArg(CA.STYLE,sp,style),
        KNOB_GAP,
        getConstrArg(CA.CL,sp,style),
        CURSOR_WIDTH
      );
      
      addSelectionListener((value)->
      {
        sps.cursorDragged(this,value);
      });
      
      EEventListener touchListener = new EEventListenerAdapter()
      {
        @Override
        public void touchUp(EEvent ee)
        {
          setHighlighted(false);
          if (eAlterEgo!=null)
            eAlterEgo.setHighlighted(false);
        }
        
        @Override
        public void touchDown(EEvent ee)
        {
          setHighlighted(true);
          if (ee.el==eKnob)
            ee.el.setSelected(false);
          if (eAlterEgo!=null)
            eAlterEgo.setHighlighted(true);
        }
      };
      eSens.addEEventListener(touchListener);
      eKnob.addEEventListener(touchListener);
      eKnob.setColorStyle(LCARS.EC_SECONDARY);
      eLine.setColorStyle(LCARS.EC_SECONDARY);
      eLine.setAlpha(0.5f);
    }
    
    protected void xScale()
    {
      setMinMaxValue(CSL.ROOM.MIN_X,CSL.ROOM.MAX_X);
      addScaleTick(-200f,"-200",LCARS.EF_TINY);
      addScaleTick(-100f,"-100",LCARS.EF_TINY);
      ScaleTick tick = addScaleTick(0f,"0",LCARS.EF_TINY);
      tick.eLine.setColorStyle(LCARS.EC_SECONDARY);
      tick.eLine.setAlpha(0.6f);
      tick.eLabel.setColorStyle(LCARS.EC_SECONDARY);
      addScaleTick(100f,"100",LCARS.EF_TINY);
      addScaleTick(200f,"200",LCARS.EF_TINY);
      addScaleLabel(valueToPos(172f),"X/cm",LCARS.EF_TINY);
    }
    
    protected void yScale()
    {
      setMinMaxValue(CSL.ROOM.MIN_Y,CSL.ROOM.MAX_Y);
      addScaleTick(-200f,"-200",LCARS.EF_TINY);
      addScaleTick(-100f,"-100",LCARS.EF_TINY);
      ScaleTick tick = addScaleTick(0f,"0",LCARS.EF_TINY);
      tick.eLine.setColorStyle(LCARS.EC_SECONDARY);
      tick.eLine.setAlpha(0.6f);
      tick.eLabel.setColorStyle(LCARS.EC_SECONDARY);
      addScaleTick(100f,"100",LCARS.EF_TINY);
      addScaleTick(200f,"200",LCARS.EF_TINY);
      addScaleLabel(valueToPos(172f),"Y/cm",LCARS.EF_TINY);
    }
    
    protected void zScale()
    {
      setMinMaxValue(CSL.ROOM.MIN_Z,CSL.ROOM.MAX_Z);
      addScaleTick(  0f,   "0",LCARS.EF_TINY);
      addScaleTick(100f, "100",LCARS.EF_TINY);
      addScaleTick(200f, "200",LCARS.EF_TINY);
      float defZ = (float)CSL.ROOM.DEFAULT_POS.z;
      ScaleTick tick = addScaleTick(defZ,String.format("%.0f",defZ),LCARS.EF_TINY);
      tick.eLine.setColorStyle(LCARS.EC_SECONDARY);
      tick.eLine.setAlpha(0.6f);
      tick.eLabel.setColorStyle(LCARS.EC_SECONDARY);
      addScaleLabel(valueToPos(220f),"Z/cm",LCARS.EF_TINY);
    }
    
    protected void setAlterEgo(ECursor eAlterEgo)
    {
      this.eAlterEgo = eAlterEgo;
    }
    
    public void setHighlighted(boolean highlight)
    {
      int ec = highlight?LCARS.EC_PRIMARY:LCARS.EC_SECONDARY;
      eKnob.setColorStyle(ec);
      eKnob.setSelected(highlight);
      eLine.setColorStyle(ec);
      eLine.setSelected(highlight);
      eLine.setAlpha(highlight?1f:0.5f);
    }
    
    @Override
    public void setValue(float value)
    {
      super.setValue(value);
      eKnob.setLabel(String.format("%.0f",value));
    }
    
    private static int getConstrArg(CA ca, ESensitivityPlot sp, int style)
    throws IllegalArgumentException
    {
      if (sp==null)
        throw new IllegalArgumentException("Invalid value of argument sp");

      Rectangle b = sp.getBounds();
      boolean horiz = (style&ES_HORIZONTAL)!=0;
      boolean lineES = (style&ES_LINE_ES)!=0;

      switch (ca)
      {
      case STYLE:
        return horiz ? style |= ES_ROTATE_KNOB : style;
      case X:
        if (horiz) 
          return b.x;
        else 
          return (lineES ? b.x-KNOB_SIZE-KNOB_GAP : b.x+b.width+KNOB_GAP);  
      case Y:
        if (!horiz) 
          return b.y;
        else
          return (lineES ? b.y-KNOB_SIZE/2-KNOB_GAP : b.y+b.height+KNOB_GAP);
      case W:
        return horiz ? b.width : KNOB_SIZE;
      case H:
        return horiz ? KNOB_SIZE/2 : b.height;
      case CL:
        return horiz ? b.height+KNOB_GAP : b.width+KNOB_GAP;
      default:
        throw new IllegalArgumentException("Invalid value of argument ca");
      }
    }
  }

  // == TESTING AND DEBUGGING ==

  public static class SensitivityPlotsTestPanel extends ATestPanel
  {
    private static final String BTN_PLOTS = "SENSITIVITY PLOTS";
    private static final String BTN_TLYSL = "TROLLEY SLIDER";

    private ESensitivityPlots        eSensPlts;
    private ETrolleySlider           eTrolleySlider;
    private HashMap<String,EElement> aeButtons;
    
    private boolean linkSteering = true;
    
    public SensitivityPlotsTestPanel(IScreen iscreen)
    {
      super(iscreen);
    }

    @Override
    public void init()
    {
      super.init();
      
      // Trolley slider tester
      eTrolleySlider = new ETrolleySlider(1300,170,2*KNOB_SIZE,532);
      eTrolleySlider.addSelectionListener((value)->
      {
        eTrolleySlider.setActualValue(value);
      });
      eTrolleySlider.addToPanel(this);
      aeButtons.get(BTN_TLYSL).setData(eTrolleySlider);
      
      // ESensitivityPlots tester
      LCARS.invokeLater(()->
      {
        eSensPlts = new ESensitivityPlots(120,170);
        eSensPlts.addSelectionListener(new SelectionListener()
        {
          @Override
          public void slicePositionsChanged(Point3d point)
          {
            if (linkSteering)
              DoAEstimator.getInstance().setTargetSource(point);
          }
        });
        eSensPlts.addToPanel(this);
        aeButtons.get(BTN_PLOTS).setData(eSensPlts);
      });
    }

    @Override
    protected int createToolBar(int x, int y, int w, int h)
    {
      aeButtons = new HashMap<String,EElement>();
      ERect eRect;
      int ey = y;

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
            ec.addToPanel(SensitivityPlotsTestPanel.this);
        }
      };

      eRect = new ERect(this,x,ey,w,h,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,BTN_PLOTS);
      eRect.addEEventListener(buttonListerner);
      add(eRect); aeButtons.put(eRect.getLabel(),eRect);
      ey += getElements().get(getElements().size()-1).getBounds().height +3;

      eRect = new ERect(this,x,ey,w,h,LCARS.ES_RECT_RND|LCARS.ES_LABEL_E,BTN_TLYSL);
      eRect.addEEventListener(buttonListerner);
      add(eRect); aeButtons.put(eRect.getLabel(),eRect);
      ey += getElements().get(getElements().size()-1).getBounds().height +3;

      return ey-y-3;
    }
    
    @Override
    protected void fps10()
    {
      if (eSensPlts!=null && eSensPlts.isDisplayed() && linkSteering && LCARS.getArg("--nohardware")==null)
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
    
  }
  
  public static void main(String[] args)
  {
    args = LCARS.setArg(args,"--panel=",SensitivityPlotsTestPanel.class.getName());
    args = LCARS.setArg(args,"--nospeech",null);
    LCARS.main(args);
    if (LCARS.getArg("--nohardware")==null)
    {
      CslHardware.getInstance().dispose();
      System.exit(0);
    }
  }

}

// EOF
