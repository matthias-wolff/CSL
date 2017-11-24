package de.tucottbus.kt.csl.hardware.micarray3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.hardware.ACompositeHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.led.ALedController;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.Beamformer3D;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.swt.ColorMeta;

/**
 * Abstract base class of the {@link MicArrayCeiling} and {@link MicArrayViewer}
 * 2D microphone sub-arrays. Each sub-array consists of 32 BeyerDynamic MM1
 * microphones (which do not have own hardware wrappers but are accessible
 * through this class). The 2D sub-arrays further contain an LED controller
 * providing access to the background and single microphone illumination.
 * 
 * @author Matthias Wolff
 * @author Martin Birth
 */
public abstract class AMicArray3DPart 
extends ACompositeHardware implements Observer 
{

  // -- Static fields and initialization --
  protected static ColorMeta[] levelColors = new ColorMeta[51]; // 1 color per 2 dBs
  static
  {
    // Initialize level colors
    ColorMeta clo = new ColorMeta(0x000408);
    ColorMeta cm1 = new ColorMeta(0x05234B);
    ColorMeta cm2 = new ColorMeta(0x4B4B00);
    ColorMeta chi = new ColorMeta(0x660006);
    for (int l=-100/*dB*/; l<-60/*dB*/; l+=2)
      levelColors[l/2+50] 
        = clo; 
    for (int l=-60/*dB*/; l<-36/*dB*/; l+=2)
      levelColors[l/2+50] 
        = LCARS.interpolateColors(clo,cm1,(l+60f)/24f);
    for (int l=-36/*dB*/; l<-24/*dB*/; l+=2)
      levelColors[l/2+50] 
        = LCARS.interpolateColors(cm1,cm2,(l+36f)/12f);
    for (int l=-24/*dB*/; l<=0/*dB*/; l+=2)
      levelColors[l/2+50] 
        = LCARS.interpolateColors(cm2,chi,(l+24f)/24f);

    // TODO: Initialize delay colors
  }
  
  // -- Fields --
  private final   ExecutorService illuminationService;
  private final   ALedController  ledController;
  protected Beamformer3D          beamformer3d;
  private final   int             minMicId;
  private final   int             maxMicId;
  protected int                   illuminationMode;
  protected final boolean[]       activeMics;

  // -- Implementation of AHardware --
  
  protected AMicArray3DPart() 
  {
    // Initialize
    beamformer3d     = Beamformer3D.getInstance();
    ledController    = getLedController();
    minMicId         = getMinMicId();
    maxMicId         = getMaxMicId();
    illuminationMode = MicArray3D.ILLUMINATION_LEVEL;
    activeMics       = new boolean[32];
    Arrays.fill(activeMics, true);
    
    // Initialize illumination service
    illuminationService 
      = Executors.newSingleThreadExecutor(getExecutorThreadFactory(
          "illuminationService"));

    // Register as observer
    beamformer3d.addObserver(this);
  }

  @Override
  public void dispose() 
  {
    beamformer3d.deleteObserver(this);
    illuminationService.shutdownNow();
    super.dispose();
  }

  @Override
  public final MicArray3D getParent() 
  {
    return MicArray3D.getInstance();
  }
  
  // -- Implementation of ACompositeHardware --

  @Override
  public Collection<AHardware> getChildren() 
  {
    ArrayList<AHardware> children = new ArrayList<AHardware>();
    children.add(getLedController());
    return children;
  }

  // -- Implementation of Observer --

  @Override
  public void update(Observable o, Object arg) 
  {
    if (o==beamformer3d)
      updateFromBeamformer3D(arg);
  }

  /**
   * Processes updates from the {@link Beamformer3D} hardware wrapper.
   * 
   * @param arg
   *          The argument passed to the {@code notifyObservers} method.
   */
  protected void updateFromBeamformer3D(Object arg)
  {
    if (Beamformer3D.NOTIFY_LEVEL.equals(arg))
      if (illuminationMode==MicArray3D.ILLUMINATION_LEVEL)
        illuminate(MicArray3D.ILLUMINATION_LEVEL);
  }
  
  // -- Abstract methods --
  
  /**
   * Returns the smallest microphone ID. The microphone ID is identical with 
   * the ASIO channel ID on the {@link HammerfallAudioDevice_Zombie}.
   */
  public abstract int getMinMicId();

  /**
   * Returns the greatest microphone ID. The microphone ID is identical with 
   * the ASIO channel ID on the {@link HammerfallAudioDevice_Zombie}.
   */
  public abstract int getMaxMicId();
  
  /**
   * Returns the LED controller hardware wrapper of this 2D microphone array.
   * The LED controller provides access to the background and single microphone
   * illumination.
   */
  public abstract ALedController getLedController();
  
  /**
   * Returns the position of the center point of the microphone array in the
   * CLS coordinate system.
   * 
   * @return The coordinates in cm.
   */
  public abstract Point3d getPosition();
  
  /**
   * Returns the position of a single microphone relative to the center of the
   * 2D microphone array.
   * 
   * @param micId
   *          The microphone ID, [0...31] for {@link MicArrayViewer} and 
   *          [32...63] for {@link MicArrayCeiling}.
   * @return The relative coordinates in cm.
   * @throws IllegalArgumentException
   *           if {@link micId} is out of range.
   */
  public abstract Point3d getMicPosition(int micId)
  throws IllegalArgumentException;
  
  /**
   * Select a preset of active microphones.
   * @param presetId 
   *          The preset ID, [0...3]
   * @throws HardwareException 
   */
  public abstract void setMicArrayPreset(int presetId) throws HardwareException;

  // -- Getters and setters --
  
  /**
   * Returns the current state of the microphone array. The returned object is
   * a copy of internal data. Modifications will have no effect on the array.
   */
  public MicArrayState getState()
  {
   return getParent().getState();
  }
  
  /**
   * Returns the microphone array input level.
   */
  public float getLevel()
  {
    float level = Float.NEGATIVE_INFINITY;
    for (int micId=minMicId; micId<=maxMicId; micId++)
      level = Math.max(level,beamformer3d.getMicLevel(micId));
    return level;
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
    checkMicId(micId);
    return beamformer3d.getMicLevel(micId);
  }
  
  /**
   * Sets the microphone illumination mode.
   * 
   * @param mode
   *          The new illumination mode, one of the {@link 
   *          MicArray3D}{@code .ILLUMINATION_xxx} constants. 
   */
  public void setIlluminationMode(int mode)
  {
    illuminationMode = mode;
    illuminate(mode);
  }

  /**
   * Returns the current microphone illumination mode.
   * 
   * @return One of the {@link MicArray3D}{@code .ILLUMINATION_xxx} constants.
   */
  public int getIlluminationMode()
  {
    return illuminationMode;
  }

  /**
   * Sets the microphone arrays'a ambient light.
   * 
   * @param color
   *          The new color of the ambient light.
   * @throws HardwareException
   *          on hardware problems.
   */
  public void setAmbientLight(ColorMeta color)
  throws HardwareException
  {
    try
    {
      getLedController().setAmbientColor(color);
    }
    catch (Exception e)
    {
      throw new HardwareException(e);
    }
  }

  /**
   * Returns the current color of the microphone arrays'a ambient light.
   *
   * @throws HardwareException
   *          on hardware problems.
   */
  public ColorMeta getAmbientLight()
  throws HardwareException
  {
    try
    {
      return getLedController().getAmbientColor();
    }
    catch (Exception e)
    {
      throw new HardwareException(e);
    }
  }
  
  /**
   * Activates or deactivates the sub-array. Activating or deactivating the
   * array means to activate or deactivate <em>all</em> microphones.
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
    for (int micId = minMicId; micId<=maxMicId; micId++)
      setMicActive(micId, active);
  }
  
  /**
   * Activates or deactivates a selection of microphones to the sub-array.
   * @param active - boolean[]
   * @throws HardwareException
   */
  public void setActiveSelection(boolean[] active)
  throws HardwareException
  {
    for (int micId = minMicId; micId<=maxMicId; micId++)
      setMicActive(micId, active[micId-minMicId]);
  }
  
  /**
   * Determines if the sub-array is active. The sub-array is active, if and only 
   * if at least one microphone is active.
   * 
   * @throws HardwareException
   *           on hardware problems.
   * @see #setMicActive(int, boolean)
   * @see #isMicActive(int)
   */  
  public boolean isActive()
  throws HardwareException
  {
    for (int micId = minMicId; micId<=maxMicId; micId++)
      if (isMicActive(micId))
        return true;
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
    if(this.getName()==MicArrayCeiling.getInstance().getName())
      micId-=MicArrayCeiling.getInstance().getMinMicId();
    activeMics[micId]=active;
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
    if(this.getName()==MicArrayCeiling.getInstance().getName())
      micId-=getMinMicId();
    return activeMics[micId];
  }

  // -- Illumination methods --
  
  /**
   * Returns the LED illumination color for a microphone input level.
   * 
   * @param level
   *          The level in dB.
   * @return The color.
   */
  protected static ColorMeta getLevelColor(float level)
  {
    if (level<-85)
      return levelColors[50];
    int colorIndex = Math.round(level/2+50);
    colorIndex = Math.min(Math.max(colorIndex,0),levelColors.length-1);
    return levelColors[colorIndex];
  }

  /**
   * Returns the LED illumination color for a microphone delay.
   * 
   * @param delay
   *          The delay in seconds.
   * @return The color.
   */
  protected ColorMeta getDelayColor(float[] delays, int micId)
  {
    // TODO: Revise!
    float minDelay = Float.MAX_VALUE;
    float maxDelay = Float.MIN_VALUE;
    float meanDelay = 0f;
    for (int i=getMinMicId(); i<=getMaxMicId(); i++)
    {
      minDelay = Math.min(minDelay,delays[i]);
      maxDelay = Math.max(maxDelay,delays[i]);
      meanDelay += delays[i];
    }
    meanDelay /= (getMaxMicId()-getMinMicId()+1);
    float normDelay = 2*((delays[micId]-minDelay)/(maxDelay-minDelay)-0.5f);
    System.err.println("normDelay="+normDelay);
    
    ColorMeta cL = new ColorMeta(0.25f,0f,0f);
    ColorMeta cM = new ColorMeta(0.25f,0.25f,0.25f);
    ColorMeta cH = new ColorMeta(0f,0f,0.5f);

    ColorMeta c = new ColorMeta(cM,1f);
    if (normDelay<0)
      c = LCARS.interpolateColors(cM,cH,Math.min(1f,Math.abs(normDelay*1.5f)));
    else if (normDelay>0)
      c = LCARS.interpolateColors(cM,cL,Math.min(1f,Math.abs(normDelay*1.5f)));

    return c;
  }

  /**
   * Returns the LED illumination color for a microphone gain.
   * 
   * @param gain
   *          The gain.
   * @return The color.
   */
  protected static ColorMeta getGainColor(float delay)
  {
    // TODO: Implement getGainColor;
    return new ColorMeta(0.25f,0.25f,0.25f);
  }
  
  /**
   * Controls the microphone LEDs.
   * 
   * @param mode
   *          The illumination mode.
   */
  protected void illuminate(int mode)
  {
    illuminationService.execute(new Runnable() 
    {
      @Override
      public void run() 
      {
        MicArrayState mas = getState();
        for (int micId=minMicId; micId<=maxMicId; micId++)
        {
          ColorMeta color = ColorMeta.BLACK;
          switch (mode)
          {
          case MicArray3D.ILLUMINATION_POS:
            color = levelColors[0];
            break;
          case MicArray3D.ILLUMINATION_LEVEL:
            color = getLevelColor(getMicLevel(micId));
            break;
          case MicArray3D.ILLUMINATION_DELAY:
            color = getDelayColor(mas.delays,micId);
            break;
          case MicArray3D.ILLUMINATION_GAIN:
            color = getGainColor(mas.gains[micId]);
            break;
          }

          try {
            if(isMicActive(micId)){
              ledController.setColor(micId-minMicId,color);
            } else {
              ledController.setColor(micId-minMicId,ColorMeta.BLACK);
            }
          }
          catch (Exception e) {
           logErr(e.getMessage(),e);
          }
        }
      }
    });
  }
  
  // -- Auxiliary methods --
  
  /**
   * Checks the specified microphone ID.
   * 
   * @param micId
   *          The microphone ID to be checked.
   * @throws IllegalArgumentException
   *          if {@code micId} is out of range, i.e. &lt; {@link #getMinMicId()}
   *          or &gt;{@link #getMaxMicId()}.
   */
  protected final void checkMicId(int micId)
  throws IllegalArgumentException
  {
    if (micId<minMicId || micId>maxMicId)
      throw new IllegalArgumentException("Invalid micId "+micId+" (not in ["
        + minMicId+","+maxMicId+"])");
  }

}
