package de.tucottbus.kt.csl.hardware.micarray3d.beamformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.AAtomicHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.audio.input.AudioInputConstants;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.audio.utils.WavePlayer;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator.MicArray;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.DelayAnalysis;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.AudioData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.dataobjects.TauData;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.doa.utils.TestData;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * This class is for testing the the phase response of the microphone array pairs.<br>
 * It is assumed that the reference microphone with number one is functional.
 * @author Martin Birth
 *
 */
public class MicrophoneGCCTester extends AAtomicHardware implements Runnable{
  
  // ############### static fields ###################
  /**
   * Runtime, the real audio output can be between 0-30000 ms.
   */
  private final static int TIME = 14000; // [ms]
  
  /**
   * Is the tester running or not
   */
  public static boolean dataAddingAllowed = Boolean.FALSE;
  
  /**
   * Reference microphone ID of the TV array
   */
  private static int REF_MIC_ID_A1 = 0;
  
  /**
   * Reference microphone ID of the ceiling array
   */
  private static int REF_MIC_ID_A2 = 0;
  
  // ############### non-static fields ###############
  
  /**
   * private instance of this class
   */
  private static  MicrophoneGCCTester instance;
  
  /**
   * Player for wave file
   */
  private final WavePlayer wavePlayer = new WavePlayer();
  
  /**
   * Instance of the DelayAnalysis class
   */
  private DelayAnalysis delayAnalysis;
    
  /**
   * Guard thread
   */
  private Thread guard = null;
  
  /**
   * thread boolean
   */
  private boolean runGuard = Boolean.FALSE;
  
  /**
   * Set this to true, to allow the running process of the analysis
   */
  private boolean runAnalysis = Boolean.FALSE;
  
  /**
   * Audio data queue
   */
  private final Queue<float[][]> audioDataQueue2 = new ConcurrentLinkedQueue<float[][]>();
  
  /**
   * Array of the audio Data
   */
  private float[][] audioInputData2x = new float[RmeHdspMadi.CHANNEL_COUNT][AudioInputConstants.FRAME_SIZE*2];
  
  /**
   * Hashmaps for running process
   */
  private final ConcurrentHashMap<Integer, AudioData> hashMapAudioData = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, TauData> hashMapTauData = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, ArrayList<Integer>> tauHashMap1 = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, ArrayList<Integer>> tauHashMap2 = new ConcurrentHashMap<>();
  
  // ################# running #######################
  
  /**
   * Private constructor -> please use {@link #getInstance()}
   */
  private MicrophoneGCCTester() {
    System.out.println("Microphone GCC analysis program was started.");
    System.out.println("________________________________________________________________________________");
    delayAnalysis = new DelayAnalysis();
    
    Path path1 = FileSystems.getDefault().getPath("c://temp//TauValuesGCCTest_Array1.txt");
    Path path2 = FileSystems.getDefault().getPath("c://temp//TauValuesGCCTest_Array2.txt");
    try {
      Files.deleteIfExists(path1);
      Files.deleteIfExists(path2);
    } catch (IOException e) {
      logErr(e.getMessage(), e);
    }
    
    
    // Start the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }
  
  /**
   * Singleton method to get the {@link  MicrophoneGCCTester} object
   * @return  MicrophoneGCCTester
   */
  public static synchronized  MicrophoneGCCTester getInstance()
  {
    if (instance==null){
      instance = new  MicrophoneGCCTester();
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
      setChanged();
      notifyObservers(NOTIFY_CONNECTION);
      
      try {
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

          if (isConnected()) {
            runAnalysis();
          } else {
            break;
          }
          
          ctr += sleepMillis;
          if (ctr >= 1000) {
            setChanged();
            notifyObservers(NOTIFY_STATE);
            ctr = 0;
          }
        }
        
        if(isConnected()){
          delayAnalysis=null;
        }
        
      } catch (Exception e) {
        logErr(e.getMessage(),e);
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        dispose();
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          logErr(e.getMessage(),e1);
        }
      }
    }
    MicArray3D.getInstance().dispose();
    return;
  }
  
  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isConnected() {
    return runAnalysis;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() {
    if (guard!=null)
    {
      runGuard = false;
      guard.interrupt();
      try { guard.join(); } catch (Exception e) {}
    }
    super.dispose();
  }

  @Override
  public AHardware getParent() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) {
    // TODO Auto-generated method stub
    return null;
  }
    
  /**
   * Starting the test and playing audio to the output
   */
  public void startTest(){
    wavePlayer.playSound(new TestData().getPath(TestData.TEST[9]));
    dataAddingAllowed=Boolean.TRUE;

    new java.util.Timer().schedule( 
            new java.util.TimerTask() {
                @Override
                public void run() {
                  dataAddingAllowed=Boolean.FALSE;
                  runAnalysis=Boolean.TRUE;
                }
            }, 
            TIME
    );
  }
  
  /**
   * Directly adding audio data to the processing pipeline
   * @param framesPerChannel
   */
  public void addAudioDataToQueue(float[][] framesPerChannel){
    audioDataQueue2.add(framesPerChannel);
  }
  
  /**
   * Running the analysis process
   */
  private void runAnalysis(){
    System.out.println("InputData sample size: "+audioDataQueue2.size()/2+" [frames] \n");
    while(!audioDataQueue2.isEmpty()){
      float[][] audioInputData1 = audioDataQueue2.poll();
      float[][] audioInputData2 = audioDataQueue2.poll();
      
      for (int i = 0; i < RmeHdspMadi.CHANNEL_COUNT; i++) {
        System.arraycopy(audioInputData1[i], 0, audioInputData2x[i], 0, AudioInputConstants.FRAME_SIZE);
        System.arraycopy(audioInputData2[i], 0, audioInputData2x[i], AudioInputConstants.FRAME_SIZE, AudioInputConstants.FRAME_SIZE);
      }
      
      MicArrayState state = MicArrayState.getCurrentState();
      
      // start array 1 analysis process
      fillAudioDataHashmap(audioInputData2x, hashMapAudioData, state.positions, MicArray.ARRAY1);
      if(hashMapAudioData.size()>1){
        delayAnalysis.run(hashMapAudioData, hashMapTauData, REF_MIC_ID_A1);
        
        // write tau data to text file
        int[] taus = new int[31];
        for (int i = 1; i < 32; i++) {
          TauData tdata = hashMapTauData.get(REF_MIC_ID_A1+","+i);
          if(tdata==null)
            continue;
          taus[i-1]=tdata.getTauValue();
        }
        writetoTextFile(taus, 1);
        
        // put tau data in hashmap for variance calculation
        for(Entry<String, TauData> entry  : hashMapTauData.entrySet()){
          TauData tData = entry.getValue();
          ArrayList<Integer> list = tauHashMap1.get(tData.getMicId());
          if(list==null)
            tauHashMap1.put(tData.getMicId(), new ArrayList<Integer>());
          ArrayList<Integer> list1 = tauHashMap1.get(tData.getMicId());
          list1.add(tData.getTauValue());
        }
      }
      // end array 1
      
      // start array 2 analysis process
      fillAudioDataHashmap(audioInputData2x, hashMapAudioData, state.positions, MicArray.ARRAY2);
      if(hashMapAudioData.size()>1){
        delayAnalysis.run(hashMapAudioData, hashMapTauData, REF_MIC_ID_A2);
        
        // write tau data to text file
        int[] taus = new int[31];
        for (int i = 1; i < 32; i++) {
          TauData tdata = hashMapTauData.get(REF_MIC_ID_A2+","+i);
          if(tdata==null)
            continue;
          taus[i-1]=tdata.getTauValue();
        }
        writetoTextFile(taus, 2);
        
        // put tau data in hashmap for variance calculation
        for(Entry<String, TauData> entry  : hashMapTauData.entrySet()){
          TauData tData = entry.getValue();
          ArrayList<Integer> list = tauHashMap2.get(tData.getMicId());
          if(list==null)
            tauHashMap2.put(tData.getMicId(), new ArrayList<Integer>());
          ArrayList<Integer> list1 = tauHashMap2.get(tData.getMicId());
          list1.add(tData.getTauValue());
        }
      }
    }
    // end array 2
        
    // data evaluation with variance calculation and command line print out
    System.out.println("- Evaluation report:");
    System.out.println("---");
    double[] variances1 = null;
    double[] variances2 = null;
    if(!tauHashMap1.isEmpty())
      variances1 = getVariances(tauHashMap1);
    if(!tauHashMap2.isEmpty())
      variances2 = getVariances(tauHashMap2);
    if(variances1!=null){
      System.out.println("Microphone Array 1 / TV");
      plotVariancesData(variances1, REF_MIC_ID_A1);
      printMicrophoneErrorInfo(variances1, REF_MIC_ID_A1);
    }
    System.out.println("\n---");
    if(variances2!=null){
      System.out.println("Microphone Array 2 / CEILING");
      plotVariancesData(variances2, REF_MIC_ID_A2);
      printMicrophoneErrorInfo(variances2, REF_MIC_ID_A2);
    }
    
    System.out.println("\nProgram has finished.");
    System.out.println("Info: Microphone ID goes from i=0, ...,31 per array.");
    System.out.println("Info: You can find all TAU values in c:/Temp/TauValuesGCCTest_Array1/2.txt!");
    System.out.println("________________________________________________________________________________");
    dispose();
  }
  
  /**
   * Fill the hashmaps with the audio and position data
   * @param framesPerChannel - float[n][m] array with n channels and m audio samples
   * @param audioDataHM - ConcurrentHashMap<Integer, AudioData>
   * @param positions - Point3d[] with microphone positions
   * @param array - {@link MicArray}
   */
  private void fillAudioDataHashmap(float[][] framesPerChannel, ConcurrentHashMap<Integer, AudioData> audioDataHM, Point3d[] positions, MicArray array){
    audioDataHM.clear();
    
    int shift = 0;
    if(array==MicArray.ARRAY2)
      shift=RmeHdspMadi.CHANNEL_COUNT/2;
    
    for (int i = 0; i < RmeHdspMadi.CHANNEL_COUNT/2; i++) {
      audioDataHM.put(i, new AudioData(i, framesPerChannel[i+shift], Boolean.TRUE, positions[i+shift]));
    }
  }
  
  // ################# data statistics methods #####################
  
  /**
   * Get the variances of the single channels
   * @param tauQueue - Queue<int[]>
   * @return double[]
   */
  private double[] getVariances(ConcurrentHashMap<Integer, ArrayList<Integer>> tauHashMap){
    double[] variances = new double[tauHashMap.size()];
    
    for (int j = 0; j < variances.length; j++) {
      ArrayList<Integer> list = tauHashMap.get(j+1);
      if(list==null)
        continue;
      int[] tauPerChannel = new int[list.size()];
      for (int i = 0; i < list.size(); i++) {
        tauPerChannel[i]=list.get(i);
      }
      variances[j]=getVariance(tauPerChannel);
    }
    
    return variances;
  }
  
  /**
   * Get the mean value of an int[] array
   * @param data
   * @return double
   */
  private double getMean(int[] data) {
    double sum = 0;
    for (int a : data)
      sum += a;
    return sum / data.length;
  }

  /**
   * Get the variance of an set of int[] data
   * @param data
   * @return double
   */
  private double getVariance(int[] data) {
    double mean = getMean(data);
    double temp = 0;
    for (int a : data)
      temp += (mean - a) * (mean - a);
    return temp / data.length;
  }
  
  // ################# data outputs #####################
  
  /**
   * Print the variances of the microphone pairs to command line
   * @param variances - double[] array
   * @param refMic - int, ID of the reference microphone
   */
  private void plotVariancesData(double[] variances, int refMic){
    System.out.print("Mic-Pair:\t");
    for (int i = 1; i < variances.length+1; i++)
      System.out.print(refMic+"/"+i+"\t");
    System.out.println();
    
    System.out.print("Variance:\t");
    for (int j = 0; j < variances.length; j++) {
      System.out.print(String.format(Locale.US,"%5.2f\t",variances[j]));
    }
    System.out.println("\n");
  }
  
  /**
   * Print out the error microphones to command line
   * @param vars, double[] array with variances
   * @param refMicId, int - reference microphone id
   */
  private void printMicrophoneErrorInfo(double[] vars, int refMicId){
    System.out.println("Reference microphone: "+refMicId);
    
    System.out.println("Disturbed Microphones:");
    System.out.print("\t0 (low, var>50):\t");
    for (int i = 0; i < vars.length; i++)
      if(vars[i]>=50.0 && vars[i]<100.0)
        System.out.print(i+1+", ");
    System.out.println();
    
    System.out.print("\t1 (mid, var>100):\t");
    for (int i = 0; i < vars.length; i++)
      if(vars[i]>=100.0 && vars[i]<200.0)
        System.out.print(i+1+", ");
    System.out.println();
    
    System.out.print("\t2 (high, var>200):\t");
    for (int i = 0; i < vars.length; i++)
      if(vars[i]>=200.0)
        System.out.print(i+1+", ");
    System.out.println();
  }
  
  /**
   * Write data to text file.
   * @param data - int[] array
   * @param arrayNumber - number of the array, to separate the text files
   */
  private void writetoTextFile(int[] data, int arrayNumber){
    BufferedWriter writer = null;
    try {
        //create a temporary file
        File logFile = new File("c://temp//TauValuesGCCTest_Array"+arrayNumber+".txt");

        writer = new BufferedWriter(new FileWriter(logFile, true));
        for (int i = 0; i < data.length; i++) {
          writer.write(data[i]+";");
        }
        writer.newLine();
    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        try {
            writer.close();
        } catch (Exception e) {
        }
    }
  }
  
  //######################## main ############################
  
  public static void main(String[] args) {
    Thread thread = new Thread(new Runnable() {
      @Override
      public void run() {
        RmeHdspMadi.getInstance();
        MicrophoneGCCTester gccTest = MicrophoneGCCTester.getInstance();
        gccTest.setVerbose(1);
        gccTest.startTest();
      }
    });
    thread.setName("Mic.GCC.Test.Thread");
    thread.start();
  }

  
  
}
