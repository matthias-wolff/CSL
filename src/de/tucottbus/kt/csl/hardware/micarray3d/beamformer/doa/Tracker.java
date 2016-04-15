package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa;

import java.util.ArrayList;
import java.util.HashSet;

import javax.vecmath.Point2d;

import org.jfree.data.xy.XYSeries;

import Jama.Matrix;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator.MicArray;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils.Plotter;


public class Tracker {
  
  // ################### static fields #######################
  
  /**
   * Set this to true, for debugging mode
   */
  private final static boolean DEBUG = Boolean.FALSE;
  
  // ################### non-static fields ###################
  
  private final HashSet<Point2d> xzHashSet = new HashSet<Point2d>();
  
  private final HashSet<Point2d> xyHashSet = new HashSet<Point2d>();
  
  private static Tracker instance;
  
  // ################### lifetime methods ###################
  
  /**
   * private constructor -> please use {@link #getInstance()}
   */
  private Tracker(){
    
  }
  
  /**
   * Get the singleton instance of the tracker
   * @return
   */
  public static synchronized Tracker getInstance()
  {
    if (instance==null){
      instance = new Tracker();
    }
    return instance;
  }
  
  /**
   * Add a point to the hash map
   * @param target
   * @param array {@link MicArray}
   */
  public void addPoint(Matrix target, MicArray array){
    if(array==MicArray.ARRAY1){
      xzHashSet.add(new Point2d(target.get(0, 0),target.get(2, 0)));
    } else {
      xyHashSet.add(new Point2d(target.get(0, 0),target.get(1, 0)));
    }
  }
  
  // ####################### Methods for debugging ############################
  
  private static Plotter plotter = new Plotter();
  private static XYSeries scatterXZSeries = new XYSeries("Scatter XZ Plot");
  private static XYSeries scatterXYSeries = new XYSeries("Scatter XZ Plot");
  
  public static void main(String[] args) {
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          RmeHdspMadi.getInstance();
          DoAEstimator.getInstance();
          
          if (DEBUG) {
            ArrayList<XYSeries> serieses = new ArrayList<XYSeries>();
            serieses.add(scatterXZSeries);
            serieses.add(scatterXYSeries);
            plotter.initFrame(serieses,AudioInputConstants.SAMPLERATE);
          }
          
        }
      });
      thread.setName("DoA-Test-Thread");
      thread.start();
  }
}
