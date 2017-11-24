package de.tucottbus.kt.csl.hardware.micarray3d;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb.Steering;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * This class represents a state of the entire microphone array. It can be used
 * for saving profiles or for transferring data to LCARS display elements.
 * 
 * <h3>Elements:</h3>
 * <ul>
 *   <li>{@link #target}</li>
 *   <li>{@link #positions}</li>
 *   <li>{@link #trolleyPos}</li>
 *   <li>{@link #delays}</li>
 *   <li>{@link #gains}</li>
 *   <li>{@link #activeMics}</li>
 *   <li>{@link #numberOfActiveMics}</li>
 * </ul>
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Review {@link #getCurrent()} method.
 *     </li>
 *   <li>TODO: Implement <code>getNumberOfActiveMics()</code>.
 *     </li>
 * </ul>
 * 
 * @author Martin Birth
 * @author Matthias Wolff
 */
public class MicArrayState implements Serializable 
{
  private static final long serialVersionUID = 1L;
  private static final int  CH_NUM = 64;

  /**
   * The target point of the microphone array beam in the CSL coordinate system
   * (dimensions measured in centimeters).
   */
  public Point3d target = new Point3d();
  
  /**
   * The absolute positions of the microphones in the CSL coordinate system 
   * (dimensions measured in centimeters).
   */
  public Point3d[] positions = new Point3d[CH_NUM];

  /**
   * The position of the trolley carrying the ceiling sub-array. This is the
   * y-coordinate in the CSL coordinate system, measured in centimeters.
   */
  public double trolleyPos = 0;
  
  /**
   * The wave-front delay times of a point-shaped sound source at the {@link 
   * #target} position for all microphones (in seconds).
   * 
   * @see #steerVec
   */
  public float[] delays = new float[CH_NUM];

  /**
   * The signal delay times to steer the 3D array to a point-shaped sound source 
   * at the {@link #target} position for all microphones (in seconds).
   * 
   * @see #delays
   */
  public float[] steerVec = new float[CH_NUM];
  
  /**
   * The gain factors to steer the 3D array to a point-shaped sound source at 
   * the {@link #target} position for all microphones.
   */
  public float[] gains = new float[CH_NUM];
  
  /**
   * The microphone activation states.
   */
  public boolean[] activeMics = new boolean[CH_NUM];
  
  /**
   * The number of activated microphones.
   * 
   * @deprecated
   */
  @Deprecated
  public int numberOfActiveMics = 0;
  
  /**
   * The most recent known actual state.
   */
  private static MicArrayState cachedState;
  
  @Override
  public boolean equals(Object obj)
  {
    if (obj==null)
      return false;

    MicArrayState other = (MicArrayState)obj;
    
    if (!target.equals(other.target)                ) return false;
    if (trolleyPos!=other.trolleyPos                ) return false;
    if (numberOfActiveMics!=other.numberOfActiveMics) return false;
    
    for (int i=0; i<CH_NUM; i++)
    {
      if (!positions[i].equals(other.positions[i])) return false;
      if (delays[i]    !=other.delays[i]          ) return false;
      if (steerVec[i]  !=other.steerVec[i]        ) return false;
      if (gains[i]     !=other.gains[i]           ) return false;
      if (activeMics[i]!=other.activeMics[i]      ) return false;
    }
    
    return true;
  }
  
  @Override
  public String toString()
  {
    String s = "[";
    s += target;
    s += ", ("; for (int i=0; i<CH_NUM; i++) s+=positions[i]; s+=")";
    s += ", "+trolleyPos;
    s += ", ("; for (int i=0; i<CH_NUM; i++) s+=(i==0?"":", ")+delays[i]; s+=")";
    s += ", ("; for (int i=0; i<CH_NUM; i++) s+=(i==0?"":", ")+steerVec[i]; s+=")";
    s += ", ("; for (int i=0; i<CH_NUM; i++) s+=(i==0?"":", ")+gains[i]; s+=")";
    s += ", ("; for (int i=0; i<CH_NUM; i++) s+=(i==0?"":", ")+activeMics[i]; s+=")";
    s += ", "+numberOfActiveMics;
    return s;
  }
  
  /**
   * Get the current microphone array state.
   * 
   * @param target
   *          The target point, {@code null} for the {@linkplain 
   *          MicArray3D#DEFAULT_TARGET default target}.
   * 
   * @see MicArray3D
   */
  public static synchronized MicArrayState getCurrent()
  {
    if
    (
      cachedState!=null 
      && cachedState.target.equals(DoAEstimator.getInstance().getTargetSource())
      && cachedState.trolleyPos==MicArrayCeiling.getInstance().getPosition().y
      && cachedState.activeMics.equals(MicArray3D.getInstance().getActiveMics())
    )
    {
      return cachedState;
    } 
    else 
    {
      cachedState=getStateInt();
      return cachedState;
    }
  }
  
  /**
   * Returns a dummy microphone array state without accessing the underlying
   * hardware.
   */
  public static MicArrayState getDummy()
  {
    MicArrayState mas = new MicArrayState();
    
    // Get absolute positions
    Point3d offset = MicArrayViewer.POS_OFFSET;
    for (int i=0; i<32; i++)
    {
      mas.positions[i] = new Point3d(MicArrayViewer.micPos[i]);
      mas.positions[i].add(offset);
    }
    offset = MicArrayCeiling.POS_OFFSET;
    mas.trolleyPos = offset.y;
    for (int i=0; i<32; i++)
    {
      mas.positions[i+32] = new Point3d(MicArrayCeiling.micPos[i]);
      mas.positions[i+32].add(offset);
    }
    
    // Get steering info
    mas.target.set(CSL.ROOM.DEFAULT_POS);
    float[] tempDelays = Steering.getDelays(mas.positions, mas.target);
    float[] tempSteerVec = Steering.getSteeringVectorFromDelays(tempDelays);
    mas.delays = Arrays.copyOf(tempDelays, tempDelays.length);
    mas.steerVec =  Arrays.copyOf(tempSteerVec, tempSteerVec.length);
    
    // gain factors
    Arrays.fill(mas.gains,1f);
    
    // active channels/mics
    Arrays.fill(mas.activeMics,true);
    mas.numberOfActiveMics = getNumberOfActiveMics(mas.activeMics);
    
    return mas;
  }
  
  // --  File operations --
  
  /**
   * Safe state to file.
   * 
   * @param state MicArrayState
   * @param filePath String
   */
  public static void toFile(MicArrayState state, String filePath) {
    try {
      FileOutputStream fout = new FileOutputStream(filePath);
      ObjectOutputStream oos = new ObjectOutputStream(fout);
      oos.writeObject(state);
      oos.close();
    } catch (Exception e) {
      Log.err(e.getMessage(), e);
    }
  }

  /**
   * Get MicArrayState from file.
   * @param filePath String
   * @return MicArrayState
   */
  public static MicArrayState fromFile(String filePath) {
    MicArrayState state = null;
    try {
      FileInputStream fin = new FileInputStream(filePath);
      ObjectInputStream ois = new ObjectInputStream(fin);
      state = (MicArrayState) ois.readObject();
      ois.close();
    } catch (Exception e) {
      Log.err(e.getMessage(), e);
    }
    return state;
  }

  // -- Private methods --

  /**
   * Retrieves the microphone array state from the underlying hardware wrappers.
   */
  private static MicArrayState getStateInt()
  {
    DoAEstimator doAEstimator = DoAEstimator.getInstance();
    MicArrayViewer micArrayViewer = MicArrayViewer.getInstance();
    MicArrayCeiling micArrayCeiling = MicArrayCeiling.getInstance();
    MicArrayState mas = new MicArrayState();

    // Get absolute positions
    Point3d offset = micArrayViewer.getPosition();
    for (int i=0; i<32; i++)
    {
      mas.positions[i] = new Point3d(micArrayViewer.getMicPosition(i));
      mas.positions[i].add(offset);
    }
    offset = micArrayCeiling.getPosition();
    mas.trolleyPos = offset.y;
    for (int i=32; i<64; i++)
    {
      mas.positions[i] = new Point3d(micArrayCeiling.getMicPosition(i));
      mas.positions[i].add(offset);
    }
    
    // Get steering info
    mas.target.set(doAEstimator.getTargetSource());
    float[] tempDelays = Steering.getDelays(mas.positions, mas.target);
    float[] tempSteerVec = Steering.getSteeringVectorFromDelays(tempDelays);
    mas.delays = Arrays.copyOf(tempDelays, tempDelays.length);
    mas.steerVec =  Arrays.copyOf(tempSteerVec, tempSteerVec.length);
    
    // gain factors
    Arrays.fill(mas.gains,1f); // TODO: get real gains
    
    // active channels/mics
    mas.activeMics=MicArray3D.getInstance().getActiveMics();
    mas.numberOfActiveMics = getNumberOfActiveMics(mas.activeMics);
    
    return mas;
  }
  
  /**
   * Counts the active microphones.
   * 
   * @deprecated
   */
  @Deprecated
  private static int getNumberOfActiveMics(boolean[] activeMics)
  {
    int count = 0;
    for (int i = 0; i < activeMics.length; i++) 
      if(activeMics[i]) 
        count++;
    return count;
  }
  
}

// EOF
