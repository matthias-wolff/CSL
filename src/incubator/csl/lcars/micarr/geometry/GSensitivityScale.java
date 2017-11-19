package incubator.csl.lcars.micarr.geometry;

import java.awt.Rectangle;
import java.awt.geom.Area;

import javax.vecmath.Color3f;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.rendering.GeometryImageCache;
import incubator.csl.lcars.micarr.geometry.rendering.SensitivityColorScheme;

/**
 * Geometry of a sensitivity scale image for {@link GSensitivityPlot}s. The
 * images is rendered on the LCARS screen side.
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class GSensitivityScale extends AGeometry
{
  private static final long serialVersionUID = 1L;
  private static final float MIN_DB = -36;
  private static final float MAX_DB = 0;

  private final int x;
  private final int y;
  private final int w;
  private final int h;
  
  /**
   * Creates a new sensitivity scale geometry.
   * 
   * @param x
   *          The x-coordinate of the upper left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the upper left corner (in LCARS panel pixels).
   * @param w
   *          The width (in LCARS panel pixels).
   * @param h
   *          The height (in LCARS panel pixels).
   */
  public GSensitivityScale(int x, int y, int w, int h)
  {
    super(false);
    this.x = x;
    this.y = y;
    this.w = w;
    this.h = h;
  }

  @Override
  public Area getArea()
  {
    return new Area(getBounds());
  }

  @Override
  public Rectangle getBounds()
  {
    return new Rectangle(x, y, w, h);
  }

  @Override
  public void paint2D(GC gc)
  {
    // Render sensitivity plot if image cache is empty
    Image image = GeometryImageCache.getImage(getImageCacheKey());
    if (image==null)
    {
      image = new Image(gc.getDevice(),this.w,this.h);
      GC gci = new GC(image);
      for (int pos=0; pos<this.w; pos++)
      {
        float db = MIN_DB + (MAX_DB-MIN_DB)*pos/this.w;
        Color3f c = SensitivityColorScheme.dbToColor3f(db);
        int r = Math.round(c.x*255);
        int g = Math.round(c.y*255);
        int b = Math.round(c.z*255);
        Color c2 = new Color(gci.getDevice(),r,g,b);
        gci.setBackground(c2);
        gci.fillRectangle(pos,0,1,getBounds().height);
        c2.dispose();
      }
      gci.dispose();
      GeometryImageCache.putImage(getImageCacheKey(),image);      
    }
    
    gc.drawImage(image,this.x,this.y);
  }

  /**
   * Returns a key for the global rendered image cache.
   * 
   * @see GeometryImageCache#putImage(Object, Image)
   * @see GeometryImageCache#getImage(Object)
   */
  protected String getImageCacheKey()
  {
    return getClass().getName()+"["+this.w+", "+this.h+"]";
  }
  
}

// EOF
