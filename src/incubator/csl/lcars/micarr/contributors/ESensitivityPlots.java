package incubator.csl.lcars.micarr.contributors;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
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

/**
 * This class contributes 2D sensitivity plots of CLS's microphone array to an
 * LCARS panel.
 * 
 * <p>
 * <b>TODO:</b>
 * <ul>
 * <li>Add color scale with a live cursor following the sensitivity at the
 * current slices position.</li>
 * <li>Add animation: Random trajectory of slice positions attracted to steering
 * focus and repelled from room borders.</li>
 * </ul>
 * </p>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class ESensitivityPlots extends ElementContributor
{
  // -- Fields --
  
  private final ESensitivityPlot eSpxy;
  private final CCursor          gSpxyCursorH;
  private final CCursor          gSpxyCursorV;
  private final ESensitivityPlot eSpyz;
  private final CCursor          gSpyzCursorH;
  private final CCursor          gSpyzCursorV;
  private final ESensitivityPlot eSpxz;
  private final CCursor          gSpxzCursorH;
  private final CCursor          gSpxzCursorV;
  private final EEventListener   plotTouchedEvent;
  private final EValue           eXPos;
  private final EValue           eYPos;
  private final EValue           eZPos;
  private final EElbo            eXyYz;
  private final EElement         eXyYzArrow;
  private final EElbo            eXyXz1;
  private final EElbo            eXyXz2;
  private final EElement         eXyXzArrow;

  // -- Life cycle --
  
  /**
   * Creates a new 2D sensitivity plots contributors.
   * 
   * @param state
   *          The initial microphone array state.
   * @param x
   *          The x-coordinate of the top-left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the top-left corner (in LCARS panel pixels).
   */
  public ESensitivityPlots(MicArrayState state, int x, int y)
  {
    super(x, y);
    
    EElbo eElbo;
    EValue eValue;
    
    // Create common event listeners
    plotTouchedEvent = new EEventListenerAdapter()
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
        setSlicePositions(((ESensitivityPlot)ee.el).elementToCsl(ee.pt));
      }
    };
    
    // XY-plot
    eSpxy = new ESensitivityPlot(null,this.x,this.y,-1,-1,ESensitivityPlot.SLICE_XY,state);
    eSpxy.addEEventListener(plotTouchedEvent);
    add(eSpxy);
    
    // - XY-plot: Frame
    eValue = new EValue(null, -CCursor.handleSize*7/8-CCursor.handleGap, -CCursor.handleGap - CCursor.handleSize/2, CCursor.handleSize*7/8+CCursor.handleGap, CCursor.handleSize/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
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
    int ex = 500;
    int ey = 0;
    eSpxz = new ESensitivityPlot(null,this.x+ex,this.y+ey,-1,-1,ESensitivityPlot.SLICE_XZ,state);
    eSpxz.addEEventListener(plotTouchedEvent);
    add(eSpxz);

    // - XZ-plot: Frame
    ey += -CCursor.handleSize/2-CCursor.handleGap;
    int eh = eSpxz.getBounds().height/2 + CCursor.handleSize/2 + CCursor.handleGap;
    eElbo = new EElbo(null,ex-13,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    eElbo = new EElbo(null,ex-13,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SW,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    ex += eSpxz.getBounds().width;
    eValue = new EValue(null, ex+3, ey, CCursor.handleGap + CCursor.handleSize-2, CCursor.handleSize/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
    eValue.setValueMargin(0); eValue.setValue("XZ");
    add(eValue);
    ex += CCursor.handleGap + CCursor.handleSize;
    eElbo = new EElbo(null,ex,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NE,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    eElbo = new EElbo(null,ex,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    ey += 2*eh - CCursor.handleSize /2;
    eValue = new EValue(null, ex-160, ey, 157, CCursor.handleSize/2, LCARS.ES_STATIC|LCARS.EC_ELBOUP, null);
    eValue.setValueMargin(CCursor.handleSize-CCursor.handleGap); eValue.setValue("REAR>FRONT");
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
    eSpyz = new ESensitivityPlot(null,this.x+ex,this.y+ey,-1,-1,ESensitivityPlot.SLICE_YZ,state);
    eSpyz.addEEventListener(plotTouchedEvent);
    add(eSpyz);

    // - YZ-plot: Frame
    ey += -CCursor.handleSize/2-CCursor.handleGap;
    eh = eSpyz.getBounds().height/2 + CCursor.handleSize/2 + CCursor.handleGap;
    eElbo = new EElbo(null,ex-13,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    eElbo = new EElbo(null,ex-13,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SW,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    ex += eSpxz.getBounds().width + CCursor.handleGap + CCursor.handleSize;
    eValue = new EValue(null, ex-160, ey, 157, CCursor.handleSize/2, LCARS.ES_STATIC|LCARS.EC_ELBOUP, null);
    eValue.setValueMargin(CCursor.handleSize-CCursor.handleGap); eValue.setValue("WINDOW>DOOR");
    add(eValue);
    eElbo = new EElbo(null,ex,ey,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_NE,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    eElbo = new EElbo(null,ex,ey+eh,10,eh,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
    eElbo.setArmWidths(6,CCursor.handleSize/2);
    add(eElbo);
    ey += 2*eh - CCursor.handleSize/2;
    ex -= CCursor.handleGap + CCursor.handleSize;
    eValue = new EValue(null, ex+3, ey, CCursor.handleGap + CCursor.handleSize-2, CCursor.handleSize/2, LCARS.ES_STATIC|LCARS.ES_SELECTED, null);
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
    ey = eSpyz.getBounds().y - this.y - CCursor.handleSize - CCursor.handleGap;
    eXPos = new EValue(null,ex,ey-3,92,CCursor.handleSize+3,LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"X");
    eXPos.setValueMargin(0); eXPos.setValueWidth(63); eXPos.setValue("000");
    eXPos.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        Point3d point = getSlicePositions();
        point.x = 0;
        setSlicePositions(point);
      }
    });
    add(eXPos);
    eYPos = new EValue(null,ex+95,ey-3,92,CCursor.handleSize+3,LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"Y");
    eYPos.setValueMargin(0); eYPos.setValueWidth(63); eYPos.setValue("000");
    eYPos.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        Point3d point = getSlicePositions();
        point.y = 0;
        setSlicePositions(point);
      }
    });
    add(eYPos);
    eZPos = new EValue(null,ex+190,ey-3,92,CCursor.handleSize+3,LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"Z");
    eZPos.setValueMargin(0); eZPos.setValueWidth(63); eZPos.setValue("000");
    eZPos.addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchUp(EEvent ee)
      {
        Point3d point = getSlicePositions();
        point.z = 160;
        setSlicePositions(point);
      }
    });
    add(eZPos);
    add(new ERect(null,ex+285,ey-3,39,CCursor.handleSize+3,LCARS.ES_STATIC|LCARS.ES_LABEL_W|LCARS.ES_SELECTED,"cm"));
    
    // Connecting lines
    // - XY <-> YZ plots (layout done by repositionConnectingLines)
    eXyYz = new EElbo(null,0,0,0,0,LCARS.ES_STATIC|LCARS.ES_SHAPE_SW,null);
    eXyYz.setArmWidths(CCursor.cursorWidth,CCursor.cursorWidth);
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
    eXyXz1.setArmWidths(CCursor.cursorWidth,CCursor.cursorWidth);
    eXyXz1.setArcWidths(30,27);
    add(eXyXz1);
    eXyXz2 = new EElbo(null,0,0,0,0,LCARS.ES_STATIC|LCARS.ES_SHAPE_NW,null);
    eXyXz2.setArmWidths(CCursor.cursorWidth,CCursor.cursorWidth);
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

    // Initialization
    gSpxyCursorH.setAlterEgo(gSpyzCursorV); gSpyzCursorV.setAlterEgo(gSpxyCursorH);
    gSpxyCursorV.setAlterEgo(gSpxzCursorV); gSpxzCursorV.setAlterEgo(gSpxyCursorV);
    gSpyzCursorH.setAlterEgo(gSpxzCursorH); gSpxzCursorH.setAlterEgo(gSpyzCursorH);
   
    setSlicePositions(new Point3d(0,0,160));
  }
  
  // -- Getters and setters --
  
  /**
   * Repositions the 2D sensitivity plot slices (CSL room coordinates in cm).
   * 
   * @param point
   *          The new slice positions.
   */
  public void setSlicePositions(Point3d point)
  {
    point.x = Math.max(-220,Math.min(220,point.x));
    point.y = Math.max(-220,Math.min(220,point.y));
    point.z = Math.max(   0,Math.min(250,point.z));
    
    DoAEstimator.getInstance().setTargetSource(point);
    MicArrayState state = MicArrayState.getCurrentState();
   
    eXPos.setValue(String.format("% 04.0f",point.x));
    eYPos.setValue(String.format("% 04.0f",point.y));
    eZPos.setValue(String.format("% 04.0f",point.z));
    
    eSpxy.setMicArrayState(state);
    eSpxy.setSlicePos(point.z);
    gSpxyCursorH.setPos(eSpxy.cslToElement(point).y,point.y);
    gSpxyCursorV.setPos(eSpxy.cslToElement(point).x,point.x);

    eSpyz.setMicArrayState(state);
    eSpyz.setSlicePos(point.x);
    gSpyzCursorH.setPos(eSpyz.cslToElement(point).y,point.z);
    gSpyzCursorV.setPos(eSpyz.cslToElement(point).x,point.y);
    
    eSpxz.setMicArrayState(state);
    eSpxz.setSlicePos(point.y);
    gSpxzCursorH.setPos(eSpxz.cslToElement(point).y,point.z);
    gSpxzCursorV.setPos(eSpxz.cslToElement(point).x,point.x);
    
    repositionConnectingLines();
  }

  /**
   * Retrieves the current 2D sensitivity plot slice positions.
   * 
   * @return
   *   The positions (CSL room coordinates in cm).
   */
  public Point3d getSlicePositions()
  {
    int x = gSpxyCursorV.getPos();
    int y = gSpxyCursorH.getPos();
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
  }
  
  /**
   * Returns the frequency for the spatial sensitivity is plotted. 
   */
  public float getFrequency()
  {
    return eSpxy.getFrequency();
  }
  
  // -- Workers and event handlers --
  
  protected void cursorDragged(CCursor cur, int pos)
  {
    Point3d point = new Point3d();

    if (cur==gSpxyCursorH)
      point = eSpxy.elementToCsl(new Point(gSpxyCursorV.getPos(),pos));
    else if (cur==gSpxyCursorV)
      point = eSpxy.elementToCsl(new Point(pos,gSpxyCursorH.getPos()));
    else if (cur==gSpyzCursorH)
      point = eSpyz.elementToCsl(new Point(gSpyzCursorV.getPos(),pos));
    else if (cur==gSpyzCursorV)
      point = eSpyz.elementToCsl(new Point(pos,gSpyzCursorH.getPos()));
    else if (cur==gSpxzCursorH)
      point = eSpxz.elementToCsl(new Point(gSpxzCursorV.getPos(),pos));
    else if (cur==gSpxzCursorV)
      point = eSpxz.elementToCsl(new Point(pos,gSpxzCursorH.getPos()));
    
    setSlicePositions(point);
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
    p1 = new Point(b1.x+gSpxyCursorV.getPos()-CCursor.cursorWidth/2,b1.y+b1.height);
    p2 = new Point(b2.x-13,p1.y+30);
    eXyYz.setBounds(new Rectangle(p1.x,p1.y,p2.x-p1.x,p2.y-p1.y));
    
    b = eXyYzArrow.getBounds();
    b.x = p1.x+CCursor.cursorWidth+CCursor.handleGap;
    b.y = p1.y+CCursor.handleGap;
    eXyYzArrow.setBounds(b);
    
    // Reposition line from XY to XZ plot
    b2 = eSpxz.getBounds();
    p1 = new Point(b1.x+b1.width,b1.y+gSpxyCursorH.getPos()-CCursor.cursorWidth/2);
    p2 = new Point(b2.x-13,b2.y+b2.height/2-CCursor.cursorWidth/2);
    pm = new Point(p1.x+(p2.x-p1.x)/2,p1.y+(p2.y-p1.y)/2);
    b1 = new Rectangle(p1.x,p1.y,pm.x-p1.x+CCursor.cursorWidth/2,pm.y-p1.y);
    int s1 = LCARS.ES_SHAPE_NE;
    if (b1.height<0)
    {
      b1 = new Rectangle(b1.x,b1.y+b1.height+CCursor.cursorWidth/2-1,b1.width,-b1.height+CCursor.cursorWidth);
      s1 = LCARS.ES_SHAPE_SE;
    }
    int s2 = LCARS.ES_SHAPE_SW;
    b2 = new Rectangle(b1.x+b1.width-CCursor.cursorWidth,pm.y,p2.x-pm.x+CCursor.cursorWidth,p2.y-pm.y);
    if (b2.height<0)
    {
      b2 = new Rectangle(b2.x,b2.y+b2.height,b2.width,-b2.height);
      s2 = LCARS.ES_SHAPE_NW;
    }
    b1.height = Math.max(b1.height,CCursor.cursorWidth);
    b2.height = Math.max(b2.height,CCursor.cursorWidth);
    eXyXz1.setBounds(b1); eXyXz1.setStyle(s1); 
    eXyXz1.setArcWidths(Math.min(b1.height,30),Math.min(b1.height-3,27));
    eXyXz2.setBounds(b2); eXyXz2.setStyle(s2);
    eXyXz2.setArcWidths(Math.min(b2.height,30),Math.min(b2.height-3,27));

    b = eXyXzArrow.getBounds();
    b.x = p1.x+CCursor.handleGap;
    b.y = p1.y+CCursor.cursorWidth+CCursor.handleGap;
    eXyXzArrow.setBounds(b);
}
  
  // -- Nested classes --
  
  protected class CCursor extends ArrayList<EElement>
  {
    protected final ESensitivityPlot sp;
    protected final boolean          horz;
    protected final boolean          handleLeftOrTop;
    protected final ERect            eSclBk;
    protected final ERect            eLine;
    protected final ERect            eHandle;
    protected       CCursor          alterEgo;
   
    private   static final long serialVersionUID = 1L;
    protected static final int  handleSize       = 44;
    protected static final int  handleGap        = 3;
    protected static final int  cursorWidth      = 3;
    
    protected CCursor(ESensitivityPlot sp, boolean horizonal, boolean handleLeftOrTop)
    { 
      this.sp = sp;
      this.horz = horizonal;
      this.handleLeftOrTop = handleLeftOrTop;
      
      // Add scale background
      Rectangle b = getInitialScaleBounds();
      eSclBk = new ERect(null,b.x,b.y,b.width,b.height,LCARS.ES_STATIC,null);
      eSclBk.setAlpha(0.2f);
      add(eSclBk);

      // Add cursor line
      b = getInitialCursorLineBounds();
      eLine = new ERect(null,b.x,b.y,b.width,b.height,LCARS.ES_STATIC|LCARS.EC_SECONDARY,null);
      add(eLine);
      
      // Add cursor handle
      if (handleLeftOrTop)
        if (horz) { b.x-=handleSize; b.y-=handleSize/4; }
        else      { b.x-=handleSize/2; b.y-=handleSize/2; }
      else
        if (horz) { b.x+=b.width; b.y-=handleSize/4; }
        else      { b.x-=handleSize/2; b.y+=b.height; }
      
      int style = LCARS.ES_RECT_RND|LCARS.EB_OVERDRAG|LCARS.ES_LABEL_C|LCARS.EF_SMALL|LCARS.EC_SECONDARY;
      eHandle = new ERect(null,b.x,b.y,handleSize,handleSize/2,style,"000");
      eHandle.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          setHighlighted(true);
          if (alterEgo!=null)
            alterEgo.setHighlighted(true);
        }

        @Override
        public void touchUp(EEvent ee)
        {
          setHighlighted(false);
          if (alterEgo!=null)
            alterEgo.setHighlighted(false);
          int pos;
          if (horz)
            pos = ee.el.getBounds().y-sp.getBounds().y+ee.pt.y;
          else
            pos = ee.el.getBounds().x-sp.getBounds().x+ee.pt.x;
          ESensitivityPlots.this.cursorDragged(CCursor.this,pos);
        }
        
        @Override
        public void touchDrag(EEvent ee)
        {
          int pos;
          if (horz)
            pos = ee.el.getBounds().y-sp.getBounds().y+ee.pt.y;
          else
            pos = ee.el.getBounds().x-sp.getBounds().x+ee.pt.x;
          ESensitivityPlots.this.cursorDragged(CCursor.this,pos);
        }
      });
      add(eHandle);
      
      // Initialize
      setHighlighted(false);
    }
    
    protected void addScaleTick(int pos, String label, boolean gridLine)
    {
      Rectangle b = getInitialCursorLineBounds();
      if (horz)
      {
        b.y = sp.getBounds().y - ESensitivityPlots.this.y + pos;
        b.height = 1;
        b.width += handleSize*7/8;
        if (handleLeftOrTop)
          b.x -= handleSize*7/8;
      }
      else
      {
        b.x = sp.getBounds().x - ESensitivityPlots.this.x + pos;
        b.width = 1;
        b.height += handleSize/2;
        if (handleLeftOrTop)
          b.y -= handleSize/2;
      }
      
      if (gridLine)
      {
        ERect eGridLine = new ERect(null,b.x,b.y,b.width,b.height,LCARS.ES_STATIC,null); 
        eGridLine.setAlpha(0.3f);
        add(eGridLine);
      }
      
      int s = LCARS.ES_STATIC|LCARS.EF_TINY;
      if (handleLeftOrTop)
        if (horz) { b.x-=3; b.y-=handleSize/2-3; s |= LCARS.ES_LABEL_SE; }
        else      { b.x+=3; s |= LCARS.ES_LABEL_W; }
      else
        if (horz) { b.y-=handleSize/2-3; b.x += b.width-handleSize*3/4-3; s |= LCARS.ES_LABEL_SE; }
        else      { b.x+=3; b.y += b.height-handleSize/2; s |= LCARS.ES_LABEL_W; }
      
      ELabel eLabel = new ELabel(null,b.x,b.y,handleSize*3/4,handleSize/2,s,label);
      eLabel.setAlpha(0.5f);
      add(eLabel);
    
    }
    
    protected void setAlterEgo(CCursor alterEgo)
    {
      this.alterEgo = alterEgo;
    }
    
    protected void setPos(int pos, double value)
    {
      // Move cursor line
      Rectangle b = eLine.getBounds();
      if (horz)
        b.y = sp.getBounds().y+pos-b.height/2;
      else
        b.x = sp.getBounds().x+pos-b.width/2;
      eLine.setBounds(b);

      // Move cursor value
      b = eHandle.getBounds();
      if (horz)
        b.y = sp.getBounds().y+pos-b.height/2;
      else
        b.x = sp.getBounds().x+pos-b.width/2;
      eHandle.setBounds(b);
      eHandle.setLabel(String.format("%.0f",value));
    }
    
    protected int getPos()
    {
      Rectangle b = eHandle.getBounds();
      if (horz)
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
      eHandle.setColorStyle(ec);
      eHandle.setSelected(highlight);
    }
  
    private Rectangle getInitialCursorLineBounds()
    {
      Rectangle b = sp.getBounds(); 
      int x = b.x+(horz?0:b.width/2-cursorWidth/2) - ESensitivityPlots.this.x;
      int y = b.y+(horz?b.height/2-cursorWidth/2:0) - ESensitivityPlots.this.y;
      int w = horz?b.width:cursorWidth;
      int h = horz?cursorWidth:b.height;
      
      if (handleLeftOrTop)
      {
        if (horz) { x-=handleGap; w+=handleGap; }
        else      { y-=handleGap; h+=handleGap; }
      }
      else
      {
        if (horz) { w+=handleGap; }
        else      { h+=handleGap; }
      }

      return new Rectangle(x,y,w,h);
    }
    
    private Rectangle getInitialScaleBounds()
    {
      Rectangle b = sp.getBounds(); 
      int x = b.x - ESensitivityPlots.this.x;
      int y = b.y - ESensitivityPlots.this.y;
      int w = horz?handleSize*3/4:b.width;
      int h = horz?b.height:handleSize/2;
      
      if (handleLeftOrTop)
      {
        if (horz) { x-=handleGap+handleSize*7/8; }
        else      { y-=handleGap+handleSize/2; }
      }
      else
      {
        if (horz) { x+=b.width+handleGap+handleSize/8; }
        else      { y+=b.height+handleGap; }
      }

      return new Rectangle(x,y,w,h);
    }
  }

  protected class CSensitivityScale extends ArrayList<EElement>
  {
    private static final long serialVersionUID = 1L;

    // TODO: ...
  }
  
}

// EOF
