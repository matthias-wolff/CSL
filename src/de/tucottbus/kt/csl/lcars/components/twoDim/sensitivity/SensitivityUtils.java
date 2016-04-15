package de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Abstract class for the sensitivity plot.
 * 
 * @author Martin Birth
 *
 */
public class SensitivityUtils{
  
  private SensitivityUtils(){}
  
  /**
   * Converting a integer pixel array to a BufferdImage.
   * 
   * Please use the width for image scanline.
   * 
   * @param pixels
   *          in[] array of pixel values (ARGB)
   * @param width
   *          int - image width
   * @param height
   *          int - image height
   * @return
   */
  public static BufferedImage convertingIntPixelImageToBufferedImage(int[] pixels, int width, int height) {
    BufferedImage image = new BufferedImage(width, height,
        BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g = image.createGraphics();
    
    image.setRGB(0, 0, width, height, pixels, 0, width);
    g.drawImage(image, 0, 0, null);

    return image;
  }
  
  /**
   * Get the a horizontal level scale image
   * 
   * @param width
   *          int: image width
   * @param height
   *          int: image height
   * @return BufferedImage
   */
  public static BufferedImage getVerticalLevelScale(int width, int height) {
    BufferedImage image = new BufferedImage(width, height,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        g.setColor(SensitivityColorScheme.getColor3f(((double) y) / height * (-36)).get());
        g.drawRect(x, y, 1, 1);
      }
    }

    return image;
  }
  
  /**
   * Get the a vertical level scale image.
   * 
   * @param width
   *          int: image width
   * @param height
   *          int: image height
   * @return BufferedImage
   */
  public static BufferedImage getHorizontalLevelScale(int width, int height){
    BufferedImage image = new BufferedImage(width, height,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        g.setColor(SensitivityColorScheme.getColor3f(((double) x) / width * (-36)).get());
        g.drawRect(x, y, 1, 1);
      }
    }
    
    return image;
  }
}
