package de.tucottbus.kt.csl.lcars.geometries;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.security.InvalidParameterException;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.jfree.experimental.swt.SWTUtils;

import de.tucottbus.kt.lcars.geometry.AGeometry;

/**
 * A buffered image geometry.
 * @author Christian Borck
 *
 */
public class GBufferedImage extends AGeometry{
  private static final long serialVersionUID = 1L;
  
  private final ImageData image;
  private final int   x;
  private final int   y;

  public GBufferedImage(Image image, int x, int y) {
    super(false);
    if(image == null)
      throw new NullPointerException("image");
    if (x < 0 || y < 0)
      throw new InvalidParameterException("Negative" + (x < 0 ? "x" : "y"));
    
    this.image         = SWTUtils.convertAWTImageToSWT(image);
    this.x             = x;
    this.y             = y;
  }

  @Override
  public Area getArea() {
    return new Area(getBounds());
  }
  
  @Override
  public Rectangle getBounds() {
    return new Rectangle(x, y, image.width, image.height);
  }

  @Override
  public void paint2D(GC gc) {
    org.eclipse.swt.graphics.Image image = new org.eclipse.swt.graphics.Image(gc.getDevice(), this.image);
    gc.drawImage(image, x, y);
    image.dispose();
  }

}
