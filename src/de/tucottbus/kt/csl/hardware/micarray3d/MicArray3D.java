package de.tucottbus.kt.csl.hardware.micarray3d;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.ACompositeHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.Beamformer3D;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.lcars.contributors.ElementContributor;

// TODO: Make sum level observable
// TODO: Provide sum audio stream

/**
 * <b>Incubating:</b> The 3D microphone array hardware wrapper. The 3D
 * microphone array consists of the {@linkplain MicArrayCeiling ceiling} and
 * {@linkplain MicArrayViewer main viewer} sub-arrays, and of the {@linkplain 
 * Beamformer3D 3D beamformer virtual device}. This is a top-level device.
 * 
 * @author Matthias Wolff
 * @author Martin Birth
 */
public final class MicArray3D extends ACompositeHardware 
{
  /**
   * No microphone illumination.
   */
  public static final int ILLUMINATION_NONE = 0;

  /**
   * Visualize microphone positions trough LEDs. This displays a static dim 
   * light by each microphone.
   */
  public static final int ILLUMINATION_POS = 1;
  
  /**
   * Visualize microphone levels trough LEDs.
   */
  public static final int ILLUMINATION_LEVEL = 2;
  
  /**
   * Visualize microphone delays trough LEDs.
   */
  public static final int ILLUMINATION_DELAY = 3;
  
  /**
   * Visualize microphone gains trough LEDs.
   */
  public static final int ILLUMINATION_GAIN = 4;
  
  private MicArrayCeiling micArrayCeiling;
  private MicArrayViewer  micArrayViewer;
  private Beamformer3D    beamformer3D;
  private DoAEstimator    doAEstimator;
  
  /**
   * Current microphone array state
   */
  private MicArrayState state;

  // -- Singleton implementation --
  
  private static volatile MicArray3D singleton = null;
  
  /**
   * Returns the singleton instance. 
   */
  public static synchronized MicArray3D getInstance()
  {
    if (singleton==null)
      singleton = new MicArray3D();
    return singleton;
  }
  
  /**
   * Creates the singleton instance.
   */
  private MicArray3D() 
  {
    micArrayCeiling = MicArrayCeiling.getInstance();
    micArrayViewer  = MicArrayViewer.getInstance();
    beamformer3D    = Beamformer3D.getInstance();
    doAEstimator    = DoAEstimator.getInstance();
  }

  // -- Implementation of ACompositeHardware --

  @Override
  public String getName() 
  {
    return "3D Microphone Array";
  }

  @Override
  public AHardware getParent() 
  {
    return CslHardware.getInstance();
  }

  @Override
  public Collection<AHardware> getChildren() 
  {
    ArrayList<AHardware> children = new ArrayList<AHardware>();
    beamformer3D = Beamformer3D.getInstance();
    doAEstimator = DoAEstimator.getInstance();
    micArrayViewer = MicArrayViewer.getInstance();
    micArrayCeiling = MicArrayCeiling.getInstance();
    children.add(beamformer3D);
    children.add(doAEstimator);
    children.add(micArrayViewer);
    children.add(micArrayCeiling);
    return children;
  }

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    // TODO Auto-generated method stub
    return null;
  }

  // -- Getters and setters --
  
  /**
   * Returns the current state of the microphone array. The returned object is
   * a copy of internal data. Modifications will have no effect on the array.
   * 
   * <p>TODO: Cache microphone array state (this method may be called 
   * frequently)!</p>
   */
  public MicArrayState getState()
  {
    state = MicArrayState.getCurrent();
    return state;
  }
  
  /**
   * Sets the array into a new state. This involves
   * <ul>
   *   <li>activating/deactivating microphones,</li>
   *   <li>setting a steering vector (delays and gains of microphone signals), 
   *   and</li>
   *   <li>moving the ceiling array.</li>
   * </ul>
   * 
   * @param micArrayState
   *          The new microphone array state.
   * @throws HardwareException
   *           if the new state did not become effective because of hardware or 
   *           communication failures.
   */
  public void setState(MicArrayState micArrayState)
  throws HardwareException
  {
    if(micArrayState!=null)
      state=micArrayState;
  }
  
  /**
   * Get the output level of the microphone array.
   * @return float value
   */
  public float getLevel()
  {
    return beamformer3D.getBeamformerOutputLevel();
  }
  
  public void setCeilingArrayYPosition(float position)
  {
    Point3d p = micArrayCeiling.getPosition();
    p.setY(position);
    try {
      micArrayCeiling.setPosition(p);
    } catch (IllegalArgumentException e) {
      logErr(e.getMessage(), e);
    } catch (HardwareException e) {
      logErr(e.getMessage(), e);
    }
  }
  
  public float getCeilingArrayYPosition()
  {
    return (float) micArrayCeiling.getPosition().y;
  }
  
  /**
   * Determines if the microphone array is calibrated.
   * 
   * @see #calibrate()
   */
  public boolean isCalibrated()
  {
    return beamformer3D.isCalibrated();
  }
  
  /**
   * Return the steering target in the CSL coordinate system.
   * 
   * @return The coordinates in cm. 
   */
  public Point3d getTarget()
  {
    return doAEstimator.getTargetSource();
  }

  /**
   * Sets a static steering target.
   * 
   * @param target
   *          The target, or {@code null} to enable auto-tracking.
   */
  public void setTarget(Point3d target)
  {
    doAEstimator.setTargetSource(target);
  }
  
  /**
   * Activates or deactivates the array. Activating or deactivating the
   * array means to activate or deactivate <em>both</em> sub-arrays.
   *  
   * @param active
   *          The new activation state.
   * @throws HardwareException
   *          on hardware problems.
   * @see #isActive()
   * @see #setMicActive(int, boolean)
   */
  public void setActive(boolean active)
  throws HardwareException
  {
    micArrayViewer.setActive(active);
    micArrayCeiling.setActive(active);
  }
  
  /**
   * Determines if the microphone array is active. The array is active, if and 
   * only if at least one of the sub-arrays ({@link MicArrayViewer} and {@link 
   * MicArrayCeiling}) is active.
   * 
   * @throws HardwareException
   *           on hardware problems.
   * @see #setActive(boolean)
   * @see #setMicActive(int, boolean)
   */
  public boolean isActive()
  throws HardwareException
  {
    if (micArrayViewer.isActive()) return true;
    if (micArrayCeiling.isActive()) return true;
    return false;
  }
  
  /**
   * Activates or deactivates a microphone.
   * 
   * @param micId
   *          The microphone ID, [0...31] for the {@linkplain MicArrayViewer
   *          viewer sub-array} or [32...63] for the {@linkplain MicArrayCeiling
   *          ceiling sub-array}.
   * @param active
   *          The new activation state.
   * @throws IllegalArgumentException
   *           if {@code midId} is out of the range [0...63].
   * @throws HardwareException
   *           on hardware problems.
   * @see #isMicActive(int)
   * @see #setActive(boolean)
   */
  public void setMicActive(int micId, boolean active)
  throws IllegalArgumentException, HardwareException
  {
    checkMicId(micId);
    if(micId<=31){
      micArrayViewer.setMicActive(micId, active);
    }
    if(micId>31 && micId<=63){
      micArrayCeiling.setMicActive(micId, active);
    }
  }
  
  /**
   * Get a boolean[] array of active channels/microphones of the 3D array.
   * @return  boolean[ID], The microphone ID, [0...31] for the {@linkplain MicArrayViewer
   *          viewer sub-array} or [32...63] for the {@linkplain MicArrayCeiling
   *          ceiling sub-array}.
   */
  public boolean[] getActiveMics(){
    boolean[] active = new boolean[64];
    try{
      for (int i = micArrayViewer.getMinMicId(); i <= micArrayViewer.getMaxMicId(); i++) {
        active[i]=micArrayViewer.isMicActive(i);
      }
      for (int i = micArrayCeiling.getMinMicId(); i <= micArrayCeiling.getMaxMicId(); i++) {
        active[i]=micArrayCeiling.isMicActive(i);
      }
    } catch(Exception e) {
      logErr(e.getMessage(), e);
    }
    return active;
  }
  
  /**
   * Determines if a microphone is active.
   * 
   * @param micId
   *          The microphone ID, [0...31] for the {@linkplain MicArrayViewer
   *          viewer sub-array} or [32...63] for the {@linkplain MicArrayCeiling
   *          ceiling sub-array}.
   * @throws IllegalArgumentException
   *           if {@code midId} is out of the range [0...63].
   * @throws HardwareException
   *           on hardware problems.
   * @see #setMicActive(int, boolean)
   * @see #isActive()
   */
  public boolean isMicActive(int micId)
  throws IllegalArgumentException, HardwareException
  {
    checkMicId(micId);
    MicArrayState mas = getState();
    return mas.activeMics[micId];
  }
  
  // -- Operations --
  
  /**
   * Calibrates the microphone input channels.
   * 
   * @see #isCalibrated()
   */
  public void calibrate()
  {
    beamformer3D.calibrate();
  }
  
  // -- Auxiliary methods --
  
  protected void checkMicId(int micId)
  throws IllegalArgumentException
  {
    if (micId<0 || micId>63)
      throw new IllegalArgumentException("Invalid micId "+micId
        + " (not in [0,63])");  
  }
  
  // -- Main (just for testing and debugging) --
  
  public static void main(String[] args)
  {
    MicArray3D micArray3D = MicArray3D.getInstance();
    micArray3D.printTree("");
    
    System.out.println("\nCommands:\n- c: calibrate\n- l: level dump\n- q: quit");
    while (true)
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      try 
      { 
        String input = in.readLine();
        try
        {
          if ("q".equals(input))
            break;
          else if ("c".equals(input))
          {
            float[] calibrationGains = Beamformer3D.getInstance().calibrate();
            
            System.out.print(String.format(Locale.US,"Calibration gains\n +: "));
            for (int j=0; j<8; j++) System.out.print(String.format(Locale.US,"%5d ",j));
            System.out.println();
            for (int i=0; i<8; i++)
            {
              System.out.print(String.format(Locale.US,"%2d: ",i*8));
              for (int j=0; j<8; j++)
                System.out.print(String.format(Locale.US,"%5.3f ",calibrationGains[8*i+j]));
              System.out.println();
            }
          }
          else if ("l".equals(input))
          {
            final float[] levels = new float[64]; Arrays.fill(levels,0f);
            final int[] ctr = new int[1];
            System.out.print("Gathering stats ");
            RmeHdspMadi.getInstance().addObserver(new Observer()
            {
              @Override
              public void update(Observable o, Object arg)
              {
                if (RmeHdspMadi.NOTIFY_RMS.equals(arg))
                {
                  for (int i=0; i<levels.length; i++)
                    levels[i]+=RmeHdspMadi.getInstance().getLevel(i);

                  ctr[0]++;
                  if ((ctr[0]%10)==0)
                    System.out.print(".");
                  
                  if (ctr[0]==100)
                  {
                    RmeHdspMadi.getInstance().deleteObserver(this);
                    for (int i=0; i<levels.length; i++) levels[i]/=ctr[0];
                    System.out.print(String.format(Locale.US," done\nBase level: %5.1f dB\n +: ",levels[0]));
                    for (int j=0; j<8; j++) System.out.print(String.format(Locale.US,"%5d ",j));
                    System.out.println();
                    for (int i=0; i<8; i++)
                    {
                      System.out.print(String.format(Locale.US,"%2d: ",i*8));
                      for (int j=0; j<8; j++)
                        System.out.print(String.format(Locale.US,"%5.1f ",levels[8*i+j]-levels[0]));
                      System.out.println();
                    }
                  }
                }
              }
            });
          }
          else if (!("".equals(input)))
            System.err.println("Unknown command \""+input+"\"");
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
      catch (IOException e) { break; }
    }
    
    micArray3D.dispose();
  }
  
}
