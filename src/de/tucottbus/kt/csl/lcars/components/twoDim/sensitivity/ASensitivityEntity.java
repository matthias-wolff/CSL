package de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity;

import static org.bridj.Pointer.allocateBytes;
import static org.bridj.Pointer.pointerToDoubles;
import static org.bridj.Pointer.pointerToFloats;

import java.awt.image.BufferedImage;

import javax.vecmath.Point3d;

import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLMem.Usage;
import com.nativelibs4java.opencl.CLQueue;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.lcars.components.twoDim.SensitivityPlot.CLState;

/**
 * Abstract class for all OpenCL entity of the sensitivity plot.
 * @author Martin Birth
 *
 */
abstract class ASensitivityEntity {

  protected CLContext context;
  protected CLQueue queue;
  private final SensitivityKernels kernels;
  
  protected CLState clstate;

  /**
   * 
   * @param kernels
   */
  public ASensitivityEntity(SensitivityKernels kernels, CLState clstate) {
    this.kernels = kernels;
    this.context = kernels.getProgram().getContext();
    this.queue = kernels.getProgram().getContext()
        .createDefaultOutOfOrderQueue();
    this.clstate=clstate;
  }

  /**
   * Get an OpenCl buffer of all microphone positions
   * 
   * @param byteOrder
   *          - ByteOrder
   * @return CLBuffer<Double>
   */
  private CLBuffer<Double> getOpenClMicPositionBuffer(Point3d[] positions) {
    double[] pos = new double[positions.length * 3];
    for (int i = 0; i < positions.length; i++) {
      pos[i * 3] = positions[i].getX();
      pos[i * 3 + 1] = positions[i].getY();
      pos[i * 3 + 2] = positions[i].getZ();
    }
    Pointer<Double> micsPointer = pointerToDoubles(pos);
    return context.createBuffer(Usage.Input, micsPointer,true);
  }

  /**
   * Get an OpenCL activeMics buffer
   * 
   * @param activeMics
   * @return CLBuffer<Byte>
   */
  private CLBuffer<Byte> getOpenClActiveMicsBuffer(boolean[] activeMics) {
    Pointer<Byte> micsActivePointer = allocateBytes(activeMics.length).order(
        context.getKernelsDefaultByteOrder());
    for (int i = 0; i < activeMics.length; i++) {
      micsActivePointer.set(i, activeMics[i] ? (byte) 1 : (byte) 0);
    }
    return context.createByteBuffer(Usage.Input, micsActivePointer,true);
  }

  /**
   * Get sensitivity image from openCL kernel.
   * 
   * @param state - {@link MicArrayState}
   * @param freq - int, frequency of the representation
   * @param sliceSelect <br>
   *          int - selected slice <br>
   *          1 := horizontal slice <br>
   *          2 := vertical front slice <br>
   *          3 := vertical side slice
   * @param slicePos - int, position of the slice
   * @param imgW - int, image width
   * @param imgH - int, image height
   * @return BufferedImage
   */
  protected BufferedImage getImageFromKernel(MicArrayState state, float freq,
      int sliceSelect, int slicePos, int imgW, int imgH) {
    BufferedImage img = null;
    
    // allocate all buffers
    CLBuffer<Double> micPosBuff = getOpenClMicPositionBuffer(state.positions);
    CLBuffer<Byte> micsActiveBuff = getOpenClActiveMicsBuffer(state.activeMics);
    CLBuffer<Float> steerBuff = context.createBuffer(Usage.Input,
        pointerToFloats(state.steerVec), true);
    
    int[] globalWorkSizes = new int[] { imgW , imgH };
    CLEvent event = null;
    
    // running the kernel with image support
    if (clstate==CLState.CL_IMAGE_2D) {
      BufferedImage image = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
      CLImage2D outImg = context.createImage2D(Usage.Output, image, false);
      event = kernels.updateImageParams(queue, micPosBuff, micsActiveBuff, steerBuff, freq, sliceSelect, slicePos, outImg, globalWorkSizes);
      
      synchronized(this){
        img = outImg.read(queue, event);
      }
      
      outImg.release();
    } else {
      // running the kernel without image support and generate a int[] array with RGB values
      CLBuffer<Integer> outputBuffer = context.createBuffer(Usage.Output,Integer.class, imgW * imgH);
      event = kernels.updateIntArrayParams(queue, micPosBuff, micsActiveBuff,
          steerBuff, freq, sliceSelect, slicePos, imgW, imgH, outputBuffer, globalWorkSizes);
      
      Pointer<Integer> outPtr;
      synchronized(this){
        outPtr = outputBuffer.read(queue, event);
      }
      
      int[] rgbaData = outPtr.getInts();
      img = SensitivityUtils.convertingIntPixelImageToBufferedImage(rgbaData, imgW, imgH);
      outPtr.release();
      outputBuffer.release();
    }
    
    // release all buffers
    if(event!=null)
      event.release();
    micPosBuff.release();
    micsActiveBuff.release();
    steerBuff.release();
    
    return img;
  }
  
  /**
   * Get sensitivity int[] array with RGBA values from openCL kernel.
   * 
   * @param state - {@link MicArrayState}
   * @param freq - int, frequency of the representation
   * @param sliceSelect <br>
   *          int - selected slice <br>
   *          1 := horizontal slice <br>
   *          2 := vertical front slice <br>
   *          3 := vertical side slice
   * @param slicePos - int, position of the slice
   * @param imgW - int, image width
   * @param imgH - int, image height
   * @return int[] array
   */
  protected int[] getIntArrayFromKernel(MicArrayState state, float freq,
      int sliceSelect, int slicePos, int imgW, int imgH) {

    // allocate all buffers
    CLBuffer<Double> micPosBuff = getOpenClMicPositionBuffer(state.positions);
    CLBuffer<Byte> micsActiveBuff = getOpenClActiveMicsBuffer(state.activeMics);
    CLBuffer<Float> steerBuff = context.createBuffer(Usage.Input,
        pointerToFloats(state.steerVec), true);
    
    int[] globalWorkSizes = new int[] { imgW , imgH };
    CLEvent event = null;
    
    CLBuffer<Integer> outputBuffer = context.createBuffer(Usage.Output,
        Integer.class, imgW * imgH);
    event = kernels.updateIntArrayParams(queue, micPosBuff, micsActiveBuff,
        steerBuff, freq, sliceSelect, slicePos, imgW, imgH, outputBuffer, globalWorkSizes);
    
    Pointer<Integer> outPtr;
    synchronized(this){
      outPtr = outputBuffer.read(queue, event);
    }
    
    // release all buffers
    if(event!=null)
      event.release();
    micPosBuff.release();
    micsActiveBuff.release();
    steerBuff.release();
    
    int[] data = outPtr.getInts();
    outPtr.release();
    outputBuffer.release();
    return data;
  }

}
