package incubator.csl.lcars.micarr.elements;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.ArrayList;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import incubator.csl.lcars.micarr.geometry.GSensitivityPlot;
import incubator.csl.lcars.micarr.geometry.rendering.ISensitivityRendererConstants;

/**
 * 2D sensitivity plot of CLS's microphone array.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class ESensitivityPlot extends EElement implements ISensitivityRendererConstants
{
  // -- Fields --
  
  /**
   * 2D sensitivity plot geometry.
   */
  private final GSensitivityPlot gsp;

  // -- Life cycle --
  
  /**
   * Creates a new 2D sensitivity plot of CLS's microphone array.
   * 
   * @param panel
   *          The LCARS panel to place the GUI element on.
   * @param x
   *          The x-coordinate of the upper left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the upper left corner (in LCARS panel pixels).
   * @param w
   *          The width (in LCARS panel pixels). -- <i>not used</i> --
   * @param h
   *          The height (in LCARS panel pixels). -- <i>not used</i> --
   * @param sliceType
   *          The slice type: {@link ISensitivityRendererConstants#SLICE_XY
   *          SLICE_XY}, {@link ISensitivityRendererConstants#SLICE_XZ
   *          SLICE_XZ}, or {@link ISensitivityRendererConstants#SLICE_YZ
   *          SLICE_YZ}.
   * @param micArrayState
   *          The microphone array state.
   */
  public ESensitivityPlot
  (
    Panel         panel, 
    int           x, 
    int           y,
    int           w, 
    int           h, 
    int           sliceType,
    MicArrayState micArrayState
  ) 
  {
    super(panel,x,y,w,h,LCARS.EB_OVERDRAG,null);
    Point pos = new Point(x,y);
    gsp = new GSensitivityPlot(sliceType,micArrayState,pos,null);
  }

  // -- Getters and setters --
  
  @Override
  public Area getArea()
  {
    return gsp.getArea();
  }

  @Override
  public Rectangle getBounds()
  {
    return gsp.getBounds();
  }  

  public boolean usesCL()
  {
    return gsp.usesCL();
  }
  
  public MicArrayState getMicArrayState() 
  {
    return gsp.getMicArrayState();
  }

  public void setMicArrayState(MicArrayState state) 
  {
    if (state==null || state.equals(getMicArrayState()))
      return;
    
    gsp.setMicArrayState(state);
    invalidate(true);
  }

  public int getSliceType()
  {
    return gsp.getSliceType();
  }

  public double getSlicePos() 
  {
    return gsp.getSlicePos();
  }

  public void setSlicePos(double slicePos) 
  {
    if (getSlicePos()==slicePos)
      return;
    
    gsp.setSlicePos(slicePos);
    invalidate(true);
  }

  public float getFrequency() 
  {
    return gsp.getFrequency();
  }

  public void setFrequency(float freq) 
  {
    if (gsp.getFrequency()==freq)
      return;

    gsp.setFrequency(freq);
    invalidate(true);
  }

  /**
   * Converts element coordinates to CSL room coordinates
   * 
   * @param point
   *          LCARS element coordinates (panel coordinates relative to the upper
   *          left corner of the element's bounding rectangle).
   * @return The respective point in the CSL room coordinate system.
   */
  public Point3d elementToCsl(Point point)
  {
    double x = Double.NaN;
    double y = Double.NaN;
    double z = Double.NaN;
    switch (gsp.getSliceType())
    {
    case SLICE_XY:
      x = -(CSL_DIM_X/2)+point.getX();
      y = CSL_DIM_Y-((CSL_DIM_Y/2)+point.getY());
      z = getSlicePos();
      break;
    case SLICE_XZ:
      x = -(CSL_DIM_X/2)+point.getX();
      y = getSlicePos();
      z = CSL_DIM_Z-point.getY();
      break;
    case SLICE_YZ:
      x = getSlicePos();
      y = point.getX()-(CSL_DIM_Y/2);
      z = CSL_DIM_Z-point.getY();
      break;
    }
    return new Point3d(x,y,z);
  }
  
  public Point cslToElement(Point3d point)
  {
    int x = 0;
    int y = 0;
    switch (gsp.getSliceType())
    {
    case SLICE_XY:
      x = (int)Math.round(point.x+(CSL_DIM_X/2));
      y = (int)Math.round(CSL_DIM_Y/2-point.y);
      break;
    case SLICE_XZ:
      x = (int)Math.round(point.x+(CSL_DIM_X/2));
      y = (int)Math.round(CSL_DIM_Z-point.z);
      break;
    case SLICE_YZ:
      x = (int)Math.round(CSL_DIM_Y/2+point.y);
      y = (int)Math.round(CSL_DIM_Z-point.z);
      break;
    }
    return new Point(x,y);
  }
  
  // -- Implementation of abstract methods --
  
  @Override
  protected ArrayList<AGeometry> createGeometriesInt() 
  {
    ArrayList<AGeometry> geos = new ArrayList<AGeometry>();
    geos.add(gsp);
    return geos;
  }

}

// EOF
