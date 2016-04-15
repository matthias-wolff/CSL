package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.math3.complex.Complex;
import org.jfree.data.xy.XYSeries;

import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.audio.utils.FFT;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.AudioData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.TauData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils.PlotData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils.Plotter;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils.StaticBandpassFIR;

/**
 * This class contains all functions an methods for the analysis filter,
 * to detect the direction of arriving audio data.
 * 
 * @author Martin Birth
 *
 */
public final class DelayAnalysis {
  
  //##################### static fields #####################
  
  /**
   * Set this to true, for debugging mode
   */
  private final static boolean DEBUG = Boolean.FALSE;
  
  /**
   * Set this to true to reduce the redundancy of a complex FFT spectrum
   */
  public final static boolean HALF_SPECTRUM = Boolean.TRUE;
  
  /**
   * Samples per meter [1/m]
   */
  private final static double SAMPLES_PRO_METER = (AudioInputConstants.SAMPLERATE*1.0)/AudioInputConstants.SPEED_OF_SOUND;
  
  /**
   * Sample distance [m]
   */
  private final static double METER = AudioInputConstants.SPEED_OF_SOUND/(AudioInputConstants.SAMPLERATE*1.0);
  
  /**
   * Maximum angle (in degree)
   */
  private final static int MAX_ANGLE = 180;
  
  /**
   * FIR bandpass filter
   */
  private final static Complex[] bpFilter = StaticBandpassFIR.getComplexBandpass1024();
    
  //################### non-static fields ###################
  
  /**
   * Object for Fourier transformation and operations
   */
  private final FFT fft;
  
  private Plotter grapher;
  private XYSeries crossFreqSeries;
  private XYSeries gccPhaseSeries;
  private XYSeries ifftPhatSeriesReal;
  
  //######################## running ########################
  
  /**
   * Private Constructor
   */
  public DelayAnalysis(){
    fft = new FFT();
    if(DEBUG){
      grapher = new Plotter();
      crossFreqSeries = new XYSeries("Cross Freq Domain 1&2");
      gccPhaseSeries = new XYSeries("GCC Phase");
      ifftPhatSeriesReal = new XYSeries("IFFT(PHAT) Real");
      
      ArrayList<XYSeries> serieses = new ArrayList<XYSeries>();
      serieses.add(crossFreqSeries);
      serieses.add(gccPhaseSeries);
      serieses.add(ifftPhatSeriesReal);
      grapher.initFrame(serieses,AudioInputConstants.SAMPLERATE);
    }
  }
int counter=0;
  /**
   * Running the delay analysis filter function for every N-1 microphone pair.
   * The argument of the maximum value is representing the tau value of this
   * pair of microphones.
   * @param audioData
   * @param tauDataHM
   * @param refMicId
   */
  public void run(ConcurrentHashMap<Integer, AudioData> audioDataHM, ConcurrentHashMap<String, TauData> tauDataHM, int refMicId) {
    tauDataHM.clear();
    
    // get all DFT spectrum for all audio channels
    // and multiply them with a static FIR filter
    for (Entry<Integer, AudioData> entry : audioDataHM.entrySet()){
      AudioData aData = entry.getValue();
      Complex[] spect = fft.getFFTSpectrum(aData.getAudioData(),HALF_SPECTRUM);
      if(spect.length!=1024)
        aData.setSpectrum(spect);
      else{
        Complex[] filteredSpect = new Complex[spect.length];
        for (int i = 0; i < filteredSpect.length; i++) {
          filteredSpect[i]=spect[i].multiply(bpFilter[i]);
        }
        aData.setSpectrum(filteredSpect);
      }
    } 
    
    // running gcc phat for every channel
    for (int i = 0; i< RmeHdspMadi.CHANNEL_COUNT/2 ; i++){
      AudioData aData = audioDataHM.get(i);
      if(aData==null || refMicId==aData.getMicId())
        continue;
      double[] crossCorrTau = getGccPerChannel(audioDataHM.get(refMicId).getSpectrum(), aData.getSpectrum());
      double distance = audioDataHM.get(refMicId).getPosition().distance(aData.getPosition());
      int tau = getTau(crossCorrTau, distance);
      tauDataHM.put(refMicId+","+aData.getMicId(), new TauData(refMicId, aData.getMicId(), tau, aData.getPosition()));
      
      if(DEBUG) {
        System.out.print("Pair: "+refMicId+","+aData.getMicId() +"\tTau Max: "+tau);
        System.out.print(String.format(Locale.US,"\t| Distance: %2.3f",distance));
        System.out.print(String.format(Locale.US,"\t| Delta Tau: %4d",getDeltaTau(distance)));
        System.out.println(String.format(Locale.US,"\t| Angle from Tau: %3.2f",(MAX_ANGLE-getAngleFromTau(tau, distance))));
      }
    }
  }
  
  // ***************************************************************************
  // ******* methods for generalized cross correlation
  // ***************************************************************************
  
  /**
   * Calculating the generalized cross correlation (gcc) function for one pair of microphones.<br>
   * The reference microphone will be {@value #REF_MIC_ID}. <br><br>
   * The spectrum of the cross correlation will be weighted by a pre-whitening filter function.<br>
   * This filter function 
   * @param spectrums - array of Complex[][] spectrums
   * @param ch
   * @return
   */
  private double[] getGccPerChannel(Complex[] X0, Complex[] Xi){
    // gcc with phat filter 1/|X_0*X_i'|
    Complex[] gccPhat = new Complex[X0.length];
    for (int i = 0; i < gccPhat.length; i++) {
      Complex crossX0Xi = X0[i].multiply(Xi[i].conjugate());
      double PHAT = (crossX0Xi).reciprocal().abs();
      gccPhat[i]=(X0[i].multiply(Xi[i].conjugate())).multiply(PHAT);
    }
    
    // IFFT from 
    Complex[] inv = fft.getInverseFftSpectrum(gccPhat, HALF_SPECTRUM);
    double[] invReal = new double[inv.length];
    
    int lenHalf = invReal.length/2;
    for (int i = 0; i < lenHalf; i++) {
      invReal[i]=inv[i+lenHalf].getReal();
    }
    
    for (int i = 0 ; i < lenHalf; i++) {
      invReal[i+lenHalf]=inv[i].getReal();
    }
    
    if(DEBUG) {
      double[] gccPhase = new double[gccPhat.length];
      for (int i = 0; i < gccPhase.length; i++) {
        gccPhase[i]= gccPhat[i].getArgument();
      }
      grapher.addDataSet(new PlotData<Complex[]>(crossFreqSeries, gccPhat));
      grapher.addDataSet(new PlotData<double[]>(gccPhaseSeries, gccPhase));
      grapher.addDataSet(new PlotData<double[]>(ifftPhatSeriesReal, invReal));
    }
         
    return invReal;
  }
  
  // ***************************************************************************
  // ******* methods for the tau search
  // ***************************************************************************
  
  /**
   * Get the argument of the maximal value is representing the tau value.
   * @param array double[]
   * @return int
   */
  private int getTau(double[] array, double distance) {
    int searchDeltaTau = getDeltaTau(distance);
    int lowerBound = array.length/2-searchDeltaTau;
    int upperBound = array.length/2+searchDeltaTau;
    
    int bestIdx = -1;
    double max = Double.NEGATIVE_INFINITY;
    for (int i = lowerBound; i < upperBound; i++) {
      double elem = array[i];
      if (elem > max) {
        max = elem;
        bestIdx = i;
      }
    }
    
    return bestIdx-(array.length/2);
  }
   
  /**
   * Get the range for the search of tau.
   * @param distance
   * @return integer value of the range
   */
  private int getDeltaTau(double distance){
    return (int) Math.ceil(distance*SAMPLES_PRO_METER);
  }
  
  /**
   * Calculating the angle of sound wave arrival.
   * @param taus
   * @param distance
   * @return
   */
  public double getAngleFromTau(int tau, double distance){
    double r = (tau*METER)/distance;
    return Math.toDegrees(Math.acos(r));
  }
  
  // ***************************************************************************
  // ******* interpolation function for the tau
  // ***************************************************************************
  
  private static final int INTERPOLATE_FACTOR = 8;
  
  /**
   * Use the first approximation of the Tayler series
   * @param index, of the tau value
   * @param r0i
   * @return
  */
  @SuppressWarnings("unused")
  private double getInterpolatedTau(int index, double[] r0i, double distance){
    int tau = getTau(r0i, distance);
    double part = (r0i[index-1]-r0i[index+1])/(r0i[index+1]-2*r0i[index]+r0i[index-1]);
    double interpolatedTau = (tau/INTERPOLATE_FACTOR)+(1/(2*INTERPOLATE_FACTOR)*part);
    return interpolatedTau;
  } 
   
}
