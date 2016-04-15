package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects;

import javax.vecmath.Point3d;

import org.apache.commons.math3.complex.Complex;

/**
 * Wrapper class for the audio data of the single microphones
 * 
 * @author Martin Birth
 */
public class AudioData {
  private final int micId;
  
  private final float[] audioData;
  
  private final boolean selected;
  
  private final Point3d absolutPosition;
  
  private Complex[] spectrum;
  
  /**
   * 
   * @param micId
   * @param audioData
   * @param selected
   * @param absolutPosition
   */
  public AudioData(int micId, float[] audioData, boolean selected, Point3d absolutPosition){
    this.micId=micId;
    this.audioData=audioData;
    this.selected=selected;
    this.absolutPosition=absolutPosition;
    this.absolutPosition.scale(1/100.0);
  }

  public int getMicId() {
    return micId;
  }

  public float[] getAudioData(){
    return audioData;
  }
  
  public boolean isSelected(){
    return selected;
  }
  
  public Point3d getPosition(){
    return absolutPosition;
  }

  public Complex[] getSpectrum() {
    return spectrum;
  }

  public void setSpectrum(Complex[] spectrum) {
    this.spectrum = spectrum;
  }
  
}
