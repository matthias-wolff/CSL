package de.tucottbus.kt.csl.hardware.audio.utils;

import java.util.Arrays;

import jogamp.opengl.util.pngj.FilterType;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

/**
 * This class handles all methods for fast Fourier transformation of a discrete audio signal.
 * 
 * @author Martin Birth
 *
 */
public class FFT {
  
  /**
   * Static value of 2*Pi
   */
  private final static double TWO_PI = 2 * Math.PI;
  
  /**
   * Instance of FastFourierTransformer to transform into the frequency domain
   */
  private final FastFourierTransformer fft;
  
  /**
   * Constructor with filter type
   * @param filter
   * @see FilterType
   */
  public FFT(){
    fft = new FastFourierTransformer(DftNormalization.UNITARY);
  }
  
  /**
   * Builds a new array of {@link Complex} from the specified two dimensional
   * array of real and imaginary parts. In the returned array {@code dataC}, the
   * data is laid out as follows
   * <ul>
   * <li>{@code dataC[i].getReal() = dataRI[0][i]},</li>
   * <li>{@code dataC[i].getImaginary() = dataRI[1][i]}.</li>
   * </ul>
   * The signal is windowed by a Van-Hann-window to reduce the picked fance effect. 
   * The output array has a double size of the input array.
   * @param frame
   *          - float[] array
   * @param halfSpect
   *          - set this to TRUE to get the only the half of the spectrum
   * @return Complex[] array of {@link Complex} with specified real and imaginary
   *         parts.
   */
  public Complex[] getFFTSpectrum(float[] frame, Boolean halfSpect) {
    int powerOf2 = getNextPowerOf2Value(frame.length);
    
    // zero-padding and windowing
    double[] window = new double[powerOf2*2];
    Arrays.fill(window, 0);
    int destPos = (int) Math.ceil((powerOf2*2-powerOf2)/2);
    for (int i = 0; i < frame.length; i++) {
      window[i+destPos] = frame[i]*getHanningValue(i, frame.length);
    }
    
    Complex[] spec = fft.transform(window, TransformType.FORWARD);
    
    if(halfSpect!=null && halfSpect){
      return getHalfFilteredSpectrum(spec);
    } else {
      return spec;
    }
  }
  
  /**
   * 
   * @param spec
   * @return
   */
  private Complex[] getHalfFilteredSpectrum(Complex[] spec){
    Complex[] specHalf = new Complex[spec.length/2];
    System.arraycopy(spec, 0, specHalf, 0, spec.length/2);
    return specHalf;
  }
  
  /**
   * Get the inverse Fourier spectrum.
   * @param spectrum {@link Complex}[] as Fourier spectrum
   * @return {@link Complex}[] array of {@link Complex}
   */
  public Complex[] getInverseFftSpectrum(Complex[] spectrum, boolean halfSpectrum){
    Complex[] transSpectrum = null;
    if(!halfSpectrum){
      transSpectrum = spectrum;
    }else{
      transSpectrum = new Complex[spectrum.length*2];
      System.arraycopy(spectrum, 0, transSpectrum, 0, spectrum.length);
      int k = spectrum.length-1;
      for (int i = transSpectrum.length/2; i < transSpectrum.length; i++) {
        transSpectrum[i] = spectrum[k--].conjugate();
      }
    }
    return fft.transform(transSpectrum, TransformType.INVERSE);
  }
    
  /**
   * Calculates the magnitude spectrum of a DFT spectrum.
   * @param spec - float[] array
   * @return float[] array
   */
  public double[] getMagnitudeOfSpectrum(Complex[] spec){
    double[] magnitude =new double[spec.length];
    
    for(int i=0;i<spec.length;i++){
      magnitude[i] = spec[i].abs();
    }
    
    return magnitude;
  }
  
  /**
   * Get the next power of 2 integer value of a number n
   * @param n - int value
   * @return int, power of 2 integer value
   */
  public static int getNextPowerOf2Value(int n){
    int count = 0;
    
    if((n&(n-1))==0)
      return n;
    
    while(n!=0){
      n>>=1;
      count+=1;
    }
    
    return 1<<count;
  }
  
  /**
   * Get a single value of the hamming function
   * @param index - int, position
   * @param length - int, length of the window function
   * @return float value
   * @see <a href="https://de.wikipedia.org/wiki/Fensterfunktion#Hamming-Fenster">Hamming Window</a>
   */
  protected float getHammingValue(int index, int length) {
    return 0.54f - 0.46f * (float) Math.cos(TWO_PI * index / (length - 1));
  }
  
  /**
   * Get a single value of the hanning function
   * @param index - int, position
   * @param length - int, length of the window function
   * @return float value
   * @see <a href="https://de.wikipedia.org/wiki/Fensterfunktion#Von-Hann-Fenster">Hanning Window</a>
   */
  protected float getHanningValue(int index, int length) {
    return 0.5f * (1f - (float) Math.cos(TWO_PI * index / (length - 1f)));
  }
  
  /**
   * Find the most dominant frequency in magnitude spectrum.
   * 
   * @param magnitude - float[] array of magnitude spectrum
   * @return float - most dominant frequency
   */
  public float getMostDominantFreq(double[] magnitude, int sampleRate){
    // find largest peak in power spectrum
    float maxMagnitude = 0;
    int maxIndex = -1;
    int len = magnitude.length;
    for(int i=0; i<len/2;i++){      
        if (magnitude[i] > maxMagnitude){
            maxMagnitude = (float)magnitude[i];
            maxIndex = i;
        }
    }
    
    // convert index of largest peak to frequency
    float freq = maxIndex * sampleRate / len;
    return freq;
  }
  
  /**
   * Get the auto power spectral density.
   * 
   * @param c1 - Complex[] spectrum
   * @return Complex[]
   */
  public static Complex[] getAutoPowerSpectrum(Complex[] c1){
    return getCrossPowerSpectrum(c1, c1);
  }
  
  /**
   * Get the cross power spectral density.
   * 
   * @param c1 - Complex[] spectrum
   * @param c2 - Complex[] spectrum
   * @return Complex[]
   */
  public static Complex[] getCrossPowerSpectrum(Complex[] c1, Complex[] c2){
    for (int i = 0; i < c1.length; i++) {
      c1[i].multiply(c2[i].conjugate());
    }
    return c1;
  }  
}
