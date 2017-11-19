package de.tucottbus.kt.csl.lcars.geometry.rendering;

import static org.bridj.Pointer.pointerToFloats;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * OpenCL-based renderer for 2D spatial sensitivity plots of the CSL microphone
 * array.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Respect gains computing sensitivity plot
 *     </li>
 * </ul>
 * 
 * @author Martin Birth, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class CLSensitivityRenderer implements ISensitivityRenderer
{
  // -- Fields --
  
  /**
   * The openCL user program.
   */
  private CLSensitivityRendererProgram program;
  
  /**
   * The opneCL context.
   */
  private CLContext context;
  
  /**
   * The openCL queue.
   */
  private CLQueue queue;

  // -- Life cycle --
  
  /**
   * The singleton instance.
   */
  private volatile static CLSensitivityRenderer singleton;
  
  /**
   * Returns the openCL-based renderer for 2D spatial sensitivity plots of the
   * CSL microphone array.
   * 
   * @throws IOException 
   *           If openCL is not available or initialization failed. In such
   *           cases the alternative {@link CpuSensitivityRenderer} may be used.
   */
  public synchronized static CLSensitivityRenderer getInstance() 
      throws IOException
  {
    if (singleton==null)
      singleton = new CLSensitivityRenderer();
    return singleton;
  }
  
  /**
   * Creates an new openCL-based renderer for 2D spatial sensitivity plots of
   * the CSL microphone array.
   * 
   * @throws IOException
   *           If openCL is not available or initialization failed. In such
   *           cases the alternative {@link CpuSensitivityRenderer} may be used.
   */
  private CLSensitivityRenderer() throws IOException
  {
    try
    {
      this.context = JavaCL.createBestContext().createDefaultQueue().getContext(); 
      this.program = new CLSensitivityRendererProgram(this.context);
      this.queue   = this.context.createDefaultOutOfOrderQueue();
      Log.info("Created openCL sensitivity plot renderer");
    }
    catch (IOException e)
    {
      Log.warn("No openCL rendering available.");
      throw e;
    }
  }

  @Override
  public void finalize()
  {
    Log.info("Finalizing openCL sensitivity plot renderer");
    
    if (queue!=null)
    {
      queue.release();
      queue = null;
    }
    if (context!=null)
    {
      context.release();
      context = null;
    }
    if (program!=null)
    {
      program.release();
      program = null;
    }
  }
  
  // -- Implementation of the ISensitivityRenderer interface --

  @Override
  public boolean usesCL()
  {
    return true;
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
  ) throws IllegalStateException
  {
    if (program==null || context==null || queue==null)
      throw new IllegalStateException("Renderer is disposed");
    
    BufferedImage img = null;
    int[] globalWorkSizes = new int[] { width , height };
    
    // Allocate buffers
    CLBuffer<Double> micPosBuff = CLUtils.point3dToBuffer(context,mas.positions);
    CLBuffer<Byte> micsActiveBuff = CLUtils.booleanToClBuffer(context,mas.activeMics);
    CLBuffer<Float> steerBuff = context.createBuffer(Usage.Input,pointerToFloats(mas.steerVec), true);

    if (CLUtils.hasImageSupport(context)) 
    {
      // Use kernel with image support
      BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      CLImage2D outImg = context.createImage2D(Usage.Output, image, false);
      CLEvent event 
        = program.updateImageParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceType, slicePos, outImg, globalWorkSizes);
      
      synchronized(this)
      {
        img = outImg.read(queue, event);
      }
      
      outImg.release();
      if (event!=null)
        event.release();
    } 
    else 
    {
      // Use kernel without image support and generate a int[] array with RGB values
      CLBuffer<Integer> outputBuffer = context.createBuffer(Usage.Output,Integer.class, width * height);
      Pointer<Integer> outPtr;
      CLEvent event 
        = program.updateIntArrayParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceType, slicePos, width, height, outputBuffer, globalWorkSizes);
      
      synchronized(this)
      {
        outPtr = outputBuffer.read(queue, event);
      }
      
      int[] rgbaData = outPtr.getInts();
      img = CLUtils.pixelsToBufferedImage(rgbaData, width, height);

      outPtr.release();
      outputBuffer.release();
      if (event!=null)
        event.release();
    }
    
    // Clean-up
    micPosBuff.release();
    micsActiveBuff.release();
    steerBuff.release();
    
    return img;
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
  ) throws IllegalStateException
  {
    if (program==null || context==null || queue==null)
      throw new IllegalStateException("Renderer is disposed");

    int[] globalWorkSizes = new int[] { width , height };

    // Allocate buffers
    CLBuffer<Double> micPosBuff = CLUtils.point3dToBuffer(context,mas.positions);
    CLBuffer<Byte> micsActiveBuff = CLUtils.booleanToClBuffer(context,mas.activeMics);
    CLBuffer<Float> steerBuff = context.createBuffer(Usage.Input, pointerToFloats(mas.steerVec), true);

    CLBuffer<Integer> outputBuffer 
      = context.createBuffer(Usage.Output, Integer.class, width * height);
    CLEvent event 
      = program.updateIntArrayParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceType, slicePos, width, height, outputBuffer, globalWorkSizes);

    // Do openCL rendering
    Pointer<Integer> outPtr;
    synchronized(this)
    {
      outPtr = outputBuffer.read(queue, event);
    }
    int[] pixels = outPtr.getInts();
    
    // Release all buffers
    if(event!=null)
      event.release();
    micPosBuff.release();
    micsActiveBuff.release();
    steerBuff.release();
    outPtr.release();
    outputBuffer.release();
    
    return pixels;
  }
  
}

// EOF
