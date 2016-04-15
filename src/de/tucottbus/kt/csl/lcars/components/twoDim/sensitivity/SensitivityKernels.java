package de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity;

import java.io.IOException;

import com.nativelibs4java.opencl.CLAbstractUserProgram;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;

/**
 * This class contains the JavaCL kernel instances with the different update methods.
 * @author Martin Birth
 *
 */
public class SensitivityKernels extends CLAbstractUserProgram {
  
  /**
   * Kernel object for image2d javaCl implementation
   */
  private CLKernel imageKernel;
  
  /**
   * Kernel object for int buffer implementation
   */
  private CLKernel intArrayKernel;
  
  /**
   * Constructor using CLContext object
   * @param context, CLContext
   * @throws IOException
   */
  public SensitivityKernels(CLContext context) throws IOException {
    super(context, readRawSourceForClass(SensitivityKernels.class));
  }

  /**
   * Constructor using CLProgram object
   * @param program, CLProgram
   * @throws IOException
   */
  public SensitivityKernels(CLProgram program) throws IOException {
    super(program, readRawSourceForClass(SensitivityKernels.class));
  }

  /**
   * 
   * @param commandQueue, CLQueue with the command order
   * @param micPosBuff, CLBuffer<Double> with the microphone positions in 3d space
   * @param micsActiveBuff, CLBuffer<Byte> 
   * @param steerBuff, CLBuffer<Float> with the steering vector
   * @param freq, float value 
   * @param sliceSelect
   * @param slicePos
   * @param outImg
   * @param globalWorkSizes
   * @param eventsToWaitFor
   * @return CLEvent
   * @throws CLBuildException
   */
  public synchronized CLEvent updateImageParams(CLQueue commandQueue,
      CLBuffer<Double> micPosBuff, CLBuffer<Byte> micsActiveBuff, CLBuffer<Float> steerBuff,
      float freq, int sliceSelect, int slicePos, CLImage2D outImg,
      int globalWorkSizes[], CLEvent... eventsToWaitFor) throws CLBuildException {
    
    if (imageKernel == null)
      imageKernel = createKernel("getSensitivityImage");
    
    imageKernel.setArgs(freq, sliceSelect, slicePos, micPosBuff, micsActiveBuff, steerBuff , outImg);
    
    return imageKernel.enqueueNDRange(commandQueue, globalWorkSizes, eventsToWaitFor);
  }
  
  /**
   * 
   * @param commandQueue
   * @param micPosBuff
   * @param micsActiveBuff
   * @param steerBuff
   * @param freq
   * @param sliceSelect
   * @param slicePos
   * @param imgW
   * @param imgH
   * @param outputBuffer
   * @param globalWorkSizes
   * @param eventsToWaitFor
   * @return CLEvent
   * @throws CLBuildException
   */
  public synchronized CLEvent updateIntArrayParams(CLQueue commandQueue,
      CLBuffer<Double> micPosBuff, CLBuffer<Byte> micsActiveBuff, CLBuffer<Float> steerBuff,
      float freq, int sliceSelect, int slicePos, int imgW, int imgH, CLBuffer<Integer> outputBuffer, 
      int globalWorkSizes[], CLEvent... eventsToWaitFor)
      throws CLBuildException {
    
    if (intArrayKernel == null)
      intArrayKernel = createKernel("getSensitivityIntArray");
    
    intArrayKernel.setArgs(freq, sliceSelect, slicePos, imgW, imgH, micPosBuff, micsActiveBuff, steerBuff, outputBuffer);
    
    return intArrayKernel.enqueueNDRange(commandQueue, globalWorkSizes, eventsToWaitFor);
  }

}
