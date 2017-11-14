package de.tucottbus.kt.csl.lcars.elements;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;

import de.tucottbus.kt.csl.lcars.geometries.GBufferedImage;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.geometry.AGeometry;

/**
 * A buffered image.
 * 
 * @author Christian Borck
 * @deprecated -- use {@link ERenderedImage} instead --
 */
@Deprecated
public class EBufferedImage extends EElement implements ImageObserver
{
  private Image image;
  
  public EBufferedImage(Panel panel, int x, int y, int style, BufferedImage img)
  {
    super(panel,x,y,0,0,style,null);
    image = img;
  }

  @Override
  public ArrayList<AGeometry> createGeometriesInt()
  {
    int x = getBounds().x;
    int y = getBounds().y;
    
    if (image!=null)
    {
      image.getWidth(this);
      image.getHeight(this);
    }
    ArrayList<AGeometry> geos = new ArrayList<AGeometry>();
    geos.add(new GBufferedImage(image, x, y));
    return geos;
  }
  
  public boolean imageUpdate(BufferedImage img, int infoflags, int x, int y, int width, int height)
  {
    if ((infoflags & (ALLBITS|SOMEBITS)) >0)
    {
      image = img;
      Rectangle rect = getBounds();
      rect.width  = img.getWidth(this);
      rect.height = img.getHeight(this);
      invalidate(true);
    }
    return true;
  }

  @Override
  public boolean imageUpdate(Image img, int infoflags, int x, int y, int width,
      int height) {
    // TODO Auto-generated method stub
    return false;
  }  
}
