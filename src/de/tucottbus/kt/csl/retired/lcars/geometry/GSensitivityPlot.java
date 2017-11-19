package de.tucottbus.kt.csl.retired.lcars.geometry;

import java.awt.Rectangle;
import java.awt.geom.Area;

import javax.vecmath.Point3d;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot.CLState;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.logging.Log;

@Deprecated
public class GSensitivityPlot extends AGeometry {
  private static final long serialVersionUID = 1L;

  public static final int ES_MASK = 0x00000003;
  public static final int ES_XY = 0x00000000;
  public static final int ES_YZ = 0x00000001;
  public static final int ES_XZ = 0x00000002;

  private final int orientation;
  
  private final int x;
  private final int y;
  private final int width;
  private final int height;
  
  private MicArrayState micArrayState;
  private double slicePos;
  private float frequency;
  private ImageData imageData;

  public GSensitivityPlot(MicArrayState micArrayState, double slicePos,
      float frequency, int orientation, Rectangle bounds) {
    super(false);
    this.orientation = orientation;
    this.x = bounds.x;
    this.y = bounds.y;
    this.width = bounds.width;
    this.height = bounds.height;
    this.micArrayState = micArrayState;
    this.slicePos = slicePos;
    this.frequency = frequency;
  }

  @Override
  public Area getArea() {
    return new Area(getBounds());
  }

  public void setSlicePos(double value) {
    slicePos = value;
  }
  
  public void setFrequency(float freq) {
    frequency=freq;
  }
  
  public void setMicArrayState(MicArrayState state){
    micArrayState = state;
  }

  @Override
  public void paint2D(GC gc) {
    if (imageData == null){
      Image img = new Image(gc.getDevice(), width, height);
      imageData = img.getImageData();
      img.dispose();
    }
    
    SensitivityPlot s = SensitivityPlot.getInstance(CLState.CL_BUFFER);
    Point3d pos3D = s.getSlicePos();
    int[] argbInts;
    switch (orientation & ES_MASK) {
      case ES_XY:
        pos3D.setZ(slicePos);
        argbInts = s.getHorizontalSensitivityIntArray(micArrayState, pos3D, frequency, width, height);
        break;
      case ES_YZ:
        pos3D.setX(slicePos);
        argbInts = s.getVerticalLateralSliceIntArray(micArrayState, pos3D, frequency, width, height);
        break;
      case ES_XZ:
        pos3D.setY(slicePos);
        argbInts = s.getVerticalFrontSliceIntArray(micArrayState, pos3D, frequency, width, height);
        break;
      default :
        Log.err("Image can not be drawn. BufferedImage is null.");
        return;
    }

    imageData.setPixels(0, 0, width*height, argbInts, 0);
    
    Image image = new Image(gc.getDevice(), imageData);
//    Rectangle b = this.bounds;      
//    double scale = Math.max(b.width / bi.getWidth(), b.height / bi.getHeight());
    
//    gc.drawImage(image, 0,0,imageData.width, imageData.height,
//                        b.x,b.y,(int)(imageData.width*scale),(int)(imageData.height*scale));

    gc.drawImage(image, x, y);

    image.dispose();
  }

  @Override
  public Rectangle getBounds() {
    return new Rectangle(x,y,width,height);
  }
    
  
}
