package incubator.csl.lcars.micarr.geometry.rendering;

import java.awt.image.BufferedImage;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;

/**
 * Renderer interface for 2D spatial sensitivity plots of the CSL microphone
 * array.
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public interface ISensitivityRenderer extends ISensitivityRendererConstants
{
  
  /**
   * Renders a 2D sensitivity plot to a buffered image.
   * 
   * @param state
   *          The microphone array state.
   * @param freq
   *          The frequency to render the spatial sensitivity for (in Hz).
   * @param sliceType
   *          The slice type: {@link #SLICE_XY}, {@link #SLICE_XZ}, or
   *          {@link #SLICE_YZ}.
   * @param slicePos
   *          The position of the slice in the remaining direction of the CSL
   *          coordinate system, i.e. the z-coordinate of <code>sliceType</code>
   *          is {@link #SLICE_XY}.
   * @param imgW
   *          The width of the image to render (in pixels).
   * @param imgH
   *          The height of the image to render (in pixels).
   * @return The buffered image.
   * @throws IllegalStateException If the renderer is disposed.
   */
  public BufferedImage renderImage
  (
    MicArrayState state, 
    float         freq,
    int           sliceType, 
    int           slicePos, 
    int           imgW, 
    int           imgH
  ) throws IllegalStateException;
  
  /**
   * Renders a 2D sensitivity plot to an integer array of RGBA values.
   * 
   * @param state
   *          The microphone array state.
   * @param freq
   *          The frequency to render the spatial sensitivity for (in Hz).
   * @param sliceType
   *          The slice type: {@link #SLICE_XY}, {@link #SLICE_XZ}, or
   *          {@link #SLICE_YZ}.
   * @param slicePos
   *          The position of the slice in the remaining direction of the CSL
   *          coordinate system, i.e. the z-coordinate of <code>sliceType</code>
   *          is {@link #SLICE_XY}.
   * @param imgW
   *          The width of the image to render (in pixels).
   * @param imgH
   *          The height of the image to render (in pixels).
   * @return An array of RGBA-pixels arranged by scan-lines.
   * @throws IllegalStateException If the renderer is disposed.
   */
  public int[] renderIntArray
  (
    MicArrayState state, 
    float         freq,
    int           sliceType, 
    int           slicePos, 
    int           imgW, 
    int           imgH
  ) throws IllegalStateException;

}

// EOF