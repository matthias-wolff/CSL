package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.vecmath.Point3d;

import org.apache.commons.math3.complex.Complex;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.util.ShapeUtilities;

import de.tucottbus.kt.lcars.logging.Log;

/**
 * This class is for testings only. This is a plotter for audio data using the jfreecharts API.
 * @author Martin Birth
 *
 */
public class Plotter implements Runnable {

  private final boolean LOG_PLOT = Boolean.FALSE;
  
  JFrame frame;

  private final Queue<PlotData<?>> dataQueue = new ConcurrentLinkedQueue<PlotData<?>>();

  private int sampleRate;

  /**
   * Guard thread
   */
  private Thread guard = null;

  /**
   * thread boolean
   */
  private boolean runGuard = false;

  /**
   * Default constructor
   */
  public Plotter() {
    frame = new JFrame("Grapher");
  }

  /**
   * Constructor with custom panel name
   * 
   * @param name
   */
  public Plotter(String name) {
    frame = new ApplicationFrame(name);
  }

  /**
   * 
   * @param series
   */
  public void initFrame(ArrayList<XYSeries> serieses, int sampleRate) {
    this.sampleRate = sampleRate;
    
    frame.setPreferredSize(new Dimension(900, 1000));
    frame.getContentPane().setBackground(Color.WHITE);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
    // JFreeChart chart = createCombinedChart();
    // ChartPanel panel = new ChartPanel(chart, true, true, true, false,
    // false);

    for (XYSeries series : serieses) {
      if (series == null)
        continue;
      if(series.getKey().toString().contains("Scatter"))
        frame.add(getScatterXYSubplotPanel(series));
      else
        frame.add(getXYSubplotPanel(series));
    }

    frame.pack();
    frame.setVisible(true);
   
    guard = new Thread(this, getClass().getSimpleName() + ".guard");
    guard.start();
  }

  @Override
  public void run() {
    runGuard = true;
    final int sleepMillis = 10;
    int ctr = 0;

    while (runGuard) {
      try {
        // - Run connection
        while (runGuard) {
          try {
            Thread.sleep(sleepMillis);
          } catch (InterruptedException e) {
            Log.err(e.getMessage(), e);
          }
          if (!runGuard) {
            break;
          }

          if (isActive()) {
            runPlotter();
          }

          ctr += sleepMillis;
          if (ctr >= 1000) {
            ctr = 0;
          }
        }
      } catch (Exception e) {
        Log.err(e.getMessage(), e);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          Log.err(e.getMessage(), e1);
        }
      }
    }
  }
  
  private boolean isActive(){
    return frame.isVisible();
  }
  
  private void runPlotter() {
    if(dataQueue.isEmpty())
      return;
    
    PlotData<?> data = dataQueue.poll();
    XYSeries series = data.getSeries();
    
    if(series.getKey().toString().contains("Scatter")){
      if(data.getData() instanceof Point3d){
        Point3d p = (Point3d) data.getData();
        series.add(p.getX(), p.getZ());
      }
    }
    
    if(data.getData() instanceof Complex[]){
      Complex[] complexData = (Complex[])data.getData();
      plotComplexArray(complexData, series);
    }
    
    if(data.getData() instanceof double[]){
      double[] doubleData = (double[])data.getData();
      plotDoubleArray(doubleData, series);
    }
    
    if(data.getData() instanceof float[]){
      float[] floatData = (float[])data.getData();
      plotFloatArray(floatData, series);
    }
  }
  
  /**
   * 
   * @param data
   */
  public void addDataSet(PlotData<?> data){
    dataQueue.add(data);
  }

  /**
   * 
   * @param series
   * @param range
   * @return
   */
  private ChartPanel getXYSubplotPanel(XYSeries series) {
    XYDataset data = new XYSeriesCollection(series);
    SamplingXYLineRenderer renderer = new SamplingXYLineRenderer();

    NumberAxis xAxis = new NumberAxis();
    if (series.getKey().toString().contains("Freq") && LOG_PLOT) {
      xAxis = new LogarithmicAxis("Frequency");
      xAxis.setRange(0, 44100 / 2);
    } else {
      xAxis.setAutoRange(true);
    }
    
    NumberAxis yAxis = new NumberAxis();
    if (series.getKey().toString().contains("BLAAAAAA")) {
      yAxis.setRange(0, 1);
    } else {
      yAxis.setAutoRangeIncludesZero(true);
    }
    
    if(series.getKey().toString().contains("IFFT")){
      yAxis.setAttributedLabel("Amplitude");
      xAxis.setAttributedLabel("Samples");
    }

    XYPlot xysubplot = new XYPlot(data, xAxis, yAxis, renderer);
    xysubplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    xysubplot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
    
    renderer.setSeriesPaint(0, new Color(0,155,210));
    
    JFreeChart chart = new JFreeChart(series.getDescription(),
        JFreeChart.DEFAULT_TITLE_FONT, xysubplot, true);
    chart.setBackgroundPaint(Color.WHITE);
    
    ChartPanel chartPanel = new ChartPanel(chart, true, true, true, false, false);
    
    return chartPanel;
  }
  
  /**
   * 
   * @param series
   * @param range
   * @return
   */
  private ChartPanel getScatterXYSubplotPanel(XYSeries series) {
    XYSeriesCollection xySeriesCollection = new XYSeriesCollection();
    xySeriesCollection.addSeries(series);

    JFreeChart jfreechart = ChartFactory.createScatterPlot(series.getDescription(),
        "X", "Z", xySeriesCollection, PlotOrientation.VERTICAL, true, true,
        false);

    Shape cross = ShapeUtilities.createDiagonalCross(1, 1);
    XYPlot xyPlot = (XYPlot) jfreechart.getPlot();
    xyPlot.setDomainCrosshairVisible(true);
    xyPlot.setRangeCrosshairVisible(true);
    XYItemRenderer renderer = xyPlot.getRenderer();
    renderer.setSeriesShape(0, cross);
    renderer.setSeriesPaint(0, Color.red);
    
    return new ChartPanel(jfreechart, true, true, true, false, false);
  }

  /**
   * 
   * @param complex
   * @param series
   * @param sampleRate
   */
  private void plotComplexArray(Complex[] complex, XYSeries series) {
    if(series==null) return;
    
    series.clear();
    int step = 1;
    if (complex.length > 100000)
      step = 15;

    for (int i = 0; i < complex.length; i += step) {
      double index = (i * sampleRate / 2) / complex.length;
      double y = complex[i].abs();
      if (y <= 0)
        y=0.00000000000000000001;
      try {
        series.add(index, y);        
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * 
   * @param doubleArray
   * @param series
   */
  private void plotDoubleArray(double[] doubleArray, XYSeries series) {
    if(series==null) return;
    
    series.clear();

    int step = 1;
    if (doubleArray.length > 10000)
      step = 15;

    if (series.getKey().toString().contains("IFFT")) {
      for (int i = -doubleArray.length/2; i < doubleArray.length/2; i += step) {
        series.add(i, doubleArray[i+doubleArray.length/2]);
      }
    } else {
      for (int i = 0; i < doubleArray.length; i += step) {
        series.add(i, doubleArray[i]);
      }
    }
  }

  /**
   * 
   * @param array
   * @param series
   */
  private void plotFloatArray(float[] array, XYSeries series) {
    if(series==null) return;
    
    series.clear();

    int step = 1;
    if (array.length > 10000)
      step = 15;

    for (int i = 0; i < array.length; i += step) {
      series.add(i, array[i]);
    }
  }
  
  

}
