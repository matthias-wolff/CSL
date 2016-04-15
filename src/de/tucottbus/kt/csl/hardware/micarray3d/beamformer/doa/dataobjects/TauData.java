package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.Localizers;

public class TauData {
  
  private final int refMicId;
  
  private final int micId;
  
  private final int tauValue;
  
  private double r_si;
  
  private final Point3d micPosition;

  public TauData(int refMicId, int micId, int tauValue, Point3d micPosition){
    this.refMicId=refMicId;
    this.micId=micId;
    this.tauValue=tauValue;
    this.micPosition=micPosition;
    r_si=tauValue*Localizers.SAMPLEMETER;
  }
  
  public int getRefMicId() {
    return refMicId;
  }

  public int getMicId() {
    return micId;
  }

  public int getTauValue() {
    return tauValue;
  }
  
  public void setRi(double rsi){
    this.r_si=rsi;
  }
  
  public double getRi(){
    return r_si;
  }
  
  public double getRiSquard(){
    return (r_si*r_si);
  }

  public Point3d getMicPosition() {
    return micPosition;
  }
  
  public void setMicPosition(Point3d micPos) {
    micPos.scale(1/100.0);
    this.micPosition.set(micPos);
  }

}
