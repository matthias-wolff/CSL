package de.tucottbus.kt.csl.hardware.micarray3d.beamformer;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.vecmath.Point3d;

import Jama.Matrix;
import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.ChannelSelector;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.DelayAnalysis;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.Localizers;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.PlausibilityChecker;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.AudioData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.TauData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils.DoATest;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * Virtual hardware: Direction of arrival (DoA) estimator.<br>
 * <br>
 * This class and all subclasses are used for an
 * estimate of the direction and position of a possible speaker.
 * 
 * @author Martin Birth
 *
 */
public final class DoAEstimator extends AAtomicHardware implements Runnable{
  
  // ################### static fields ###################
  
  /**
   * Set this to true, for debugging mode
   */
  private final static boolean DEBUG = Boolean.FALSE;
  
  /**
   * {@link AudioInputConstant.FRAME_SIZE}
   */
  private final static int FRAME_SIZE =  AudioInputConstants.FRAME_SIZE;
  
  /**
   * The default target (0,0,160) cm.
   */
  public static final Point3d DEFAULT_TARGET = new Point3d(0,0,160.0);
    
  /**
   * Selector for microphone array
   */
  public enum MicArray {
    ARRAY1, ARRAY2
  }
  
  //################### non-static fields ###################
  
  /**
   * private instance of this class
   */
  private static DoAEstimator instance;
  
  /**
   * Instance of the DelayAnalysis class
   */
  private DelayAnalysis delayAnalysis;
  
  /**
   * Instance of the ChannelSelector class
   */
  private ChannelSelector channelSelector;
  
  /**
   * Closed form localization
   */
  private final Localizers localizers;
  
  private final PlausibilityChecker plausibilityChecker;
  
  /**
   * Guard thread
   */
  private Thread guard = null;
  
  /**
   * thread boolean
   */
  private boolean runGuard = false;
  
  /**
   * thread boolean
   */
  private boolean autoMode = false;
  
  /**
   * Audio data queue
   */
  private final Queue<float[][]> audioDataQueue=new LinkedBlockingQueue<float[][]>();
  
  /**
   * Current microphone array state
   */
  private MicArrayState state;
  
  /**
   * Target source point
   */
  private final Point3d targetSource = new Point3d(DEFAULT_TARGET);
  
  private final ConcurrentHashMap<Integer, AudioData> hashMapAudioDataArray1 = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, AudioData> hashMapAudioDataArray2 = new ConcurrentHashMap<>();
  
  private final ConcurrentHashMap<String, TauData> hashMapTauDataArray1 = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, TauData> hashMapTauDataArray2 = new ConcurrentHashMap<>();
  
  private final AtomicInteger frameCounter = new AtomicInteger(0);
  
  private final float[][] audioInputData2x = new float[RmeHdspMadi.CHANNEL_COUNT][AudioInputConstants.FRAME_SIZE*2];
  
  //################### running process ###################
  
  /**
   * Private constructor -> please use {@link #getInstance()}
   */
  private DoAEstimator(){
    channelSelector = ChannelSelector.getInstance();
    delayAnalysis = new DelayAnalysis();
    localizers=new Localizers();
    plausibilityChecker=new PlausibilityChecker();
    
    // Start the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }
  
  /**
   * Singleton method to get the {@link DoAEstimator} object
   * @return DoAEstimator
   */
  public static synchronized DoAEstimator getInstance()
  {
    if (instance==null){
      instance = new DoAEstimator();
    }
    return instance;
  }
  
  @Override
  public void run() {
    runGuard = true;
    final int sleepMillis = 10;
    int ctr = 0;

    while (runGuard)
    {
      try {
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        log("Doa estimator started.");

        // - Run connection
        while (runGuard) {
          try {
            Thread.sleep(sleepMillis);
          } catch (InterruptedException e) {
            logErr(e.getMessage(),e);
          }
          if (!runGuard){
            break;
          }

          if (!audioDataQueue.isEmpty()) {
            runDoAEstimator();
          }
          
          if(!isConnected())
            break;

          ctr += sleepMillis;
          if (ctr >= 1000) {
            setChanged();
            notifyObservers(NOTIFY_STATE);
            ctr = 0;
          }
        }
        
        if(isConnected()){
          channelSelector=null;
          delayAnalysis=null;
        }
      } catch (Exception e) {
        logErr(e.getMessage(),e);
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          logErr(e.getMessage(),e1);
        }
      }
    }
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    if (guard!=null)
    {
      runGuard = false;
      try 
      {
        guard.interrupt();
        guard.join();
      } 
      catch (Exception e) 
      { 
        logErr("",e); 
      }
    }
    guard = null;
    super.dispose();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "DoA Estimator";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isConnected() {
    if(isDisposed()){
      return false;
    }
    
    if(delayAnalysis!=null && channelSelector != null)
      return true;
    else
      return false;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public AHardware getParent() {
    return MicArray3D.getInstance();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) {
    return null;
  }

 // ***************************************************************************
 // ******* core methods
 // ***************************************************************************
  
  int counter = 0;
  
  /**
   * Core method to run the DoAEstimator.<br>
   * 
   *  <ol><b>Step sequence:</b>
   *  <li>Read audio data from queue</li>
   *  <li>Merging to frames</li>
   *  <li>Running analysis filter (GCC function)</li> 
   *  <li>Running target localization</li> 
   *  <li>plausibility check</li>
   *  <li>particle filter</li>
   *  </ol>
   * @throws Exception
   */
  private void runDoAEstimator() throws Exception {
    long time = 0;
    
    if(DEBUG)
      time = System.currentTimeMillis();
    
    // 1. get audio data from queue
    float[][] audioInputData = audioDataQueue.poll();
    if(isAutoMode()==false)
      return;
    
    // 2. Merging two frames
    if(frameCounter.get()<2){
      for (int i = 0; i < RmeHdspMadi.CHANNEL_COUNT; i++) {
        System.arraycopy(audioInputData[i], 0, audioInputData2x[i], AudioInputConstants.FRAME_SIZE*frameCounter.get(), AudioInputConstants.FRAME_SIZE);
      }
      frameCounter.incrementAndGet();
      return;
    }
    frameCounter.set(0);
    
    state = MicArrayState.getCurrentState();
    
    // 3. Fill hashmaps with audio data
    channelSelector.fillAudioHM(audioInputData2x, hashMapAudioDataArray1, state.positions, MicArray.ARRAY1);
    channelSelector.fillAudioHM(audioInputData2x, hashMapAudioDataArray2, state.positions, MicArray.ARRAY2);
    
    // 4.1 running analysis filter (array 1)
    Matrix targetMatrix1 = null;
    if(hashMapAudioDataArray1.size()>1){
      int refMicId = 0;
      delayAnalysis.run(hashMapAudioDataArray1, hashMapTauDataArray1,refMicId);
      
      if(DEBUG){
        int[] taus1 = new int[hashMapTauDataArray1.size()];
        for (int j = 0; j < hashMapTauDataArray1.size(); j++) {
          TauData tData = hashMapTauDataArray1.get(refMicId+","+j);
          if(tData == null)
            continue;
          taus1[j]=tData.getTauValue();
        }
        DoATest.writetoTextFile(taus1, 1);
      }
      
      // 5.1 localization of the target point (array 1)
      if(hashMapTauDataArray1.size()>=3)
        targetMatrix1 = localizers.getTargetVektor(hashMapAudioDataArray1.get(refMicId).getPosition(), refMicId, hashMapTauDataArray1, MicArray.ARRAY1);
      if(targetMatrix1==null)
        return;
    }
    
    // 4.2 running analysis filter (array 2)
    Matrix targetMatrix2 = null;
    if(hashMapAudioDataArray2.size()>1){
      int refMicId = 0;
      delayAnalysis.run(hashMapAudioDataArray2, hashMapTauDataArray2,refMicId);
      
      if(DEBUG){
        int[] taus1 = new int[hashMapTauDataArray2.size()];
        for (int j = 0; j < hashMapTauDataArray2.size(); j++) {
          TauData tData = hashMapTauDataArray2.get(refMicId+","+j);
          if(tData == null)
            continue;
          taus1[j]=tData.getTauValue();
        }
        DoATest.writetoTextFile(taus1, 2);
      }
      
      // 5.2 localization of the target point (array 2)
      if(hashMapTauDataArray2.size()>=3){
        targetMatrix2 = localizers.getTargetVektor(hashMapAudioDataArray2.get(refMicId).getPosition(), refMicId, hashMapTauDataArray2, MicArray.ARRAY1);
      }
      if(targetMatrix2==null)
        return;
    }
    
    // 6. Verification of positions for plausibility
    Point3d targetA1 = null;
    Point3d targetA2 = null;
    if(targetMatrix1!=null){
      targetA1 = plausibilityChecker.checkArray1(targetMatrix1);
      targetSource.setX(targetA1.getX());
      targetSource.setZ(targetA1.getZ());
      if (DEBUG)
        System.out.println("T1: "+targetA1.toString());
    }
    if(targetMatrix2!=null){
      targetA2 = plausibilityChecker.checkArray2(targetMatrix2);
      targetSource.setY(targetA2.getY());
      if (DEBUG)
        System.out.println("T2: "+targetA2.toString());
    }
    
    // 7. particle filter
    // TODO: create classes

    if(DEBUG)
      System.out.println("DoA runtime:"+(System.currentTimeMillis()-time)+" ");
  }
  
  /**
   * Directly adding audio data to the processing pipeline
   * @param framesPerChannel
   */
  public void addAudioDataToQueue(float[][] framesPerChannel){
    audioDataQueue.add(framesPerChannel);
  }

  /**
   * Get the target source point.
   * @return Point3d
   */
  public Point3d getTargetSource() {
    return targetSource;
  }
  
  /**
   * Set a target point for 3d beamforming.
   * @param target - Point3d
   * @see #isAutoMode()
   */
  public void setTargetSource(Point3d target) {
    if(!isAutoMode())
     targetSource.set(target);
  }
  
  /**
   * Return TRUE, if the AutoMode is active.
   * @return boolean
   */
  public boolean isAutoMode(){
    return autoMode;
  }
  
  /**
   * Set this to TRUE too activate the auto mode.
   * @param mode
   */
  public void setAutoMode(boolean mode){
    autoMode=mode;
  }
  
  /**
   * Get the length of audio sample
   * @return int
   */
  public int getSampleLength(){
      return FRAME_SIZE;
  }
}
