package de.tucottbus.kt.csl.retired.lcars.components.twoDim.sensitivity;

import java.awt.image.BufferedImage;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot.CLState;

/**
 * Entity to create a single slice image
 * @author Martin Birth
 *
 */
@Deprecated
public class SensitivityEntity extends ASensitivityEntity{

  /**
   * Constructor
   * @param kernels SensitivityKernels
   */
  public SensitivityEntity(SensitivityKernels kernels){
    this(kernels, CLState.CL_IMAGE_2D);
  }
  
  /**
   * Constructor
   * @param kernels SensitivityKernels
   */
  public SensitivityEntity(SensitivityKernels kernels, CLState clState) {
    super(kernels, clState);
  }
  
  /**
   * Creating a horizontal plot indicating the sensitivity distribution from the microphone array.
   * 
   * @param state - {@link MicArrayState}
   * @param zSlicePos - Double, position of the horizontal slice on z axis
   * @param freq - float, representing frequency
   * @param imgW - int, width of the image
   * @param imgH - int, height of the image
   * @return BufferedImage
   */
  public BufferedImage getHorizontalSliceImage(MicArrayState state, int zSlicePos, float frequency, int imgW, int imgH){
    return getImageFromKernel(state, frequency, 1, zSlicePos, imgW, imgH);
  }

  /**
   * Creating a vertical plot indicating the sensitivity distribution from the microphone array.
   * 
   * @param state - {@link MicArrayState}
   * @param ySlicePos - Double, position of the vertical slice on y axis
   * @param freq - float, representing frequency
   * @param imgW - int, width of the image
   * @param imgH - int, height of the image
   * @return BufferedImage
   */
  public BufferedImage getVerticalFrontSliceImage(MicArrayState state, int ySlicePos, float frequency, int imgW, int imgH) {
    return getImageFromKernel(state, frequency, 2, ySlicePos, imgW, imgH);
  }

  /**
   * Creating a vertical plot indicating the sensitivity distribution from the microphone array.
   * 
   * @param state - {@link MicArrayState}
   * @param xSlicePos - Double, position of the vertical slice on x axis
   * @param freq - float, representing frequency
   * @param imgW - int, width of the image
   * @param imgH - int, height of the image
   * @return
   */
  public BufferedImage getVerticalLateralSliceImage(MicArrayState state, int xSlicePos, float frequency, int imgW, int imgH) {
    return getImageFromKernel(state, frequency, 3, xSlicePos, imgW, imgH);
  }
  
  /**
   * Creating a horizontal plot indicating the sensitivity distribution from the microphone array.
   * 
   * @param state - {@link MicArrayState}
   * @param zSlicePos - Double, position of the horizontal slice on z axis
   * @param freq - float, representing frequency
   * @param imgW - int, width of the image
   * @param imgH - int, height of the image
   * @return int[] - pixel array with an BGRA color model
   */
  public int[] getHorizontalSliceIntArray(MicArrayState state, int zSlicePos, float frequency, int imgW, int imgH){
    return getIntArrayFromKernel(state, frequency, 1, zSlicePos, imgW, imgH);
  }

  /**
   * Creating a vertical plot indicating the sensitivity distribution from the microphone array.
   * 
   * @param state - {@link MicArrayState}
   * @param ySlicePos - Double, position of the vertical slice on y axis
   * @param freq - float, representing frequency
   * @param imgW - int, width of the image
   * @param imgH - int, height of the image
   * @return int[] - pixel array with an BGRA color model
   */
  public int[] getVerticalFrontSliceIntArray(MicArrayState state, int ySlicePos, float frequency, int imgW, int imgH) {
    return getIntArrayFromKernel(state, frequency, 2, ySlicePos, imgW, imgH);
  }

  /**
   * Creating a vertical plot indicating the sensitivity distribution from the microphone array.
   * 
   * @param state - {@link MicArrayState}
   * @param xSlicePos - Double, position of the vertical slice on x axis
   * @param freq - float, representing frequency
   * @param imgW - int, width of the image
   * @param imgH - int, height of the image
   * @return int[] - pixel array with an BGRA color model
   */
  public int[] getVerticalLateralSliceIntArray(MicArrayState state, int xSlicePos, float frequency, int imgW, int imgH) {
    return getIntArrayFromKernel(state, frequency, 3, xSlicePos, imgW, imgH);
  }
}
