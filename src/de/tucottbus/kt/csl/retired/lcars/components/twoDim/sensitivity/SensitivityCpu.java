package de.tucottbus.kt.csl.retired.lcars.components.twoDim.sensitivity;

import java.awt.image.BufferedImage;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb.Steering;

/**
 * @author Martin Birth
 */
@Deprecated
public class SensitivityCpu {
  
  private final static double PI_DOUBLE = 2 * Math.PI;
 
  private static SensitivityCpu instance;

  /**
   * Private constructor, please use singleton
   */
  private SensitivityCpu() {
  }

  /**
   * singleton method to get an single object of this class
   * 
   * @return Sensitivity
   */
  public static synchronized SensitivityCpu getInstance() {
    if (instance == null) {
      instance = new SensitivityCpu();
    }
    return instance;
  }
  
  /**
   * Calculating the distribution of sensitivity for a desired 3D target
   * position.
   * 
   * @param x
   *          X-Value of the target position.
   * @param y
   *          y-Value of the target position.
   * @param z
   *          Z-Value of the target position.
   * @return Logarithmic dB-value of a single point in space.
   */
  private double getDB(MicArrayState state, float freq, double x, double y, double z) {
    double sumRe = 0;
    double sumIm = 0;
    double sum = 0;
    double delta = 0;
    double a_n = 1;
    int k = state.positions.length;

    for (int n = 0; n < state.positions.length; n++) {
      if (state.activeMics[n] == false) {
        k--;
        continue;
      }
      
      double tau = Steering.getDelayFromMicToPoint(state.positions[n], new Point3d(x,y,z));
      delta = PI_DOUBLE * freq * (state.steerVec[n] + tau);

      // TODO: Gewichtungsfaktor
      //a_n=tau/steeringVector[n];

      sumRe += state.gains[n] * a_n * Math.cos(delta);
      sumIm += state.gains[n] * a_n * Math.sin(delta);
    }
    sum = Math.sqrt(sumRe * sumRe + sumIm * sumIm) / k;

    return 20 * Math.log10(sum);
  }
  
  public int[] getHorizontalSliceIntArray(MicArrayState state,
      int zSlicePos, float freq, int imgW, int imgH) {
    int[] pixel = new int[imgW * imgH];
    int i = 0;
    
    for (int y = 0; y < imgH; y++) {
      for (int x = 0; x < imgW; x++) {
        double val = getDB(state,freq,x-(imgW / 2),-y + (imgH / 2), zSlicePos);
        pixel[i]=SensitivityColorScheme.getIntColor(val);
        i++;
      }
    }
    
    return pixel;
  }
  
  public BufferedImage getHorizontalSliceImage(MicArrayState state, int zSlicePos, float frequency, int imgW, int imgH){
    int[] pixel = new int[imgW * imgH];
    int i = 0;
    
    for (int y = 0; y < imgH; y++) {
      for (int x = 0; x < imgW; x++) {
        double val = getDB(state,frequency,x-(imgW / 2),-y + (imgH / 2), zSlicePos);
        pixel[i]=SensitivityColorScheme.getColor3f(val).get().getRGB();
        i++;
      }
    }
    
    return SensitivityUtils.convertingIntPixelImageToBufferedImage(pixel, imgW, imgH);
  }
  
  public int[] getVerticalFrontIntArray(MicArrayState state, int ySlicePos, float frequency, int imgW, int imgH) {
    int[] pixel = new int[imgW * imgH];
    int i = 0;
    
    for (int z = 0; z < imgH; z++) {
      for (int x = 0; x < imgW; x++) {
        double val = getDB(state,frequency,x-(imgW/2), ySlicePos, imgH-z);
        pixel[i]=SensitivityColorScheme.getIntColor(val);
        i++;
      }
    }

    return pixel;
  }
  
  public BufferedImage getVerticalFrontImage(MicArrayState state, int ySlicePos, float frequency, int imgW, int imgH) {
    int[] pixel = new int[imgW * imgH];
    int i = 0;
    
    for (int z = 0; z < imgH; z++) {
      for (int x = 0; x < imgW; x++) {
        double val = getDB(state,frequency,x-(imgW/2), ySlicePos, imgH-z);
        pixel[i]=SensitivityColorScheme.getColor3f(val).get().getRGB();
        i++;
      }
    }
    
    return SensitivityUtils.convertingIntPixelImageToBufferedImage(pixel, imgW, imgH);
  }

  public int[] getVerticalLateralIntArray(MicArrayState state, int xSlicePos, float frequency, int imgW, int imgH) {
    int[] pixel = new int[imgW * imgH];
    int i = 0;

    for (int y = 0; y < imgH; y++) {
      for (int z = 0; z < imgW; z++) {
        pixel[i]=SensitivityColorScheme.getIntColor(getDB(state,frequency,xSlicePos, -y + (imgH / 2), z ));
        i++;
      }
    }

    return pixel;
  }
  
  public BufferedImage getVerticalLateralSliceImage(MicArrayState state, int xSlicePos, float frequency, int imgW, int imgH) {
    int[] pixel = new int[imgW * imgH];
    int i = 0;

    for (int y = 0; y < imgH; y++) {
      for (int z = 0; z < imgW; z++) {
        double val = getDB(state,frequency,xSlicePos, -y + (imgH / 2), z );
        pixel[i]=SensitivityColorScheme.getColor3f(val).get().getRGB();
        i++;
      }
    }
    
    return SensitivityUtils.convertingIntPixelImageToBufferedImage(pixel, imgW, imgH);
  }
  
}
