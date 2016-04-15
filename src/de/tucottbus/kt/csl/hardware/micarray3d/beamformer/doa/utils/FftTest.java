package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils;

import java.util.ArrayList;

import org.apache.commons.math3.complex.Complex;
import org.jfree.data.xy.XYSeries;

import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.utils.AudioUtils;
import de.tucottbus.kt.csl.hardware.audio.utils.FFT;

public class FftTest {
  private static Plotter plotter = new Plotter();
  private static XYSeries timeDomain1 = new XYSeries("Time Domain 1");
  private static XYSeries timeDomain2 = new XYSeries("Time Domain 2");
  private static XYSeries freqSeriesX0 = new XYSeries("Inv Freq X0");
  private static XYSeries freqSeriesX1 = new XYSeries("Freq X1");
  private static XYSeries gccFreqSeries1 = new XYSeries("GCC Freq");
  private static XYSeries ifftSeries = new XYSeries("After IFFT");
  
  private static int argmax(double[] array){
    double maxVal = Double.NEGATIVE_INFINITY;
    int maxIndex = 0;
    for (int i = 0; i < array.length; i++) {
      if(array[i] > maxVal){
        maxVal = array[i];
        maxIndex = i;
      }
    }
    return maxIndex;
  }
  
  public static float getRMS(float[] raw) {
    float sum = 0;
    if (raw.length == 0)
      return sum;
    
    for (float sample : raw) {
      sum += sample * sample;
    }
    
    return sum;
  }
  
  public static float getSNR(float[] signal, float[] noise){
    float s = getRMS(signal);
    float n = getRMS(noise);
    
    return (float) (10*Math.log10(s/n));
  }
  
  public static void main(String[] args) {
    Thread thread = new Thread(new Runnable() {
      @SuppressWarnings("unused")
      @Override
      public void run() {
        // create plotter
        final ArrayList<XYSeries> series = new ArrayList<XYSeries>();
        series.add(timeDomain1);
        series.add(timeDomain2);
        series.add(freqSeriesX0);
        series.add(freqSeriesX1);
        series.add(gccFreqSeries1);
        series.add(ifftSeries);
        plotter.initFrame(series,AudioInputConstants.SAMPLERATE);
        
        // get input signal 1
        TestData test = new TestData();
        float[] audioData = AudioUtils.getDataFromWaveFile(test.getPath(TestData.TEST[5]),AudioInputConstants.SAMPLERATE);
        
        // get input signal 2
        float[] noise = AudioUtils.getDataFromWaveFile(test.getPath(TestData.TEST[6]),AudioInputConstants.SAMPLERATE);
        
        // shift audio data
        final int offest = 48000;
        final int windowSize = 1024;
        final int delta = 91;
        float[] audioDataSub = new float[windowSize];
        float[] audioDataSub2 = new float[windowSize];
        System.arraycopy(audioData, offest+0, audioDataSub, 0, audioDataSub.length);
        System.arraycopy(audioData, offest+delta, audioDataSub2, 0, audioDataSub2.length);
        
        // add noise to signal 1 and 2
        int noiseIndex = 10000;
        int noiseShift = 20000;
        float[] noiseSub = new float[windowSize];
        float[] noiseSub2 = new float[windowSize];
        System.arraycopy(noise, noiseIndex+0, noiseSub, 0, noiseSub.length);
        System.arraycopy(noise, noiseIndex+noiseShift, noiseSub2, 0, noiseSub2.length);
        System.out.println("SNR1: "+getSNR(audioDataSub, noiseSub));
        System.out.println("SNR2: "+getSNR(audioDataSub2, noiseSub2));
        for (int i = 0; i < audioDataSub.length; i++) {
          audioDataSub[i]=(float) (audioDataSub[i]+(0.5*noiseSub[i]));
          audioDataSub2[i]=(float) (audioDataSub2[i]+(0.5*noiseSub2[i]));
        }
        
        // run fft on input data
        FFT fft = new FFT();
        final boolean halfSpectrum = Boolean.TRUE;
        Complex[] X0 = fft.getFFTSpectrum(audioDataSub,halfSpectrum);
        Complex[] Xi = fft.getFFTSpectrum(audioDataSub2,halfSpectrum);
        
        Complex[] X0NOISE = fft.getFFTSpectrum(noiseSub,halfSpectrum);
        Complex[] X1NOISE = fft.getFFTSpectrum(noiseSub2,halfSpectrum);
        
        Complex[] filter = StaticBandpassFIR.getComplexBandpass1024();
        for (int i = 0; i < args.length; i++) {
          X0[i].multiply(filter[i]);
          Xi[i].multiply(filter[i]);
        }
        
        // GCC PHAT
        Complex[] gccPhat = new Complex[X0.length];
        for (int i = 0; i < gccPhat.length; i++) {
          Complex crossX0X1 = X0[i].multiply(Xi[i].conjugate());
          double PHAT = (crossX0X1).reciprocal().abs();
          Complex XX0 = X0[i].multiply(X0[i].conjugate());
          Complex XXi = Xi[i].multiply(Xi[i].conjugate());
          Complex XX0Noise = X0NOISE[i].multiply(X0NOISE[i].conjugate());
          Complex XXiNoise = X1NOISE[i].multiply(X1NOISE[i].conjugate());
          
          double MSC = (((XX0.multiply(XXi)).reciprocal()).multiply(crossX0X1.abs()*crossX0X1.abs())).abs();
          Complex ML = (XX0.multiply(XXi)).divide(XX0Noise.multiply(XXi).add(XXiNoise.multiply(XX0)));
          Complex SCOT = ((XX0.multiply(XXi)).sqrt()).reciprocal();
          gccPhat[i]=(crossX0X1).multiply(PHAT);
        }
        
        // IFFT
        Complex[] idft = fft.getInverseFftSpectrum(gccPhat,halfSpectrum);
        double[] real = new double[idft.length];
        for (int i = 0; i < idft.length; i++) {
          real[i]=idft[i].getReal();
        }
        System.out.println("Estimated tau:" +argmax(real));
        System.out.println("Estimated time delay:" +argmax(real)/44100.0);
        
        // create symmetric IDFT of GGC PHAT
        double[] realSym = new double[idft.length];
        System.arraycopy(real, idft.length/2, realSym, 0, idft.length/2);
        System.arraycopy(real, 0 , realSym, idft.length/2, idft.length/2);
        
        // plot all data
        plotter.addDataSet(new PlotData<float[]>(timeDomain1, audioDataSub));
        plotter.addDataSet(new PlotData<float[]>(timeDomain2, audioDataSub2));
        plotter.addDataSet(new PlotData<Complex[]>(freqSeriesX0, X0));
        plotter.addDataSet(new PlotData<Complex[]>(freqSeriesX1, Xi));
        plotter.addDataSet(new PlotData<Complex[]>(gccFreqSeries1, gccPhat));
        plotter.addDataSet(new PlotData<double[]>(ifftSeries, realSym));
      }
    });
    thread.setName("FFT-Test-Thread");
    thread.start();
  }
}
