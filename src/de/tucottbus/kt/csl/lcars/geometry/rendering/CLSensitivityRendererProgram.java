package de.tucottbus.kt.csl.lcars.geometry.rendering;

import java.io.IOException;

import com.nativelibs4java.opencl.CLAbstractUserProgram;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLQueue;

/**
 * OpenCL user program rendering 2D spatial sensitivity plots of the CSL
 * microphone array. The program comprises two {@linkplain CLKernel openCL
 * kernels}, one for rendering to an {@linkplain #imageKernel openCL 2D image}
 * and the other for rendering to an {@linkplain #intArrayKernel integer array}.
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class CLSensitivityRendererProgram extends CLAbstractUserProgram
{
  // -- Fields --
  
  /**
   * OpenCL kernel for rendering to a {@link CLImage2D 2D image}.
   */
  private CLKernel imageKernel;
  
  /**
   * OpenCL kernel for rendering to an integer array.
   */
  private CLKernel intArrayKernel;

  /**
   * Flag indicating that this renderer program is released.
   */
  private boolean isReleased = false;
  
  // -- Life cycle --
  
  /**
   * Creates a sensitivity rendering program from an openCL context.
   * 
   * @param context
   *          The openCL context.
   * @throws IOException
   *          If openCL initialization failed.
   */
  public CLSensitivityRendererProgram(CLContext context) throws IOException
  {
    super(context, readRawSourceForClass(CLSensitivityRendererProgram.class));
  }

  /**
   * Releases CL resources.
   */
  public void release()
  {
    isReleased = true;
    if (imageKernel!=null)
    {
      imageKernel.release();
      imageKernel = null;
    }
    if (intArrayKernel!=null)
    {
      intArrayKernel.release();
      intArrayKernel = null;
    }
  }
  
  // -- API --

  /**
   * Sets new rendering parameters for the {@link #imageKernel} rendering to a
   * {@link CLImage2D 2D image}.
   * 
   * @param commandQueue
   *          CLQueue with the command order.
   * @param micPosBuff
   *          The microphone positions in the CSL coordinate system as, e.g.,
   *          obtained by
   *          {@link CLUtils#point3dToBuffer(CLContext, javax.vecmath.Point3d[])
   *          CLUtils.getOpenClMicPositionBuffer(...)}
   * @param micsActiveBuff
   *          Array of 64 bytes indicating the activation states of the
   *          microphones (0: off, 1: on).
   * @param steerBuff
   *          Steering vector, one point in the CSL coordinate system.
   * @param freq
   *          The frequency to render the spatial sensitivity for in Hertz.
   * @param sliceType
   *          The slice type: {@link ISensitivityRenderer#SLICE_XY SLICE_XY},
   *          {@link ISensitivityRenderer#SLICE_XZ SLICE_XZ}, or
   *          {@link ISensitivityRenderer#SLICE_YZ SLICE_YZ}.
   * @param slicePos
   *          The position of the slice in the remaining direction of the CSL
   *          coordinate system, i.e. the z-coordinate of <code>sliceType</code>
   *          is {@link ISensitivityRenderer#SLICE_XY SLICE_XY}.
   * @param outImg
   *          The {@link CLImage2D 2D image} to render into.
   * @param globalWorkSizes
   *          <b style="color:red">?</b> Commit an integer array containing the
   *          width and height of the image to be rendered in pixels, e.g.
   *          <code>new int[] { imgW, imgH }</code>.
   * @param eventsToWaitFor
   *          -- <i>not used</i> --
   * @return OpenCL event indicating the progress of rendering.
   * @throws CLBuildException
   *           On rendering errors.
   */
  public synchronized CLEvent updateImageParams
  (
    CLQueue          commandQueue,
    CLBuffer<Double> micPosBuff, 
    CLBuffer<Byte>   micsActiveBuff,
    CLBuffer<Float>  steerBuff, 
    float            freq, 
    int              sliceType, 
    int              slicePos,
    CLImage2D        outImg, 
    int              globalWorkSizes[], 
    CLEvent...       eventsToWaitFor
  )
  throws CLBuildException, IllegalStateException
  {
    if (isReleased)
      throw new IllegalStateException("Renderer program is released.");
    
    if (imageKernel == null)
      imageKernel = createKernel("getSensitivityImage");

    imageKernel.setArgs(freq, sliceType, slicePos, micPosBuff, micsActiveBuff,
        steerBuff, outImg);

    return imageKernel.enqueueNDRange(commandQueue, globalWorkSizes,
        eventsToWaitFor);
  }
  
  /**
   * Sets new rendering parameters for the {@link #intArrayKernel} rendering to an
   * integer array.
   * 
   * @param commandQueue
   *          CLQueue with the command order.
   * @param micPosBuff
   *          The microphone positions in the CSL coordinate system as, e.g.,
   *          obtained by
   *          {@link CLUtils#point3dToBuffer(CLContext, javax.vecmath.Point3d[])
   *          CLUtils.getOpenClMicPositionBuffer(...)}
   * @param micsActiveBuff
   *          Array of 64 bytes indicating the activation states of the
   *          microphones (0: off, 1: on).
   * @param steerBuff
   *          Steering vector, one point in the CSL coordinate system.
   * @param freq
   *          The frequency to render the spatial sensitivity for in Hertz.
   * @param sliceType
   *          The slice type: {@link ISensitivityRenderer#SLICE_XY SLICE_XY},
   *          {@link ISensitivityRenderer#SLICE_XZ SLICE_XZ}, or
   *          {@link ISensitivityRenderer#SLICE_YZ SLICE_YZ}.
   * @param slicePos
   *          The position of the slice in the remaining direction of the CSL
   *          coordinate system, i.e. the z-coordinate of <code>sliceType</code>
   *          is {@link ISensitivityRenderer#SLICE_XY SLICE_XY}.
   * @param imgW
   *          The width of the image to render (in pixels).
   * @param imgH
   *          The height of the image to render (in pixels).
   * @param outputBuffer
   *          The buffer to render into.
   * @param globalWorkSizes
   *          <b style="color:red">?</b> Commit an integer array containing the
   *          width and height of the image to be rendered in pixels, e.g.
   *          <code>new int[] { imgW, imgH }</code>.
   * @param eventsToWaitFor
   *          -- <i>not used</i> --
   * @return OpenCL event indicating the progress of rendering.
   * @throws CLBuildException
   *           On rendering errors.
   */
  public synchronized CLEvent updateIntArrayParams
  (
    CLQueue           commandQueue,
    CLBuffer<Double>  micPosBuff, 
    CLBuffer<Byte>    micsActiveBuff, 
    CLBuffer<Float>   steerBuff,
    float             freq, 
    int               sliceType, 
    int               slicePos, 
    int               imgW, 
    int               imgH, 
    CLBuffer<Integer> outputBuffer, 
    int               globalWorkSizes[], 
    CLEvent...        eventsToWaitFor
  )
  throws CLBuildException, IllegalStateException
  {
    if (isReleased)
      throw new IllegalStateException("Renderer program is released.");

    if (intArrayKernel == null)
      intArrayKernel = createKernel("getSensitivityIntArray");

    intArrayKernel.setArgs(freq, sliceType, slicePos, imgW, imgH, micPosBuff,
        micsActiveBuff, steerBuff, outputBuffer);

    return intArrayKernel.enqueueNDRange(commandQueue, globalWorkSizes,
        eventsToWaitFor);
  }

}

// EOF