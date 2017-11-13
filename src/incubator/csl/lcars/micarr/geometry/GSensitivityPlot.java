package incubator.csl.lcars.micarr.geometry;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.lcars.elements.ERenderedImage;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.logging.Log;
import incubator.csl.lcars.micarr.geometry.rendering.CLSensitivityRenderer;
import incubator.csl.lcars.micarr.geometry.rendering.CpuSensitivityRenderer;
import incubator.csl.lcars.micarr.geometry.rendering.ISensitivityRenderer;
import incubator.csl.lcars.micarr.geometry.rendering.ISensitivityRendererConstants;

/**
 * Geometry of a 2D spatial sensitivity plot of the CSL microphone array.
 * 
 * <h3>TODO:</h3>
 * <ul>
 *   <li>Make sensitivity image scalable.</li>
 * </ul>
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 * @deprecated -- Use {@link ERenderedImage}! --
 */
@Deprecated
public class GSensitivityPlot extends AGeometry implements ISensitivityRendererConstants
{
  private static final long serialVersionUID = 1L;

  /**
   * Renderer supplying the sensitivity images.
   */
  private ISensitivityRenderer renderer;
  
  /**
   * Rendering cache.
   */
  private Image image;
  
  /**
   * Type of slice to display: {@link ISensitivityRendererConstants#SLICE_XY
   * SLICE_XY}, {@link ISensitivityRendererConstants#SLICE_XZ SLICE_XZ}, or
   * {@link ISensitivityRendererConstants#SLICE_YZ SLICE_YZ}.
   */
  private final int sliceType;
  
  /**
   * Position of slice orthogonal to {@link #sliceType}.
   */
  private int slicePos;
  
  private final Point pos;
  
  private final Dimension size;
  
  private MicArrayState state;
  
  private float freq;
  
  private ImageData imageData;

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
    this.slicePos  = sliceType==SLICE_XY ? 160 : 0;
    this.freq      = 1000;
   
    try
    {
      this.renderer = new CLSensitivityRenderer();
    }
    catch (Exception e)
    {
      Log.warn("No openCL rendering available. Falling back to CPU rendering.");
      this.renderer = new CpuSensitivityRenderer();
    }
  }

  @Override
  public void finalize()
  {
    clearCache();
  }
  
  protected void clearCache()
  {
    if (image==null)
      return;
    image.dispose();
    image = null;
  }
  
  // -- Getters and setters --

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

  public Dimension getDefaultSize()
  {
    return getDefaultSize(this.sliceType);
  }
  
  public static Dimension getDefaultSize(int sliceType)
  {
    switch (sliceType) 
    {
    case SLICE_XZ:
      return new Dimension(CSL_DIM_X,CSL_DIM_Z);
    case SLICE_YZ:
      return new Dimension(CSL_DIM_Y,CSL_DIM_Z);
    default: // SLICE_XY
      return new Dimension(CSL_DIM_X,CSL_DIM_Y);
    }
  }
  
  public int getSliceType()
  {
    return sliceType;
  }
  
  public void setSlicePos(double slicePos) 
  {
    int slicePosNew = (int)Math.round(slicePos);
    if (this.slicePos==slicePosNew)
      return;
    this.slicePos = slicePosNew;
    clearCache();
  }
  
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
    clearCache();
  }
  
  /**
   * Returns the frequency for the spatial sensitivity is plotted. 
   */
  public float getFrequency()
  {
    return this.freq;
  }
  
  public void setMicArrayState(MicArrayState state)
  {
    if (state==null || state.equals(this.state))
      return;
    this.state = state;
    clearCache();
  }
  
  public MicArrayState getMicArrayState()
  {
    return this.state;
  }

  // -- Workers --
  
  @Override
  public void paint2D(GC gc) 
  {
    // Render sensitivity plot if image cache is empty
    if (image==null)
    {
      int imgW = getDefaultSize().width;
      int imgH = getDefaultSize().height;
      if (imageData == null)
      {
        Image img = new Image(gc.getDevice(),imgW,imgH);
        imageData = img.getImageData();
        img.dispose();
      }
  
      int[] pixels = renderer.renderIntArray(state,freq,sliceType,slicePos,imgW,imgH);
      imageData.setPixels(0,0,imgW*imgH,pixels,0);
      image = new Image(gc.getDevice(),imageData);
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

}
