package de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa;

import java.util.concurrent.ConcurrentHashMap;

import javax.vecmath.Point3d;

import Jama.Matrix;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator.MicArray;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.TauData;

/**
 * This class containing all methods for a closed form microphone array
 * localization based on the the GCC-PHAT algorithm in the {@link DelayAnalysis} class.
 * 
 * @author Martin Birth
 *
 */
public class Localizers {
  
  /**
   * Set this to true, for debugging mode
   */
  private final static boolean DEBUG = Boolean.FALSE;
  
  /**
   * Sample distance [m]
   */
  public final static double SAMPLEMETER = AudioInputConstants.SPEED_OF_SOUND/(AudioInputConstants.SAMPLERATE*1.0);

  private int errorCounter = 0;
  
  /**
   * Constructor
   */
  public Localizers(){
  }
 
  /**
   * This method calculates the target vector to the relative delay times.
   * @param refMic, Point3d - coordinates of the reference microphone
   * @param refMicId, int - id of the ref-mic
   * @param tauHashMap - ConcurrentHashMap<String, TauData> / {@link TauData}
   * @param array - {@link MicArray}, to select the sub microphone array
   * @return the target vector of the type {@link Matrix}
   */
  public Matrix getTargetVektor(Point3d refMic, int refMicId, ConcurrentHashMap<String, TauData> tauHashMap, MicArray array){
    
    Matrix targetVector = getPossibleTargetSources(refMic, refMicId, tauHashMap, null);
    if(targetVector==null)
      return null;
    if(DEBUG){
      System.out.print("ErrorMatrix Iteration No. 1: ");
      printMatrix(targetVector);
    }
    
    double rsi = targetVector.get(3, 0);
    Matrix b = getMatrixB(tauHashMap, rsi, refMicId);
    Matrix targetVector2 = getPossibleTargetSources(refMic, refMicId, tauHashMap, b);
    if(targetVector2==null)
      return null;
    if(DEBUG){
      System.out.print("ErrorMatrix Iteration No. 2: ");
      printMatrix(targetVector2);
    }
        
    return targetVector2;
  }
  
  /**
   * Get the estimated target source. <br>
   * This is one iteration step through the closed form target estimation. 
   * @param refMic - Point3d of the reference microphone
   * @param tauHashMap - ConcurrentHashMap<String, TauData> / {@link TauData}
   * @param matrixB - {@link Matrix}, set this to NULL in the first iteration step
   * @return the target vector of the type {@link Matrix}
   */
  private Matrix getPossibleTargetSources(Point3d refMic, int refMicId, ConcurrentHashMap<String, TauData> tauHashMap, Matrix matrixB){
    Matrix gDis = getDistancesMatrix(refMic, refMicId, tauHashMap);
    Matrix Q = getErrorMatrix(matrixB, tauHashMap.size());
    Matrix invQ = Q.inverse();

    Matrix gDisT = ((Matrix) gDis.clone()).transpose();
    Matrix m0 = gDisT.times(invQ);
    Matrix m1 = m0.times(gDis);
    if (m1.det() == 0 && DEBUG){
      System.err.println("Error: Det(m1) is zero! "+ ++errorCounter);
      return null;
    }
    Matrix m2 = m1.inverse();
    Matrix m3 = m2.times(gDisT);
    Matrix m4 = m3.times(invQ);
        
    Matrix disVec = getDistanceVector(refMic, refMicId, tauHashMap).transpose();
    Matrix m5 = m4.times(disVec);
    
    if(m5.get(0,0)>100 && DEBUG){
      System.err.println("Error: Values too large!"+ ++errorCounter);
      return null;
    }
    
    if(m5.get(0,0)<-100 && DEBUG){
      System.err.println("Error: Values too small!"+ ++errorCounter);
      return null;
    }
    
    return m5;
  }
  
  /**
   * Get the error Matrix. In the first iteration round, the Matrix B can be null.
   * @param matB - Matrix
   * @param dim - dim
   * @return Matrix
   */
  private Matrix getErrorMatrix(Matrix matB, int dim){
    Matrix coVar = getCovarianzMatrix(dim);
    if(matB==null){
      return coVar;
    }
    Matrix matBB = matB;
    Matrix maA = matB.times(coVar);
    Matrix out = maA.times(matBB);
    return out;
  }
  
  /**
   * Get the matrix with the microphone position errors
   * @param refMic
   * @param tauDataHM
   * @return Matrix
   */
  private Matrix getDistancesMatrix(Point3d refMic, int refMicId, ConcurrentHashMap<String, TauData> tauDataHM){
    Matrix distances=new Matrix(tauDataHM.size(),4);
    
    int m = 0;
    for (int j = 0; j < RmeHdspMadi.CHANNEL_COUNT/2; j++) {
      TauData tData = tauDataHM.get(refMicId+","+j);
      if(tData == null)
        continue;
      distances.set(m, 0, (refMic.x-tData.getMicPosition().x));
      distances.set(m, 1, (refMic.y-tData.getMicPosition().y));
      distances.set(m, 2, (refMic.z-tData.getMicPosition().z));
      distances.set(m, 3, tData.getRi());
      m++;
    }
    
    return distances;
  }
  
  /**
   * Get the vector of microphone positions
   * @param refMic
   * @param tauDataHM
   * @return
   */
  private Matrix getDistanceVector(Point3d refMic, int refMicId, ConcurrentHashMap<String, TauData> tauDataHM){
    double[] disVecArr = new double[tauDataHM.size()];
    
    int i = 0;
    for (int j = 0; j < RmeHdspMadi.CHANNEL_COUNT/2; j++) {
      TauData tData = tauDataHM.get(refMicId+","+j);
      if(tData == null)
        continue;
      disVecArr[i]=0.5*(tData.getRiSquard()
          +(refMic.x*refMic.x)
          +(refMic.y*refMic.y)
          +(refMic.z*refMic.z)
          -(tData.getMicPosition().x*tData.getMicPosition().x)
          -(tData.getMicPosition().y*tData.getMicPosition().y)
          -(tData.getMicPosition().z*tData.getMicPosition().z));
      i++;
    }
    
    return new Matrix(disVecArr, 1); 
  }
  
  /**
   * Get a symmetric a-priori covariance matrix.
   * @param dim
   * @return Matrix
   */
  private Matrix getCovarianzMatrix(int dim){
    Matrix coVarMatrix=new Matrix(dim,dim);
    
    for(int m=0;m<dim;m++){
      for(int n=0;n<dim;n++){
        coVarMatrix.set(m, n, (m==n)?1:0.5);
      }
    }
    
    return coVarMatrix;
  }
  
  /**
   * Get the matrix with all estimated distances from the source point to every single microphone
   * @param tauHashMap
   * @param rs1
   * @return Matrix
   */
  private Matrix getMatrixB(ConcurrentHashMap<String, TauData> tauDataHM, double rs1, int refMicId){
    int dim = tauDataHM.size();
    Matrix bMat = new Matrix(dim,dim);
    
    int m=0, n=0;
    for (int j = 0; j < RmeHdspMadi.CHANNEL_COUNT/2; j++) {
      TauData tData = tauDataHM.get(refMicId+","+j);
      if(tData == null)
        continue;
      bMat.set(m++, n++, -((tData.getTauValue()*SAMPLEMETER)-rs1) );
    }
    
    return bMat;
  }
  
  // ################### for testings only ###################
  
  /**
   * Print out matrix for debugging
   * @param matrix
   */
  private void printMatrix(Matrix matrix){
    int cols = matrix.getColumnDimension();
    int rows = matrix.getRowDimension();
    for (int j = 0; j < rows; j++) {
      for (int i = 0; i < cols; i++) {
        double d = matrix.get(j, i);
        System.out.print(((d < 0) ? "" : " ")+String.format("%f |", d));
      }
      System.out.println("");
    }
    System.out.println("______________________________________");
  }
}
