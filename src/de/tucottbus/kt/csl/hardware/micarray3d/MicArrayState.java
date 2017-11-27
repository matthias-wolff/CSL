package de.tucottbus.kt.csl.hardware.micarray3d;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.dsb.Steering;

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
    mas.numberOfActiveMics = mas.activeMics.length;
    
    return mas;
  }
  
  // --  File operations --
  
  /**
   * Saves a microphone array state to a file.
   * 
   * @param state
   *          The state.
   * @param filePath
   *          The path name.
   * @throws FileNotFoundException
   *           if the file exists but is a directory rather than a regular file,
   *           does not exist but cannot be created, or cannot be opened for any
   *           other reason.
   * @throws SecurityException
   *           if a security manager exists and its <code>checkWrite</code>
   *           method denies write access to the file.
   * @throws IOException
   *           if I/O errors occur.
   */
  public static void toFile(MicArrayState state, String filePath)
  throws FileNotFoundException, SecurityException, IOException
  {
    FileOutputStream fout = new FileOutputStream(filePath);
    ObjectOutputStream oos = new ObjectOutputStream(fout);
    oos.writeObject(state);
    oos.close();
  }

  /**
   * Loads a microphone array state from a file.
   * 
   * @param filePath
   *          The path name.
   * @return The state
   * @throws FileNotFoundException
   *           if the file does not exist, is a directory rather than a regular
   *           file, or for some other reason cannot be opened for reading.
   * @throws SecurityException
   *           if a security manager exists and its <code>checkRead</code>
   *           method denies read access to the file.
   * @throws IOException
   *           if I/O errors occur.
   */
  public static MicArrayState fromFile(String filePath) 
  throws FileNotFoundException, SecurityException, IOException
  {
    try
    {
      FileInputStream fin = new FileInputStream(filePath);
      ObjectInputStream ois = new ObjectInputStream(fin);
      MicArrayState state = (MicArrayState) ois.readObject();
      ois.close();
      return state;
    }
    catch (ClassNotFoundException e)
    {
      // This cannot happen actually
      throw new IOException(e);
    }
  }

}

// EOF
