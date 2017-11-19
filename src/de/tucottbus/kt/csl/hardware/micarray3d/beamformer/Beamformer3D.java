package de.tucottbus.kt.csl.hardware.micarray3d.beamformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.audio.output.MAudioDeviceLine34;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArray3D;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayViewer;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb.DSBeamformer;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

/**
 * Virtual hardware device wrapper of the 3D beamformer. 
 * 
 * @author Martin Birth
 * @author MAtthias Wolff
 */
public final class Beamformer3D extends DSBeamformer implements Observer, Runnable
{
  // -- Constants --
  public  static final String NOTIFY_LEVEL      = "NOTIFY_LEVEL";
  private static final float  LEVEL_ATTENUATION = 1; // dB per 20 ms;

  // -- Fields --
  
  /**
   * The level filtering service. Falling slopes of microphone input levels are
   * smoothed out at a rate of {@link #LEVEL_ATTENUATION} per ASIO frame 
   * interval (20 ms). Rising slopes are not filtered.
   */
  private ScheduledExecutorService levelFilterService;
  
  /**
   * Last known levels of the 64 microphone input channels.
   */
  private volatile float[] levels;

  // -- Singleton implementation --
  private static volatile Beamformer3D singleton = null;
  
  /**
   * Manages the audio connection.
   */
  protected Thread guard;
  
  /**
   * Boolean to control the thread loops
   */
  private boolean runGuard = false;

  /**
   * Returns the singleton instance. 
   */
  public static synchronized Beamformer3D getInstance()
  {
    if (singleton==null)
      singleton = new Beamformer3D();
    return singleton;
  }
  
  /**
   * Creates the singleton instance. 
   */
  private Beamformer3D() 
  {
    // starting the guard thread
    guard = new Thread(this,getClass().getSimpleName()+".guard");
    guard.start();
  }
  
  @Override
  public void run() {
    runGuard = true;
    final int sleepMillis = 100;
    int ctr = 0;
    
    // Initialize audio input device
    hammerfallAudioDevice = RmeHdspMadi.getInstance();
    hammerfallAudioDevice.addObserver(this);
    
    // Initialize oudio output device
    mAudioDeviceLine34 = MAudioDeviceLine34.getInstance();
    
    // Initialize arrays
    levels = new float[64];
    Arrays.fill(levels,Float.NEGATIVE_INFINITY);

    // Initialize level filter service
    levelFilterService 
      = Executors.newSingleThreadScheduledExecutor(getExecutorThreadFactory(
          "levelFilterService"));
    levelFilterService.scheduleAtFixedRate(new Runnable() 
    {
      @Override
      public void run() 
      {
        for (int i=0; i<levels.length; i++)
          levels[i] -= LEVEL_ATTENUATION;
        setChanged(); notifyObserversAsync(NOTIFY_LEVEL);
      }
    },20,20,TimeUnit.MILLISECONDS);
    
    // - Run connection
    while (runGuard) {
      try {
        setChanged();
        notifyObservers(NOTIFY_CONNECTION);

        try {
          Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {}
        if (!runGuard)
          break;

        if (!isConnected())
          break;

        ctr += sleepMillis;
        if (ctr >= 1000) {
          setChanged();
          notifyObservers(NOTIFY_STATE);
          ctr = 0;
        }
        
      } catch (Exception e) {
        logErr(e.getMessage(), e);

        setChanged();
        notifyObservers(NOTIFY_CONNECTION);
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e1) {
          logErr(e1.getMessage(), e1);
        }
      }
    }
  }
  
  // -- Implementation of AHardware --

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose() 
  {
    if(hammerfallAudioDevice!=null)
      hammerfallAudioDevice.deleteObserver(this);
    
    if(levelFilterService!=null)
      levelFilterService.shutdownNow();
    
    if (guard!=null)
    {
      runGuard = false;
      guard.interrupt();
      try { guard.join(); } catch (Exception e) {}
    }
    super.dispose();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AHardware getParent() 
  {
    return MicArray3D.getInstance();
  }
  
  // -- Implementation of ACompositeHardware --

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<AHardware> getChildren()
  {
    ArrayList<AHardware> children = new ArrayList<AHardware>();
    children.add(hammerfallAudioDevice);
    children.add(mAudioDeviceLine34);
    return children;  
  }  

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() 
  {
    return "3D Beamformer (Virtual Device)";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  // -- Implementation of Observer --

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(Observable o, Object arg) 
  {
    if (o==hammerfallAudioDevice)
      updateFromHammerfallAudioDevice(arg);
  }

  /**
   * Processes updates from the {@link HammerfallAudioDevice_Zombie}.
   * 
   * @param arg
   *          The argument passed to the {@code notifyObservers} method.
   */
  protected void updateFromHammerfallAudioDevice(Object arg)
  {
    if (RmeHdspMadi.NOTIFY_RMS.equals(arg))
    {
      // Track levels
      for (int i=0; i<levels.length; i++)
      {
        float rms = hammerfallAudioDevice.getRMS(i);
        float level = 20*(float)Math.log10(rms);
        levels[i] = Math.max(level,levels[i]);
      }
    }
  }

  // -- Getters and setters --
  
  /**
   * Determines if the underlying audio input is calibrated.
   * 
   * @see #calibrate()
   */
  public boolean isCalibrated()
  {
    return hammerfallAudioDevice.isCalibrated();
  }
  
  /**
   * Returns the current microphone input level.
   * 
   * @param micId
   *          The microphone ID, [0...31] for {@link MicArrayViewer} and 
   *          [32...63] for {@link MicArrayCeiling}.
   * @return The level, [-&infin;...0] dB.
   * @throws IllegalArgumentException
   *           if {@link micId} is out of range.
   */
  public float getMicLevel(int micId)
  throws IllegalArgumentException
  {
    if (micId<0 || micId>=levels.length)
      throw new IllegalArgumentException("micId "+micId+" out of range [0..63]");
    return levels[micId];
  }
  
  /**
   * Sending the audio data directly to the DS-beamformer and put it on the output device.
   * @param inBuffer
   * @see DSBeamformer
   */
  public void sendAudioToBeamformer(float[] inBuffer){
    MicArrayState state = MicArray3D.getInstance().getState();
    if(state==null)
      return;
    delayAndSumBeamforming(state, inBuffer);
    sendAudioToOutput(outFloatBuffer);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void sendAudioToOutput(float[] audioData) {
    mAudioDeviceLine34.sendAudioData(audioData);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public float[] getAudioOutputData() {
    return outFloatBuffer;
  }
  
  // -- Operations --

  /**
   * Calibrates the underlying audio input device.
   * 
   * @returns An array of calibration gain factors, one per channel.
   * @throws IllegalStateException
   *           if the calibrator has not yet collected sufficient data for 
   *           calibration.
   * @see #isCalibrated()
   */
  public float[] calibrate()
  throws IllegalStateException
  {
    return hammerfallAudioDevice.calibrate();
  }

}
