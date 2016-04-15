package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.particlefilter;

public interface ParticleEvaluator<T> {
  public double evaluate(T p);
}
