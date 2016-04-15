package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.particlefilter;

import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle2D;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle3D;

public class MultiEvaluator<T> implements ParticleEvaluator<T> {
  private double mu = 0;

  private double sigma = 20;

  private double noise = 0.0;

  private final Random r = new Random();

  @Override
  public double evaluate(T p) {
    double error = r.nextDouble() * noise;
    double gauss = 0;
    if(p instanceof Particle2D){
      Point2d point = ((Particle2D)p).getCoords();
      gauss = gaussian(point.getX(), mu, sigma);
    } if(p instanceof Particle3D) {
      Point3d point = ((Particle3D)p).getCoords();
      gauss =  gaussian(point.getX(), mu, sigma);
    }
    return gauss+error;
  }
  
  public static double gaussian(double x, double mu, double sigma) {
    double d2 = (x - mu) * (x - mu);
    return Math.exp(-d2 / sigma);
  }

  public double getNoise() {
    return noise;
  }

  public void setNoise(double noise) {
    this.noise = noise;
  }

  public double getSigma() {
    return sigma;
  }

  public void setSigma(double sigma) {
    this.sigma = sigma;
  }
  
  public double getMu() {
    return sigma;
  }

  public void setMu(double mu) {
    this.mu = mu;
  }


}