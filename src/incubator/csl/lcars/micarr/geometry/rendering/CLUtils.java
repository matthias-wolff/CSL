package incubator.csl.lcars.micarr.geometry.rendering;

import static org.bridj.Pointer.allocateBytes;
import static org.bridj.Pointer.pointerToDoubles;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteOrder;

import javax.vecmath.Point3d;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLMem.Usage;

/**
 * Static OpenCL-rendering utility functions.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class CLUtils
{
  /**
   * Detects openCL support of rendering to {@link CLImage2D 2D images}.
   * 
   * @param context
   *          The openCL context.
   * @return <code>true</code> of image rendering is supported,
   *         <code>false</code> otherwise.
   */
  public static boolean hasImageSupport(CLContext context)
  {
    return context.getDevices()[0].hasImageSupport();
  }
  
  /**
   * Converts and array 3D points to a openCL buffer of doubles.
   * <p style="color:red"><b>TODO:</b> Rename to <code>pos3dToClBuffer</code>!</p>
   * 
   * @param context
   *          The openCL context.
   * @param positions
   *          An array of 3D points.
   * @return An openCL buffer of doubles representing the positions.
   */
  public static CLBuffer<Double> getOpenClMicPositionBuffer
  (
    CLContext context,
    Point3d[] positions
  )
  {
    double[] pos = new double[positions.length*3];
    for (int i = 0; i < positions.length; i++)
    {
      pos[i*3] = positions[i].getX();
      pos[i*3 + 1] = positions[i].getY();
      pos[i*3 + 2] = positions[i].getZ();
    }
    Pointer<Double> pPos = pointerToDoubles(pos);
    return context.createBuffer(Usage.Input, pPos, true);
  }  
  
  /**
   * Converts an array of booleans to an openCL buffer of bytes.
   * <p style="color:red"><b>TODO:</b> Rename to <code>booleanToClBuffer</code>!</p>
   * 
   * @param context
   *          The openCL context.
   * @param activeMics
   *          An array of booleans.
   * @return An openCL buffer of bytes representing the booleans.
   */
  public static CLBuffer<Byte> getOpenClActiveMicsBuffer
  (
    CLContext context,
    boolean[] activeMics
  )
  {
    ByteOrder bo = context.getKernelsDefaultByteOrder();
    Pointer<Byte> pActiveMics = allocateBytes(activeMics.length).order(bo);
    for (int i = 0; i < activeMics.length; i++)
    {
      pActiveMics.set(i, activeMics[i] ? (byte) 1 : (byte) 0);
    }
    return context.createByteBuffer(Usage.Input, pActiveMics, true);
  }
  
  /**
   * Converts a integer RBGA-pixel array to a BufferdImage.
   * 
   * @param pixels
   *          Array of RGBA values arranged by scan-lines.
   * @param width
   *          The width of the image.
   * @param height
   *          The height of the image.
   * @return
   */
  public static BufferedImage pixelsToBufferedImage
  (
    int[] pixels, 
    int   width, 
    int   height
  ) 
  {
    BufferedImage image = new BufferedImage(width, height,
        BufferedImage.TYPE_3BYTE_BGR);

    Graphics2D g = image.createGraphics();
    image.setRGB(0, 0, width, height, pixels, 0, width);
    g.drawImage(image, 0, 0, null);
    g.dispose();

    return image;
  }
  
}

// EOF
