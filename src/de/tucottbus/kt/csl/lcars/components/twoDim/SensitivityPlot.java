package de.tucottbus.kt.csl.lcars.components.twoDim;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.vecmath.Point3d;

import com.nativelibs4java.opencl.JavaCL;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity.SensitivityCpu;
import de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity.SensitivityEntity;
import de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity.SensitivityKernels;
import de.tucottbus.kt.csl.lcars.components.twoDim.sensitivity.SensitivityUtils;

/**
 * This class generates a different plots of the current microphone array configuration.
 * 
 * @author Martin Birth
 */
public class SensitivityPlot {
  /**
   * Dimensions of the room
   */
  private static final int FIELD_WIDTH = 440;
  private static final int FIELD_HEIGTH = 250;
  
  private final Point3d slicePos = new Point3d(0,0,160);
  
  public enum CLState {
    NO_CL,
    CL_BUFFER,
    CL_IMAGE_2D;
  }
  
  private static CLState _clstate;
  
  private SensitivityKernels kernels = null;
  
  private static volatile SensitivityPlot instance;
  
  /**
   * Constructor method
   */
  private SensitivityPlot()
  {
    if (_clstate == CLState.NO_CL)
      return;

    boolean doubleSupport = JavaCL.createBestContext().createDefaultQueue()
        .getContext().getDevices()[0].isDoubleSupported();

    if (!doubleSupport)
    {
      if (_clstate != CLState.NO_CL)
        _clstate = CLState.NO_CL;
    } 
    else
    {
      boolean imgSupport = JavaCL.createBestContext().createDefaultQueue()
          .getContext().getDevices()[0].hasImageSupport();
      if (!imgSupport)
        _clstate = CLState.CL_BUFFER;

      try
      {
        kernels = new SensitivityKernels(
            JavaCL.createBestContext().createDefaultQueue().getContext());
      } 
      catch (IOException e)
      {
        e.printStackTrace();
      }
      
      // TODO: Create SensitivityEntity only once (leaks memory!)
    }
  }
    
  /**
   * Default singleton method to get a instance with the {@link CLState#CL_IMAGE_2D} support.
   * 
   * @deprecated Use {@link #getInstance()} instead!
   */
  @Deprecated
  public static synchronized SensitivityPlot getInstance(CLState clstate){
    _clstate=clstate;
    if (instance == null) {
      instance = new SensitivityPlot();
    }
    return instance;
  }
  
  public static synchronized SensitivityPlot getInstance(){
    return getInstance(CLState.CL_IMAGE_2D);
  }
  
  /**
   * Creating a horizontal plots indicating the sensitivity distribution from the microphone array.
   * <br><br>
   * Please run before {@link #update(MicArrayState, Point3d, float)}
   * @param width - int, width of the image
   * @param height - int, height of the image
   * @return BufferedImage
   * @see #update(MicArrayState, Point3d, float)
   */
  public BufferedImage getHorizontalSensitivityImage(MicArrayState state, Point3d slicePos, float frequency, int width, int height) {
    int zSlicePos = (int)checkSlicePointConsistence(slicePos).getZ();
    if(_clstate==CLState.NO_CL)
    {
      return SensitivityCpu.getInstance().getHorizontalSliceImage(state, zSlicePos, frequency, width, height);
    } else {
      SensitivityEntity sens = new SensitivityEntity(kernels,_clstate);
      return sens.getHorizontalSliceImage(state, zSlicePos, frequency, width, height);
    }
  }
  
  /**
   * Creating a vertical plots indicating the sensitivity distribution from the microphone array.
   * <br><br>
   * Please run before {@link #update(MicArrayState, Point3d, float)}
   * @param width - int, width of the image
   * @param height - int, height of the image
   * @return BufferedImage
   * @see #update(MicArrayState, Point3d, float)
   */
  public BufferedImage getVerticalFrontSliceImage(MicArrayState state, Point3d slicePos, float frequency, int width, int height){
    int ySlicePos = (int)checkSlicePointConsistence(slicePos).getY();
    if(_clstate==CLState.NO_CL)
    {
      return SensitivityCpu.getInstance().getVerticalFrontImage(state, ySlicePos, frequency, width, height);
    } else {
      SensitivityEntity sens = new SensitivityEntity(kernels,_clstate);
      return sens.getVerticalFrontSliceImage(state, ySlicePos, frequency, width, height);
    }
  }
  
  /**
   * Creating a vertical plots indicating the sensitivity distribution from the microphone array.
   * <br><br>
   * Please run before {@link #update(MicArrayState, Point3d, float)}
   * @param width - int, width of the image
   * @param height - int, height of the image
   * @return BufferedImage
   * @see #update(MicArrayState, Point3d, float)
   */
  public BufferedImage getVerticalLateralSliceImage(MicArrayState state, Point3d slicePos, float frequency, int width, int height){
    int xSlicePos = (int)checkSlicePointConsistence(slicePos).getX();
    if(_clstate==CLState.NO_CL)
    {
      return SensitivityCpu.getInstance().getVerticalLateralSliceImage(state, xSlicePos, frequency, width, height);
    } else {
      SensitivityEntity sens = new SensitivityEntity(kernels,_clstate);
      return sens.getVerticalLateralSliceImage(state, xSlicePos, frequency, width, height);
    }
  }
  
  /**
   * Creating a horizontal plots indicating the sensitivity distribution from the microphone array.
   * <br><br>
   * Please run before {@link #update(MicArrayState, Point3d, float)}
   * @param width - int, width of the image
   * @param height - int, height of the image
   * @return int[] - pixel array with an BGRA color model
   * @see #update(MicArrayState, Point3d, float)
   */
  public int[] getHorizontalSensitivityIntArray(MicArrayState state, Point3d slicePos, float frequency, int width, int height) {  
    int zSlicePos = (int)checkSlicePointConsistence(slicePos).getZ();
    if(_clstate==CLState.NO_CL)
    {
      return SensitivityCpu.getInstance().getHorizontalSliceIntArray(state, zSlicePos, frequency, width, height);
    } else {
      SensitivityEntity sens = new SensitivityEntity(kernels,_clstate);
      return sens.getHorizontalSliceIntArray(state, zSlicePos, frequency, width, height);
    }
  }
  
  /**
   * Creating a vertical plots indicating the sensitivity distribution from the microphone array.
   * <br><br>
   * Please run before {@link #update(MicArrayState, Point3d, float)}
   * @param width - int, width of the image
   * @param height - int, height of the image
   * @return int[] - pixel array with an BGRA color model
   * @see #update(MicArrayState, Point3d, float)
   */
  public int[] getVerticalFrontSliceIntArray(MicArrayState state, Point3d slicePos, float frequency, int width, int height){
    int ySlicePos = (int)checkSlicePointConsistence(slicePos).getY();
    if(_clstate==CLState.NO_CL)
    {
      return SensitivityCpu.getInstance().getVerticalFrontIntArray(state, ySlicePos, frequency, width, height);
    } else {
      SensitivityEntity sens = new SensitivityEntity(kernels,_clstate);
      return sens.getVerticalFrontSliceIntArray(state, ySlicePos, frequency, width, height);
    }
  }
  
  /**
   * Creating a vertical plots indicating the sensitivity distribution from the microphone array.
   * <br><br>
   * Please run before {@link #update(MicArrayState, Point3d, float)}
   * @param width - int, width of the image
   * @param height - int, height of the image
   * @return int[] - pixel array with an BGRA color model
   * @see #update(MicArrayState, Point3d, float)
   */
  public int[] getVerticalLateralSliceIntArray(MicArrayState state, Point3d slicePos, float frequency, int width, int height){
    int xSlicePos = (int)checkSlicePointConsistence(slicePos).getX();
    if(_clstate==CLState.NO_CL)
    {
      return SensitivityCpu.getInstance().getVerticalLateralIntArray(state, xSlicePos, frequency, width, height);
    } else {
      SensitivityEntity sens = new SensitivityEntity(kernels,_clstate);
      return sens.getVerticalLateralSliceIntArray(state, xSlicePos, frequency, width, height);
    }
  }
  
  /**
   * Get the a vertical level scale image
   * 
   * @param width
   *          int: image width
   * @param height
   *          int: image height
   * @return BufferedImage
   */
  public BufferedImage getVerticalScaleImage(int width, int height){
    return SensitivityUtils.getVerticalLevelScale(width, height);
  }
  
  /**
   * Get the a horizontal level scale image
   * 
   * @param width
   *          int: image width
   * @param height
   *          int: image height
   * @return BufferedImage
   */
  public BufferedImage getHorizontalScaleImage(int width, int height) {
    return SensitivityUtils.getHorizontalLevelScale(width, height);
  }
  
  /**
   * Get the current positions of the three slices.
   * @return Point3d <br>
   *        Point3d().getX() - position of the vertical slice on x axis<br>
   *        Point3d().getY() - position of the vertical slice on y axis<br>
   *        Point3d().getZ() - position of the horizontal slice on z axis
   *        
   */
  public Point3d getSlicePos(){
    return slicePos;
  }
  
  /**
   * Check whether the point lies within the coordinate system.
   * 
   * @param Point3d in
   * @return
   */
  private Point3d checkSlicePointConsistence(Point3d in){
    Point3d ret = new Point3d();
    int zSliceMax = FIELD_HEIGTH;
    int sliceMax = FIELD_WIDTH/2;
    
    ret.setX((in.getX() <= -sliceMax) ? -sliceMax : ((in.getX() >= sliceMax) ? sliceMax : in.getX()));
    ret.setY((in.getY() <= -sliceMax) ? -sliceMax : ((in.getY() >= sliceMax) ? sliceMax : in.getY()));
    ret.setZ((in.getZ() <= 0) ? 0 : ((in.getZ() >= zSliceMax) ? zSliceMax : in.getZ()));
    
    return ret;
  }
  
  /**
   * This is true, if CL_BUFFER or CL_IMAGE_2D is active
   * @return
   */
  public boolean isOpenCLActive() {
    if(_clstate==CLState.CL_BUFFER || _clstate==CLState.CL_IMAGE_2D)
      return true;
    else {
      return false;
    }
  }
  
  /**
   * Get the {@link CLState} object flag from this class
   * @return
   */
  public CLState getCLState(){
    return _clstate;
  }
}
