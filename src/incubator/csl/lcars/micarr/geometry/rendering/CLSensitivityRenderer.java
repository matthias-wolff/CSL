package incubator.csl.lcars.micarr.geometry.rendering;

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

/**
 * OpenCL-based renderer for 2D spatial sensitivity plots of the CSL microphone
 * array.
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
   * Creates an new openCL-based renderer for 2D spatial sensitivity plots of
   * the CSL microphone array.
   * 
   * <p>NOTE: Applications must invoke {@link #dispose()} when finished with the
   * renderer in order to free CL resources.</p>
   * 
   * @throws IOException
   *           If openCL is not available or initialization failed. In such
   *           cases the alternative {@link CpuSensitivityRenderer} may be used.
   */
  public CLSensitivityRenderer() throws IOException
  {
    this.context = JavaCL.createBestContext().createDefaultQueue().getContext(); 
    this.program = new CLSensitivityRendererProgram(this.context);
    this.queue   = this.context.createDefaultOutOfOrderQueue();
  }

  @Override
  public void finalize()
  {
    System.err.println("CLSensitivityRenderer.finalize()");
    
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
  public BufferedImage renderImage
  (
    MicArrayState state, 
    float         freq,
    int           sliceSelect, 
    int           slicePos, 
    int           imgW, 
    int           imgH
  ) throws IllegalStateException
  {
    if (program==null || context==null || queue==null)
      throw new IllegalStateException("Renderer is disposed");
    
    BufferedImage img = null;
    int[] globalWorkSizes = new int[] { imgW , imgH };
    
    // Allocate buffers
    CLBuffer<Double> micPosBuff = CLUtils.getOpenClMicPositionBuffer(context,state.positions);
    CLBuffer<Byte> micsActiveBuff = CLUtils.getOpenClActiveMicsBuffer(context,state.activeMics);
    CLBuffer<Float> steerBuff = context.createBuffer(Usage.Input,pointerToFloats(state.steerVec), true);

    if (CLUtils.hasImageSupport(context)) 
    {
      // Use kernel with image support
      BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
      CLImage2D outImg = context.createImage2D(Usage.Output, image, false);
      CLEvent event 
        = program.updateImageParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceSelect, slicePos, outImg, globalWorkSizes);
      
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
      CLBuffer<Integer> outputBuffer = context.createBuffer(Usage.Output,Integer.class, imgW * imgH);
      Pointer<Integer> outPtr;
      CLEvent event 
        = program.updateIntArrayParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceSelect, slicePos, imgW, imgH, outputBuffer, globalWorkSizes);
      
      synchronized(this)
      {
        outPtr = outputBuffer.read(queue, event);
      }
      
      int[] rgbaData = outPtr.getInts();
      img = CLUtils.pixelsToBufferedImage(rgbaData, imgW, imgH);

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
    MicArrayState state, 
    float         freq,
    int           sliceSelect, 
    int           slicePos, 
    int           imgW, 
    int           imgH
  ) throws IllegalStateException
  {
    if (program==null || context==null || queue==null)
      throw new IllegalStateException("Renderer is disposed");

    int[] globalWorkSizes = new int[] { imgW , imgH };

    // Allocate buffers
    CLBuffer<Double> micPosBuff = CLUtils.getOpenClMicPositionBuffer(context,state.positions);
    CLBuffer<Byte> micsActiveBuff = CLUtils.getOpenClActiveMicsBuffer(context,state.activeMics);
    CLBuffer<Float> steerBuff = context.createBuffer(Usage.Input, pointerToFloats(state.steerVec), true);

    CLBuffer<Integer> outputBuffer 
      = context.createBuffer(Usage.Output, Integer.class, imgW * imgH);
    CLEvent event 
      = program.updateIntArrayParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceSelect, slicePos, imgW, imgH, outputBuffer, globalWorkSizes);

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
