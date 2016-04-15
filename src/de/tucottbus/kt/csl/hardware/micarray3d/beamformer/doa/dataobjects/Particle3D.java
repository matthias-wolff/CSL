package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects;

import java.util.Random;

import javax.vecmath.Point3d;

import Jama.Matrix;

public class Particle3D implements Particle {
  
  /**
   * Coordinates of the 3d point
   */
  private Point3d coords;

  /**
   * velocity in x,y and z direction
   */
  private Point3d velocity = new Point3d(0,0,0);
  
  public Particle3D(Point3d coords){
   this.coords=coords;
  }
  
  public Point3d getCoords(){
    return coords;
  }
  
  public void setCoords(Point3d coords){
    this.coords=coords;
  }
  
  public Point3d getVelocity(){
    return velocity;
  }
  
  public void setVelocity(Point3d velocity){
    this.velocity=velocity;
  }
  
  public Matrix getParticleVector(){
    double[] vecArr = new double[6];
    vecArr[0]=coords.x;
    vecArr[1]=coords.y;
    vecArr[2]=coords.z;
    vecArr[3]=velocity.x;
    vecArr[4]=velocity.y;
    vecArr[5]=velocity.z;
    
    return new Matrix(vecArr, 1).transpose(); 
  }
  
  public boolean isEmpty(){
    if(coords==null || velocity==null)
      return true;
    return false;
  }
  
  public int hasCode(){
    return coords.hashCode() ^ velocity.hashCode();
  }
  
  public boolean equals(Particle3D p) {
    return this.coords.equals(p.getCoords())
        && this.velocity.equals(p.getVelocity());
  }
  
  @Override
  public Particle3D clone() {
    try {
      return (Particle3D) super.clone();
    } catch(CloneNotSupportedException e) {
      throw new Error();
    }
  }

  @Override
  public void addNoise(Random r, double spread) {
    coords.x += spread*r.nextGaussian();
    coords.y += spread*r.nextGaussian();
    coords.z += spread*r.nextGaussian();
  }
  
}
