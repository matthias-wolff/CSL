package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects;

import java.util.Random;

import javax.vecmath.Point2d;

import Jama.Matrix;

public class Particle2D implements Particle {
  
  /**
   * Coordinates of the 2d point 
   */
  private Point2d coords;

  /**
   * velocity in x and z direction
   */
  private Point2d velocity = new Point2d(0,0);

  
  public Particle2D(Point2d coords){
   this.coords=coords;
  }
  
  public void setCoords(Point2d coords){
    this.coords=coords;
  }
  
  public Point2d getCoords(){
    return coords;
  }
  
  public void setVelocity(Point2d velocity){
    this.velocity=velocity;
  }
  
  public Point2d getVelocity(){
    return velocity;
  }
  
  public Matrix getParticleVector(){
    double[] vecArr = null;
      vecArr = new double[4];
      vecArr[0]=coords.x;
      vecArr[1]=coords.y;
      vecArr[2]=velocity.x;
      vecArr[3]=velocity.y;
    
    return new Matrix(vecArr, 1).transpose(); 
  }
  
  @Override
  public void addNoise(Random r, double spread) {
    coords.x += spread*r.nextGaussian();
    coords.y += spread*r.nextGaussian();
  }
  
  public boolean isEmpty(){
    if(coords==null || velocity==null)
      return true;
    return false;
  }
  
  public int hasCode(){
    return coords.hashCode() ^ velocity.hashCode();
  }
  
  public boolean equals(Particle2D p) {
    return this.coords.equals(p.getCoords())
        && this.velocity.equals(p.getVelocity());
  }
  
  @Override
  public Particle2D clone() {
    try {
      return (Particle2D) super.clone();
    } catch(CloneNotSupportedException e) {
      throw new Error();
    }
  }
  
}
