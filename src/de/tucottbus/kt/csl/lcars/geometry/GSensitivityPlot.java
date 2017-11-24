package de.tucottbus.kt.csl.lcars.geometry;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.lcars.geometry.rendering.CLSensitivityRenderer;
import de.tucottbus.kt.csl.lcars.geometry.rendering.CpuSensitivityRenderer;
import de.tucottbus.kt.csl.lcars.geometry.rendering.ISensitivityRenderer;
import de.tucottbus.kt.csl.lcars.geometry.rendering.ISensitivityRendererConstants;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.rendering.GeometryImageCache;

/**
 * Geometry of a 2D spatial sensitivity plot of the CSL microphone array. The
 * images are rendered on the LCARS screen side.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class GSensitivityPlot extends AGeometry implements ISensitivityRendererConstants
{
  // -- Serializable fields --
  
  /**
   * The default serial version id.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Type of slice to display: {@link ISensitivityRendererConstants#SLICE_XY
   * SLICE_XY}, {@link ISensitivityRendererConstants#SLICE_XZ SLICE_XZ}, or
   * {@link ISensitivityRendererConstants#SLICE_YZ SLICE_YZ}.
   */
  private final int sliceType;

  /**
   * The microphone array state to render the sensitivity plot for.
   */
  private MicArrayState state;
  
  /**
   * The frequency to render the sensitivity plot for.
   */
  private float freq;
  
  /**
   * Position of slice orthogonal to {@link #sliceType}.
   */
  private int slicePos;
  
  /**
   * Top-left corner of the geometry, in LCARS panel pixels.
   */
  private Point pos;
  
  /**
   * Size of the geometry, in LCARS panel pixels.
   */
  private final Dimension size;

  // -- Transient fields (not serialized) --
  
  /**
   * Renderer supplying the sensitivity images.
   */
  private transient ISensitivityRenderer renderer;
  
  // -- Life cycle
  
  /**
   * Creates a new 2D sensitivity plot geometry.
   * 
   * @param sliceType
   *          The slice type: {@link ISensitivityRendererConstants#SLICE_XY
   *          SLICE_XY}, {@link ISensitivityRendererConstants#SLICE_XZ
   *          SLICE_XZ}, or {@link ISensitivityRendererConstants#SLICE_YZ
   *          SLICE_YZ}.
   * @param state
   *          The microphone array state.
   * @param pos
   *          The position of the geometry (LCARS panel coordinates).
   * @param size
   *          -- <i>reserved</i> -- Commit <code>null</code>!
   */
  public GSensitivityPlot
  (
    int           sliceType, 
    MicArrayState state, 
    Point         pos,
    Dimension     size
  ) 
  {
    super(false);
    this.sliceType = sliceType;
    this.state     = state;
    this.pos       = pos;
    this.size      = getDefaultSize();
    this.slicePos  = sliceType==SLICE_XY ? (int)Math.round(CSL.ROOM.DEFAULT_POS.z) : 0;
    this.freq      = 1000;   
  }

  @Override
  protected void finalize() throws Throwable
  {
    GeometryImageCache.removeImage(getImageCacheKey());
  }
  
  // -- Getters and setters --
  
  /**
   * Determines if this plot geometry uses openCL for rendering.
   */
  public boolean usesCL()
  {
    return renderer.usesCL();
  }
  
  @Override
  public Area getArea() 
  {
    return new Area(getBounds());
  }

  @Override
  public Rectangle getBounds() 
  {
    if (size!=null)
      return new Rectangle(pos.x,pos.y,size.width,size.height);
    else
      return new Rectangle(pos.x,pos.y,getDefaultSize().width,getDefaultSize().height);
  }

  /**
   * Returns the default plot image size.
   */
  public Dimension getDefaultSize()
  {
    return getDefaultSize(this.sliceType);
  }
  
  /**
   * Returns the default plot image size.
   * 
   * @param sliceType
   *          The slice type: {@link ISensitivityRendererConstants#SLICE_XY
   *          SLICE_XY}, {@link ISensitivityRendererConstants#SLICE_XZ
   *          SLICE_XZ}, or {@link ISensitivityRendererConstants#SLICE_YZ
   *          SLICE_YZ}.
   */
  public static Dimension getDefaultSize(int sliceType)
  {
    switch (sliceType) 
    {
    case SLICE_XZ:
      return new Dimension(CSL.ROOM.DIM_X,CSL.ROOM.DIM_Z);
    case SLICE_YZ:
      return new Dimension(CSL.ROOM.DIM_Y,CSL.ROOM.DIM_Z);
    default: // SLICE_XY
      return new Dimension(CSL.ROOM.DIM_X,CSL.ROOM.DIM_Y);
    }
  }
  
  /**
   * Repositions this geometry.
   * 
   * @param x
   *          The new absolute x-coordinate of the top-left corner (in LCARS
   *          panel pixels).
   * @param y
   *          The new absolute y-coordinate of the top-left corner (in LCARS
   *          panel pixels).
   */
  public void setPos(int x, int y)
  {
    this.pos = new Point(x,y);
  }
  
  /**
   * Returns the plot slice type: {@link ISensitivityRendererConstants#SLICE_XY
   * SLICE_XY}, {@link ISensitivityRendererConstants#SLICE_XZ SLICE_XZ}, or
   * {@link ISensitivityRendererConstants#SLICE_YZ SLICE_YZ}.
   */
  public int getSliceType()
  {
    return sliceType;
  }
  
  /**
   * Sets the microphone array state.
   * 
   * @param state
   *          The microphone array state.
   */
  public void setMicArrayState(MicArrayState state)
  {
    if (state==null || state.equals(this.state))
      return;
    this.state = state;
    GeometryImageCache.removeImage(getImageCacheKey());
  }
  
  /**
   * Returns the microphone array state. 
   */
  public MicArrayState getMicArrayState()
  {
    return this.state;
  }
  
  /**
   * Sets the plot slice position.
   * 
   * @param slicePos
   *          The position.
   */
  public void setSlicePos(double slicePos) 
  {
    int slicePosNew = (int)Math.round(slicePos);
    if (this.slicePos==slicePosNew)
      return;
    this.slicePos = slicePosNew;
    GeometryImageCache.removeImage(getImageCacheKey());
  }
  
  /**
   * Returns the plot slice position.
   */
  public double getSlicePos()
  {
    return this.slicePos;
  }
  
  /**
   * Sets the frequency for which the spatial sensitivity is plotted.
   * 
   * @param freq
   *          The frequency in Hz, must be positive.
   */
  public void setFrequency(float freq) 
  {
    if (this.freq==freq)
      return;

    this.freq = Math.max(freq, 0.1f);
    GeometryImageCache.removeImage(getImageCacheKey());
  }
  
  /**
   * Returns the frequency for the spatial sensitivity is plotted. 
   */
  public float getFrequency()
  {
    return this.freq;
  }

  // -- Workers --
  
  @Override
  public void paint2D(GC gc) 
  {
    if (renderer==null)
      try
      {
        this.renderer = CLSensitivityRenderer.getInstance();
      }
      catch (Exception e)
      {
        this.renderer = CpuSensitivityRenderer.getInstance();
      }
    
    // Render sensitivity plot if image cache is empty
    Image image = GeometryImageCache.getImage(getImageCacheKey());
    if (image==null)
    {
      int width = getDefaultSize().width;
      int height = getDefaultSize().height;
      PaletteData palette = new PaletteData((int)0xFF00l, (int)0xFF0000l, (int)0xFF000000l);
      ImageData imageData = new ImageData(width,height,32,palette);
      int[] pixels = renderer.renderIntArray(state,freq,sliceType,slicePos,width,height);
      imageData.setPixels(0,0,width*height,pixels,0);
      image = new Image(gc.getDevice(),imageData);
      
      GeometryImageCache.putImage(getImageCacheKey(),image);
    }
    
    // Draw sensitivity plot
    gc.drawImage(image,pos.x,pos.y);
    /* -- Draw scaled plot
     * Rectangle b = this.bounds;      
     * double scale = Math.max(b.width / bi.getWidth(), b.height / bi.getHeight());
     * gc.drawImage(image, 0,0,imageData.width, imageData.height,
     *            b.x,b.y,(int)(imageData.width*scale),(int)(imageData.height*scale)); 
     */
  }

  /**
   * Returns a key for the global rendered image cache.
   * 
   * @see GeometryImageCache#putImage(Object, Image)
   * @see GeometryImageCache#getImage(Object)
   */
  protected Object getImageCacheKey()
  {
    String key = getClass().getName()+"[";
    key += state.toString();
    key += ", "+freq;
    key += ", "+sliceType;
    key += ", "+slicePos;
    key += ", "+getDefaultSize().width;
    key += ", "+getDefaultSize().height;
    key += "]";
    return key;
  }

}

// EOF
