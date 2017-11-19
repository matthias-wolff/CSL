package de.tucottbus.kt.csl.lcars.geometry.rendering;

import java.awt.image.BufferedImage;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb.Steering;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * Renderer for 2D spatial sensitivity plots of the CSL microphone array. This
 * implementation uses the CPU and should only be used if
 * {@link CLSensitivityRenderer} is unavailable.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class CpuSensitivityRenderer implements ISensitivityRenderer
{
  // -- Constants --
  
  private final static double PI_DOUBLE = 2 * Math.PI;

  // -- Life cycle --
  
  /**
   * The singleton instance.
   */
  private volatile static CpuSensitivityRenderer singleton;
  
  /**
   * Returns the CPU-based renderer for 2D spatial sensitivity plots of the CSL
   * microphone array.
   */
  public synchronized static CpuSensitivityRenderer getInstance() 
  {
    if (singleton==null)
      singleton = new CpuSensitivityRenderer();
    return singleton;
  }
  
  /**
   * Creates a new CPU-based renderer for 2D spatial sensitivity plots of the CSL
   * microphone array.
   */
  private CpuSensitivityRenderer()
  {
    Log.info("Created CPU sensitivity plot renderer");
  } 
  
  // -- Implementation of the ISensitivityRenderer interface --
  
  @Override
  public boolean usesCL()
  {
    return false;
  }
  
  @Override
  public BufferedImage renderImage
  (
    MicArrayState mas, 
    float         freq,
    int           sliceType, 
    int           slicePos, 
    int           width, 
    int           height
  )
  {
    int[] pixels = renderIntArray(mas,freq,sliceType,slicePos,width,height);
    return CLUtils.pixelsToBufferedImage(pixels,width,height);
  }
  
  @Override
  public int[] renderIntArray
  (
    MicArrayState mas, 
    float         freq, 
    int           sliceType,
    int           slicePos, 
    int           width, 
    int           height
  )
  {
    switch (sliceType)
    {
    case SLICE_XY:
      return renderIntArray_XY(mas,slicePos,freq,width,height);
      
    case SLICE_XZ:
      return renderIntArray_XZ(mas,slicePos,freq,width,height);
      
    case SLICE_YZ:
      return renderIntArray_YZ(mas,slicePos,freq,width,height);
      
    default:
      throw new IllegalArgumentException("Invalid value of sliceType");
    }
  }

  // -- Workers --
  
  /**
   * Calculates the spatial sensitivity at a given coordinate. 
   * 
   * @param x
   *          x position in the CSL room coordinate system (in cm).
   * @param y
   *          y position in the CSL room coordinate system (in cm).
   * @param z
   *          z position in the CSL room coordinate system (in cm).
   * @return The sensitivity (in dB)
   */
  public static float getDB
  (
    MicArrayState state, 
    float         freq, 
    double        x, 
    double        y, 
    double        z
  ) 
  {
    double sumRe = 0;
    double sumIm = 0;
    double sum = 0;
    double delta = 0;
    double a_n = 1;
    int k = state.positions.length;

    for (int n = 0; n < state.positions.length; n++) 
    {
      if (state.activeMics[n] == false) 
      {
        k--;
        continue;
      }
      
      double tau = Steering.getDelayFromMicToPoint(state.positions[n], new Point3d(x,y,z));
      delta = PI_DOUBLE * freq * (state.steerVec[n] + tau);

      // TODO: Respect gains computing sensitivity plot
      //a_n=tau/steeringVector[n];

      sumRe += state.gains[n] * a_n * Math.cos(delta);
      sumIm += state.gains[n] * a_n * Math.sin(delta);
    }
    sum = Math.sqrt(sumRe * sumRe + sumIm * sumIm) / k;

    return (float)(20*Math.log10(sum));
  }
  
  private static int[] renderIntArray_XY
  (
    MicArrayState state,
    int           z, 
    float         freq, 
    int           imgW, 
    int           imgH
  ) 
  {
    int[] pixels = new int[imgW*imgH];
    
    for (int i = 0, y = 0; y < imgH; y++) 
      for (int x = 0; x < imgW; x++, i++) 
      {
        float db = getDB(state, freq, x-(imgW/2), -y+(imgH/2), z);
        pixels[i] = SensitivityColorScheme.dbToBGRA(db);
      }
    
    return pixels;
  }
    
  private static int[] renderIntArray_XZ
  (
    MicArrayState state, 
    int           y, 
    float         freq, 
    int           imgW, 
    int           imgH
  ) 
  {
    int[] pixels = new int[imgW*imgH];
    
    for (int i=0, z = 0; z < imgH; z++)
      for (int x = 0; x < imgW; x++, i++) 
      {
        float db = getDB(state, freq, x-(imgW/2), y, imgH-z);
        pixels[i] = SensitivityColorScheme.dbToBGRA(db);
      }

    return pixels;
  }

  private static int[] renderIntArray_YZ
  (
    MicArrayState state, 
    int           x, 
    float         freq, 
    int           imgW, 
    int           imgH
  ) 
  {
    int[] pixels = new int[imgW*imgH];

    for (int i = 0, z = 0; z < imgH; z++)
      for (int y = 0; y < imgW; y++, i++) 
      {
        float db = getDB(state, freq, x, y-(imgW/2), imgH-z);
        pixels[i] = SensitivityColorScheme.dbToBGRA(db);
      }

    return pixels;
  }

}

// EOF
