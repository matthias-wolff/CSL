package de.tucottbus.kt.csl.retired.lcars.components.threeDim;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.media.j3d.Canvas3D;
import javax.swing.JFrame;

import com.nativelibs4java.opencl.JavaCL;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.SensitivityPlot.CLState;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.sensitivity.SensitivityEntity;
import de.tucottbus.kt.csl.retired.lcars.components.twoDim.sensitivity.SensitivityKernels;
import de.tucottbus.kt.lcars.logging.Log;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.PlugIn;
import ij3d.Content;
import ij3d.Image3DUniverse;

/**
 * This class shows a 3d object of all sensitivity slices in the CSL.
 * 
 * @author Martin Birth
 */
@Deprecated
public class Viewer3d implements PlugIn {

  // ############ static fields #################

  private final String PATH = "C:/Users/wolff/Downloads/CSL_SensitivityCubeData/";

  private final String FILE = "CSL_SensitivityCube";

  private final String EXT = ".zip";

  /**
   * Room width
   */
  private final static int WIDTH = 440;

  /**
   * Room height
   */
  private final static int HEIGTH = 440;

  /**
   * Room length
   */
  private final static int LENGTH = 250;

  private final static int CANVAS_DIM = 950;
  
  // ############ non-static fields #################

  private static Image3DUniverse univ;

  private SensitivityKernels kernels = null;

  private Content content;

  private int threshold = 124;

  private ImagePlus imageStack = null;

  private float freq;
  
  // ############ running #################

  /**
   * Constructor
   */
  public Viewer3d() {
    if (IJ.getInstance() == null)
      new ij.ImageJ(ImageJ.NO_SHOW);
  }

  /**
   * Start imageJ plugin
   */
  public void startPlugin() {
    IJ.runPlugIn("de.tucottbus.kt.csl.lcars.components.threeDim.Viewer3d", "");
  }

  /**
   * Collecting the data for a three dimensional image projection.
   * 
   * @param frequency
   *          - float
   * @return ImagePlus
   * @see SensitivityPlot.getInstance(CLState.CL_BUFFER);
   */
  private ImagePlus generate3dImageData(float frequency) {
    MicArrayState state = MicArray3D.getInstance().getState();
    if (kernels==null)
    {
      try {
        kernels = new SensitivityKernels(JavaCL.createBestContext().createDefaultQueue().getContext());
      } catch (IOException e) {
        Log.err(e.getMessage(), e);
      }
    }

    ImageStack stack = new ImageStack(WIDTH, HEIGTH);
    SensitivityEntity sens = new SensitivityEntity(kernels, CLState.CL_BUFFER);
    for (int i = LENGTH; i > 0; i--) {
      int[] slice = sens.getHorizontalSliceIntArray(state, i, frequency, WIDTH,
          HEIGTH);
      convertBGRAtoARGB(slice);
      stack.addSlice("" + i, slice);
    }

    ImagePlus imp = new ImagePlus("SensitivityCube", stack);
    imp.setAntialiasRendering(true);
    return imp;
  }

  private void convertBGRAtoARGB(int[] colorInt) {
    for (int j = 0; j < colorInt.length; j++) {
      int a = (colorInt[j] & 0x000000ff) << 24;
      int r = (colorInt[j] & 0x0000ff00) << 8;
      int g = (colorInt[j] & 0x00ff0000) >> 8;
      int b = ((colorInt[j] & 0xff000000) >> 24) & 0xff;
      colorInt[j] = a | r | g | b;
    }
  }

  /**
   * Reads all data from file.
   * 
   * @param path
   * @param extention
   * @return
   */
  private File[] getAllFilesFromPath(String path, String extention) {
    File dir = new File(path);

    return dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String filename) {
        return filename.endsWith(extention);
      }
    });
  }

  /**
   * Searches for data file on storage.
   * 
   * @param frequency
   * @return String - filename
   */
  private String searchForSensitivityCubeDataFile(int frequency) {
    File[] files = getAllFilesFromPath(PATH, EXT);

    for (File f : files) {
      if (f.getAbsolutePath().contains(FILE)
          && f.getAbsolutePath().contains("" + frequency))
        return f.getAbsolutePath();
    }

    return null;
  }

  /**
   * Reading data from a data, if it exists, or to generate the data and save it
   * to file.
   * 
   * @param frequency
   * @return ImagePlus
   * @see #generate3dImageData(float)
   * 
   */
  protected ImagePlus getImageData(float frequency) {
    this.freq = frequency;
    String loadPath = searchForSensitivityCubeDataFile((int) frequency);

    ImagePlus imp;
    if (loadPath != null) {
      imp = IJ.openImage(loadPath);
      if (imp != null) {
        Log.info("3D data of path (" + loadPath + ") has been loaded");
        return imp;
      }
      Log.err("3D files in the path (" + loadPath + ") does not exist.");
    }

    imp = generate3dImageData(frequency);
    String filepath = PATH + FILE + "_" + (int) frequency + EXT;
    IJ.saveAs(imp, EXT, filepath);
    Log.info("3D image data saved to " + filepath);
    return imp;
  }
  
  /**
   * 
   */
  @Override
  public void run(String arg) {
    univ = new Image3DUniverse(CANVAS_DIM, CANVAS_DIM);
    univ.addInteractiveBehavior(new Viewer3dCustomBehavior(univ));
    univ.setAutoAdjustView(true);
    
    setViewPoint();
    
    content = new Content("");
    content.displayAs(Content.VOLUME);
  }
 
  /**
   * Get the canvas of the Image3DUniverse
   * 
   * @return
   */
  public Canvas3D getCanvas() {
    return univ.getCanvas();
  }

  /**
   * Set the frequency value and update/generate the
   * the volume rendering.
   * 
   * @param freq
   *          - float
   */
  public void setFrequency(float freq) {
    if (imageStack == null || this.freq != freq)
      imageStack = getImageData(freq);
    content = univ.addVoltex(imageStack);
    content.setThreshold(threshold);
    content.setSaturatedVolumeRendering(true);
  }
  
  /**
   * Set the threshold valuem of the displayed data
   * 
   * @param th
   *          - int
   */
  public synchronized void setThreshold(int th) {
    threshold = th;
    if (univ == null)
      return;
    content.setThreshold(threshold);
  }

  public synchronized int getThreshold() {
    return threshold;
  }

  public boolean isActive() {  
    if(univ == null)
      return false;
    else {
      return true;
    }
  }

  /**
   * Start the rotation animation
   */
  public void startAnimation() {
    if (isActive())
      univ.startAnimation();
  }

  /**
   * Stop the rotation animation
   */
  public void stopAnimation() {
    if (isActive())
      univ.pauseAnimation();
  }
  
  public void lock() {
    if(isActive()) {
      univ.getSelected().setLocked(true);
    }
  }

  public void unlock() {
    if(isActive()) {
      univ.getSelected().setLocked(false);
    }
  }
  
  public void setCoordinateSystem(String s) {
    if(isActive()) {
      univ.getSelected().showCoordinateSystem(
        getBoolean(s));
    }
  }

  private boolean getBoolean(String s) {
    return new Boolean(s).booleanValue();
  }
  
  public void snapshot(String w, String h) {
    if(!isActive())
      return;

    int iw = Integer.parseInt(w);
    int ih = Integer.parseInt(h);
    univ.takeSnapshot(iw, ih).show();
  }
  
  /**
   * Reset the viewpoint
   */
  public void resetView() {
    if(!isActive())
      return;
    univ.resetView();
    //setViewPoint(); // TODO: set the init viewpoint
  }
  
  /**
   * Set the initialized point of view
   */
  private void setViewPoint(){
    univ.rotateY(Math.toRadians(30));
    univ.rotateX(Math.toRadians(90));
    univ.rotateY(Math.toRadians(15));
    univ.fireTransformationUpdated();
    univ.fireTransformationFinished();
  }

  /**
   * Disposing the viewer3d
   */
  public void dispose() {
    univ.close();
    try {
      this.finalize();
    } catch (Throwable e) {
      Log.err(e.getMessage(), e);
    }
  }

  // -- for testings only --
  
  private void initFrame(Canvas3D canvas) {
    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setTitle("CognitiveSystemsLab 3D Sensitivity");
    frame.setSize(1000, 1000);
    frame.setLocationRelativeTo(null);
    frame.setLayout(new BorderLayout());

    if (frame != null && canvas != null) {
      frame.add(canvas);
      frame.setVisible(true);
    }
  }

  public static void main(String[] args) {
    Viewer3d viewer = new Viewer3d();
    viewer.startPlugin();
    for (int i = 1020; i < 1100; i++) {
      viewer.getImageData(i);
    }
    
    viewer.initFrame(viewer.getCanvas());
    viewer.setFrequency(1000);
    viewer.setThreshold(130);
    
    viewer.startAnimation();
  }

}
