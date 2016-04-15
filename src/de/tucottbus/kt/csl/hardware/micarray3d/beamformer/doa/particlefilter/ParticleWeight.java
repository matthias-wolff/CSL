package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.particlefilter;

import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle;

public class ParticleWeight<T extends Particle> implements Cloneable {
  
  private T data;
  
  private double lastWeight;
  
  private double weight;
  
  private int copyCount;
  
  public ParticleWeight(T p) {
    this(p, 1, 0);
  }

  public ParticleWeight(T p, double lastWeight, int copyCount) {
    this.data = p;
    this.lastWeight = lastWeight;
    this.weight = 1.0;
    this.copyCount = copyCount;
  }
  
  public T getData(){
    return data;
  }
  
  public void setLastWeight(double lastWeight){
    this.lastWeight=lastWeight;
  }
  
  public double getLastWeight(){
    return lastWeight;
  }

  public double getWeight(){
    return weight;
  }
  
  public void setWeight(double weight){
    this.weight=weight;
  }

  public int getCopyCount(){
    return copyCount;
  }
}