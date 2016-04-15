package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.particlefilter;

import java.util.Random;

import javax.vecmath.Point2d;
import javax.vecmath.Point3d;

import Jama.Matrix;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle2D;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.Particle3D;

/**
 * This class contains the dynamic model of the GCC tracker.
 * @author Martin Birth
 *
 */
public class DynamicsModel {
  
  //################### static fields ###################
  
  /**
   * Speed [m/s]
   */
  private final static double V = 1;
  
  /**
   * angular velocity [1/s]
   */
  private final static double BETA = 10;
  
  /**
   * Time between two measurements [s]
   */
  private final static double DELTA_T = (AudioInputConstants.FRAME_SIZE*1.0)/AudioInputConstants.SAMPLERATE;
  
  /**
   * Acceleration in x,y or z direction
   */
  private final static double A_XYZ = Math.exp(-BETA*DELTA_T);
  
  /**
   * 
   */
  private final static double B_XYZ = V*Math.sqrt(1-(A_XYZ*A_XYZ));
  
  /**
   * Generator for random noise
   */
  private final static Random RANDOM = new Random();
  
  /**
   * Dimension of the identity matrices
   */
  private final static int DIM_2D = 4;
  private final static int DIM_3D = 6;
  
  //################### non-static fields ###################
  
  private final Matrix positionUpdateMatrix2d;
  private final Matrix positionUpdateMatrix3d;
  
  private final Matrix velocityUpdateMatrix2d;
  private final Matrix velocityUpdateMatrix3d;
  
  private final Matrix noiseVector2d;
  private final Matrix noiseVector3d;
  
  //################### lifetime methods ###################
  
  /**
   * Constructor
   */
  public DynamicsModel(){
    positionUpdateMatrix2d = getPositionUpdateMatrix(DIM_2D);
    positionUpdateMatrix3d = getPositionUpdateMatrix(DIM_3D);
    
    velocityUpdateMatrix2d = getVelocityUpdateMatrix(DIM_2D);
    velocityUpdateMatrix3d = getVelocityUpdateMatrix(DIM_3D);
    
    noiseVector2d = getNoiseVector(DIM_2D);
    noiseVector3d = getNoiseVector(DIM_3D);
  }
  
  /**
   * Predict a new particle based on the old particle
   * @param p
   * @return
   */
  public Particle getPredictedParticle(Particle p) {
    if(p instanceof Particle2D){
      return predictParticle2d((Particle2D)p);
    }else if(p instanceof Particle3D){
      return predictParticle3d((Particle3D)p);
    } else return p;
  }
  
  /**
   * Predict a 2d particle
   * @param p - {@link Particle2D}
   * @return Particle2D
   */
  private Particle predictParticle2d(Particle2D p){
    Particle2D newP = p.clone();
    Matrix lastVec=newP.getParticleVector();
    Matrix vXt = (velocityUpdateMatrix2d.times(lastVec)).plus(noiseVector2d);
    Matrix pMatrix = positionUpdateMatrix2d.times(vXt);
    
    newP.setCoords(new Point2d(pMatrix.get(0, 0),pMatrix.get(1, 0)));
    newP.setVelocity(new Point2d(pMatrix.get(2, 0),pMatrix.get(3, 0)));
    
    return newP;
  }
  
  /**
   * Predict a 3d particle
   * @param p - {@link Particle3D}
   * @return Particle2D
   */
  private Particle predictParticle3d(Particle3D p){
    Particle3D newP = p.clone();
    Matrix lastVec=newP.getParticleVector();
    Matrix vXt = (velocityUpdateMatrix3d.times(lastVec)).plus(noiseVector3d);
    Matrix pMatrix = positionUpdateMatrix3d.times(vXt);
    
    newP.setCoords(new Point3d(pMatrix.get(0, 0),pMatrix.get(1, 0),pMatrix.get(2, 0)));
    newP.setVelocity(new Point3d(pMatrix.get(3, 0),pMatrix.get(4, 0),pMatrix.get(5, 0)));
    
    return newP;
  }
  
  /**
   * Get the matrix for the update of the position
   * @param n - dimension
   * @return Martix
   */
  private Matrix getPositionUpdateMatrix(int n){
    Matrix updateM = Matrix.identity(n, n);
    if(n==4){
      updateM.set(0, 2, DELTA_T);
      updateM.set(1, 3, DELTA_T);
    }
    if(n==6){
      updateM.set(0, 3, DELTA_T);
      updateM.set(1, 4, DELTA_T);
      updateM.set(2, 5, DELTA_T);
    }
    return updateM;
  }
  
  /**
   * Get the update matrix for the velocity
   * @param n - dimension
   * @return Matrix
   */
  private Matrix getVelocityUpdateMatrix(int n){
    double A_XYZ = Math.exp(-BETA*DELTA_T);
    Matrix updateM = Matrix.identity(n, n);
    updateM.set(3, 3, A_XYZ);
    if(n==4)
      updateM.set(2, 2, A_XYZ);
    if(n==6){
      updateM.set(4, 4, A_XYZ);
      updateM.set(5, 5, A_XYZ);
    }
    return updateM;
  }
  
  /**
   * Get a vector to add some noise to the dynamics model
   * @param n - int, dimension
   * @return Matrix
   */
  private Matrix getNoiseVector(int n){
    double[] vecArr = null;
    if(n==4){
      vecArr = new double[n];
      vecArr[0] = 0;
      vecArr[1] = 0;
      vecArr[2] = B_XYZ*RANDOM.nextDouble();
      vecArr[3] = B_XYZ*RANDOM.nextDouble();
    } else if(n==6) {
      vecArr = new double[n];
      vecArr[0] = 0;
      vecArr[1] = 0;
      vecArr[2] = 0;
      vecArr[3] = B_XYZ*RANDOM.nextDouble();
      vecArr[4] = B_XYZ*RANDOM.nextDouble();
      vecArr[5] = B_XYZ*RANDOM.nextDouble();
    }
    
    return new Matrix(vecArr, 1).transpose(); 
  }
  
}
