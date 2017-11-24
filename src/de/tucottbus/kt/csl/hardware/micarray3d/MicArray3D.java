package de.tucottbus.kt.csl.hardware.micarray3d;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import javax.vecmath.Point3d;

import de.tucottbus.kt.csl.CSL;
import de.tucottbus.kt.csl.hardware.ACompositeHardware;
import de.tucottbus.kt.csl.hardware.AHardware;
import de.tucottbus.kt.csl.hardware.CslHardware;
import de.tucottbus.kt.csl.hardware.HardwareException;
import de.tucottbus.kt.csl.hardware.audio.input.audiodevices.RmeHdspMadi;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.Beamformer3D;
import de.tucottbus.kt.csl.hardware.micarray3d.beamformer.DoAEstimator;
import de.tucottbus.kt.csl.lcars.contributors.ESensitivityPlots;
import de.tucottbus.kt.csl.lcars.contributors.ETrolleySlider;
import de.tucottbus.kt.lcars.IScreen;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.EElementArray;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElbo;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListener;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;
import de.tucottbus.kt.lcars.util.Objectt;

/**
 * The 3D microphone array hardware wrapper. The 3D
 * microphone array consists of the {@linkplain MicArrayCeiling ceiling} and
 * {@linkplain MicArrayViewer main viewer} sub-arrays, and of the {@linkplain 
 * Beamformer3D 3D beamformer virtual device}. This is a top-level device.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: LCARS sub-panel should not directly invoke hardware or MicArrayState instance getters.
 *     </li>
 *   <li>TODO: Make sum level observable.
 *     </li>
 *   <li>TODO: Provide sum audio stream.
 *     </li>
 * </ul>
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
  
  private final MicArrayCeiling micArrayCeiling;
  private final MicArrayViewer  micArrayViewer;
  private final Beamformer3D    beamformer3D;
  private final DoAEstimator    doAEstimator;
  
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
    children.add(beamformer3D);
    children.add(doAEstimator);
    children.add(micArrayViewer);
    children.add(micArrayCeiling);
    return children;
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
  
  /**
   * Sets the microphone illumination mode.
   * 
   * @param mode
   *          The new illumination mode, one of the {@link 
   *          MicArray3D}{@code .ILLUMINATION_xxx} constants. 
   */
  public void setIlluminationMode(int mode)
  {
    micArrayViewer.setIlluminationMode(mode);
    micArrayCeiling.setIlluminationMode(mode);
  }

  /**
   * Returns the current microphone illumination mode.
   * 
   * @return One of the {@link MicArray3D}{@code .ILLUMINATION_xxx} constants.
   */
  public int getIlluminationMode()
  {
    return micArrayViewer.getIlluminationMode();
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
  
  // -- LCARS --
  
  /**
   * The LCARS sub-panel instance.
   */
  protected volatile LcarsSubPanel subPanel;

  @Override
  public ElementContributor getLcarsSubpanel(int x, int y) 
  {
    if (subPanel==null)
      subPanel = new LcarsSubPanel(x, y);
    return subPanel;
  }

  protected class LcarsSubPanel extends ElementContributor
  {
    protected       ESensitivityPlots cSpls;
    protected final ELabel            eSplsInit;
    protected final ETrolleySlider    cTrlySldr;
    protected final EValue            eTrlyPos;
    protected final ERect             eTrlyLock;
    protected final ERect             eLinkWPlots;
    protected final EValue            eSteerX;
    protected final ERect             eSteerXDec;
    protected final ERect             eSteerXInc;
    protected final EValue            eSteerY;
    protected final ERect             eSteerYDec;
    protected final ERect             eSteerYInc;
    protected final EValue            eSteerZ;
    protected final ERect             eSteerZDec;
    protected final ERect             eSteerZInc;
    protected final ERect             eCalibrate;
    protected final EElementArray     cTarget;
    protected final EElementArray     cConfig;
    protected final EElementArray     cIllum;
    protected final ERect             eElaLock;

    protected final LinkedHashMap<String,Point3d>   hTargets;
    protected final LinkedHashMap<String,boolean[]> hConfig;
    protected final LinkedHashMap<String,Integer>   hIllum;
    
    protected MicArrayState lastMas = MicArrayState.getDummy(); 

    public LcarsSubPanel(int x, int y)
    {
      super(x, y);
      
      // Initialize lists
      // - Steering targets
      hTargets = new LinkedHashMap<String,Point3d>();
      hTargets.put("AUTO"   ,null);
      hTargets.put("DEFAULT",CSL.ROOM.DEFAULT_POS);
      hTargets.put("CENTER" ,new Point3d(0,0,170));

      // - Configurations
      LinkedHashMap<String,int[]> hConfigInt= new LinkedHashMap<String,int[]>();
      //                        Mic-ID: 0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63
      hConfigInt.put("ALL OFF", new int[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
      hConfigInt.put("FULL"   , new int[]{ 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1});

      hConfig = new LinkedHashMap<String,boolean[]>();
      for (Entry<String,int[]> entry : hConfigInt.entrySet())
      {
        boolean[] act = new boolean[entry.getValue().length];
        for (int i=0; i<act.length; i++)
          act[i] = entry.getValue()[i]!=0;
        hConfig.put(entry.getKey(),act);
      }

      // - Illumination modes
      hIllum = new LinkedHashMap<String,Integer>();
      hIllum.put("OFF"  ,MicArray3D.ILLUMINATION_NONE);
      hIllum.put("POS"  ,MicArray3D.ILLUMINATION_POS);
      hIllum.put("LEVEL",MicArray3D.ILLUMINATION_LEVEL);
      hIllum.put("DELAY",MicArray3D.ILLUMINATION_DELAY);
      hIllum.put("GAIN" ,MicArray3D.ILLUMINATION_GAIN);
      
      // Sensitivity plots place holder
      int ex = 30;
      int ey = 30;
      eSplsInit = add(new ELabel(null,ex, ey,1140,586,LCARS.ES_STATIC|LCARS.EC_TEXT|LCARS.ES_LABEL_C,"2D SENSITIVITY PLOTS\nINITIALIZING..."));
      
      // Trolley slider group
      int ex0 = 1141;
      cTrlySldr = (ETrolleySlider)add(new ETrolleySlider(ex0,ey,66,532));
      cTrlySldr.setLocked(true);
      cTrlySldr.addSelectionListener(new ETrolleySlider.SelectionListener()
      {
        @Override
        public void selectionChanged(float value)
        {
          try
          {
            MicArray3D.getInstance().setCeilingArrayYPosition(value);
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      });
      ex = ex0-33;
      ey -= 50;
      ERect eRect = new ERect(null,ex,ey,58,22,LCARS.ES_STATIC|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_W|LCARS.EF_SMALL,"POS");
      add(eRect);
      ex += eRect.getBounds().width;
      eTrlyPos = new EValue(null,ex,ey,35,22,LCARS.ES_SELECTED,null);
      eTrlyPos.setValue("N/A");
      eTrlyPos.setValueMargin(0); eTrlyPos.setValueWidth(eTrlyPos.getBounds().width);
      add(eTrlyPos);
      ex += eTrlyPos.getBounds().width +3;
      eRect = new ERect(null,ex,ey,38,22,LCARS.ES_SELECTED|LCARS.EF_SMALL|LCARS.ES_LABEL_W|LCARS.ES_RECT_RND_E,"cm");
      add(eRect);
      EEventListener trlyPosResetListener = new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try 
          {
            EElement el = (EElement)ee.el.getData();
            el.setSelected(!el.isSelected());
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
          if (!cTrlySldr.isLocked())
            try
            {
              cTrlySldr.setValue(0f);
              MicArray3D.getInstance().setCeilingArrayYPosition(0);
            }
            catch (Exception e)
            {
              e.printStackTrace();
            }
        }

        @Override
        public void touchUp(EEvent ee)
        {
          try 
          {
            EElement el = (EElement)ee.el.getData();
            el.setSelected(!el.isSelected());
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      };
      eTrlyPos.setData(eRect); eTrlyPos.addEEventListener(trlyPosResetListener);
      eRect.setData(eTrlyPos); eRect.addEEventListener(trlyPosResetListener);
      
      ex = ex0-33;
      ey = 586;
      EValue eValue = new EValue(null,ex,ey,93,22,LCARS.ES_STATIC|LCARS.ES_RECT_RND_W,null);
      eValue.setValue("TROLLEY");
      eValue.setValueMargin(0);
      add(eValue);
      ex += eValue.getBounds().width;
      EElbo eElbo = new EElbo(null,ex,ey,38,30,LCARS.ES_STATIC|LCARS.ES_SHAPE_NE,null);
      eElbo.setArmWidths(6,22);
      add(eElbo);
      ey += eElbo.getBounds().height;
      eElbo = new EElbo(null,ex,ey,38,56,LCARS.ES_STATIC|LCARS.ES_SHAPE_SE,null);
      eElbo.setArmWidths(6,38);
      add(eElbo);
      ey += eElbo.getBounds().height-38;
      eTrlyLock = new ERect(null,ex0-33,ey,90,38,LCARS.ES_RECT_RND_W|LCARS.ES_SELECTED|LCARS.ES_LABEL_E,"LOCK");
      add(eTrlyLock);
      cTrlySldr.setLockControl(eTrlyLock);
      
      // Steering group
      EEventListener steerIncDecListener = new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try
          {
            MicArray3D ma = MicArray3D.getInstance();
            
            int x = (int)Math.round(ma.getTarget().x);
            int y = (int)Math.round(ma.getTarget().y);
            int z = (int)Math.round(ma.getTarget().z);
            
            if      (ee.el==eSteerXDec) x--;
            else if (ee.el==eSteerXInc) x++;
            else if (ee.el==eSteerYDec) y--;
            else if (ee.el==eSteerYInc) y++;
            else if (ee.el==eSteerZDec) z--;
            else if (ee.el==eSteerZInc) z++;
            
            ma.setTarget(new Point3d(x,y,z));
          } 
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      };
      
      ex0 = -12;
      ex = ex0;
      int eh = 38;
      int ew = 104;
      int dh = 3;

      eValue = add(new EValue(null,ex,ey,eh,4*eh+3*dh,LCARS.ES_STATIC|LCARS.ES_RECT_RND_W|LCARS.ES_DISABLED,null));
      eValue.setValueMargin(0); eValue.setValueWidth(eh/2);
      ex += eh/2+3;
      eValue = add(new EValue(null,ex,ey,171-eh/2-3,eh,LCARS.ES_STATIC|LCARS.ES_SELDISED,null));
      eValue.setValue("STEERING"); eValue.setValueMargin(0);
      ex += eValue.getBounds().width;
      eLinkWPlots = add(new ERect(null,ex,ey,120,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"LINK W/PLOTS"));
      eLinkWPlots.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          boolean selected = !eLinkWPlots.isBlinking();
          eLinkWPlots.setBlinking(selected);
        }
      });
      ex += eLinkWPlots.getBounds().width +3;
      eRect = add(new ERect(null,ex,ey,ew,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"RESET"));
      eRect.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try
          {
            MicArray3D.getInstance().setTarget(CSL.ROOM.DEFAULT_POS);
          } 
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      });
      ex += eRect.getBounds().width +3;
      eSteerX = add(new EValue(null,ex,ey,ew,eh,LCARS.ES_SELDISED|LCARS.ES_LABEL_W,"X/cm"));
      eSteerX.setValue("N/A"); eSteerX.setValueMargin(0); eSteerX.setValueWidth(55);
      ex += eSteerX.getBounds().width;
      eSteerXDec = add(new ERect(null,ex,ey,eh,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C,"\u2013"));
      eSteerXDec.addEEventListener(steerIncDecListener);
      ex += eSteerXDec.getBounds().width +3;
      eSteerXInc = add(new ERect(null,ex,ey,eh,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C,"+"));
      eSteerXInc.addEEventListener(steerIncDecListener);
      ex += eSteerXInc.getBounds().width +3;
      eSteerY = add(new EValue(null,ex,ey,ew,eh,LCARS.ES_SELDISED|LCARS.ES_LABEL_W,"Y/cm"));
      eSteerY.setValue("N/A"); eSteerY.setValueMargin(0); eSteerY.setValueWidth(53);
      ex += eSteerY.getBounds().width;
      eSteerYDec = add(new ERect(null,ex,ey,eh,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C,"\u2013"));
      eSteerYDec.addEEventListener(steerIncDecListener);
      ex += eSteerYDec.getBounds().width +3;
      eSteerYInc = add(new ERect(null,ex,ey,eh,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C,"+"));
      eSteerYInc.addEEventListener(steerIncDecListener);
      ex += eSteerYInc.getBounds().width +3;
      eSteerZ = add(new EValue(null,ex,ey,ew,eh,LCARS.ES_SELDISED|LCARS.ES_LABEL_W,"Z/cm"));
      eSteerZ.setValue("N/A"); eSteerZ.setValueMargin(0); eSteerZ.setValueWidth(53);
      ex += eSteerZ.getBounds().width;
      eSteerZDec = add(new ERect(null,ex,ey,eh,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C,"\u2013"));
      ex += eSteerZDec.getBounds().width +3;
      eSteerZDec.addEEventListener(steerIncDecListener);
      eSteerZInc = add(new ERect(null,ex,ey,eh,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_C,"+"));
      eSteerZInc.addEEventListener(steerIncDecListener);
      ex += eSteerZInc.getBounds().width +3;
      eCalibrate = add(new ERect(null,ex,ey,158,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E|LCARS.ES_RECT_RND_E,"CALIBRATE"));
      eCalibrate.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try
          {
            MicArray3D.getInstance().calibrate();
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      });
      ex += eCalibrate.getBounds().width +3;
      
      // Target group
      ex = ex0 + eh/2 +3;
      ey += eh+dh;
      ERect ePrev = add(new ERect(null,ex,ey,68,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"PREV"));
      ex += ePrev.getBounds().width +3;
      eValue = add(new EValue(null,ex,ey,78,eh,LCARS.ES_SELDISED,null));
      eValue.setValueMargin(0); eValue.setValueWidth(eValue.getBounds().width);
      eValue.setValue("TARGET");
      ex += eValue.getBounds().width;
      ew = 125;
      cTarget = new EElementArray(this.x+ex,this.y+ey,ERect.class,new Dimension(ew,eh),1,7,LCARS.EC_PRIMARY,null);
      for (Entry<String, Point3d> entry : hTargets.entrySet())
        cTarget.add(entry.getKey()).setData(entry.getValue());
      while (cTarget.getItemCount()%7!=0)
      {
        EElement el = cTarget.add("");
        el.setStatic(true); el.setDisabled(true);
        el.setAlpha(0.3f);
      }
      cTarget.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try
          {
            MicArray3D.getInstance().setTarget((Point3d)ee.el.getData());
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      });
      cTarget.setLock(true);
      add(cTarget,false);
      ERect eNext = add(new ERect(null,ex+7*(ew+3),ey,71,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"NEXT"));
      eElaLock = add(new ERect(null,1129,ey,69,3*eh+2*dh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_NE,"LOCK"));
      eElaLock.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          boolean lock = !cTarget.getLock();
          cTarget.setLock(lock);
          cConfig.setLock(lock);
          cIllum.setLock(lock);
        }
      });
      cTarget.setPageControls(ePrev, eNext);
      eRect = add(new ERect(null,1201,ey,38,3*eh+2*dh,LCARS.ES_STATIC|LCARS.ES_RECT_RND_E|LCARS.ES_DISABLED,null));
      
      // Config. group
      ex = ex0 + eh/2 +3;
      ey += eh+dh;
      ePrev = add(new ERect(null,ex,ey,68,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"PREV"));
      ex += ePrev.getBounds().width +3;
      eValue = add(new EValue(null,ex,ey,78,eh,LCARS.ES_SELDISED,null));
      eValue.setValueMargin(0); eValue.setValueWidth(eValue.getBounds().width);
      eValue.setValue("CONFIG");
      ex += eValue.getBounds().width;
      ew = 125;
      cConfig = new EElementArray(this.x+ex,this.y+ey,ERect.class,new Dimension(ew,eh),1,7,LCARS.EC_PRIMARY,null);
      for (Entry<String,boolean[]> entry : hConfig.entrySet())
        cConfig.add(entry.getKey()).setData(entry.getValue());
      while (cConfig.getItemCount()%7!=0)
      {
        EElement el = cConfig.add("");
        el.setStatic(true); el.setDisabled(true);
        el.setAlpha(0.3f);
      }
      cConfig.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try
          {
            MicArray3D ma = MicArray3D.getInstance(); 
            boolean[] act = (boolean[])ee.el.getData();
            for (int i=0; i<act.length; i++)
              ma.setMicActive(i,act[i]);
          } 
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      });
      cConfig.setLock(true);
      add(cConfig,false);
      eNext = add(new ERect(null,ex+7*(ew+3),ey,71,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"NEXT"));
      cConfig.setPageControls(ePrev, eNext);
      
      // Illum. group
      ex = ex0 + eh/2 +3;
      ey += eh+dh;
      ePrev = add(new ERect(null,ex,ey,68,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"PREV"));
      ex += ePrev.getBounds().width +3;
      eValue = add(new EValue(null,ex,ey,78,eh,LCARS.ES_SELDISED,null));
      eValue.setValueMargin(0); eValue.setValueWidth(eValue.getBounds().width);
      eValue.setValue("ILLUM");
      ex += eValue.getBounds().width;
      ew = 125;
      cIllum = new EElementArray(this.x+ex,this.y+ey,ERect.class,new Dimension(ew,eh),1,7,LCARS.EC_PRIMARY,null);
      for (Entry<String,Integer> entry : hIllum.entrySet())
        cIllum.add(entry.getKey()).setData(new Integer(entry.getValue()));
      while (cIllum.getItemCount()%7!=0)
      {
        EElement el = cIllum.add("");
        el.setStatic(true); el.setDisabled(true);
        el.setAlpha(0.3f);
      }
      cIllum.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          try
          {
            MicArray3D.getInstance().setIlluminationMode((Integer)ee.el.getData());
          }
          catch (Exception e)
          {
            e.printStackTrace();
          }
        }
      });
      cIllum.setLock(true);
      add(cIllum,false);
      eNext = add(new ERect(null,ex+7*(ew+3),ey,71,eh,LCARS.EC_PRIMARY|LCARS.ES_LABEL_E,"NEXT"));
      cIllum.setPageControls(ePrev, eNext);

      // Fat initialization
      LCARS.invokeLater(()->
      {
        // Sensitivity plots
        cSpls = new ESensitivityPlots(30,30);
        cSpls.addSelectionListener(new ESensitivityPlots.SelectionListener()
        {
          @Override
          public void slicePositionsChanged(Point3d point)
          {
            if (eLinkWPlots.isBlinking())
              try
              {
                MicArray3D.getInstance().setTarget(point);
              } 
              catch (Exception e)
              {
                e.printStackTrace();
              }
          }
        });
        eSplsInit.setVisible(false);
        add(cSpls);
      },1500);

    }

    @Override
    protected void fps10()
    {
      // HACK: Polling for GUI update is not elegant (but safe)
      MicArrayState mas = MicArrayState.getCurrent();
      MicArray3D ma = MicArray3D.getInstance();
      
      // Feed sensitivity plots and trolley slider
      try
      {
        if (cSpls!=null)
        {
          cSpls.setMicArrayState(mas);
          if (eLinkWPlots.isBlinking())
            cSpls.setSelection(mas.target);
        }
        cTrlySldr.setActualValue(Math.round(mas.trolleyPos));
        eTrlyPos.setValue(String.format("% 04.0f",(float)Math.round(mas.trolleyPos)));
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      
      // Feed steering group
      try
      {
        eSteerX.setValue(String.format("% 04.0f",(float)Math.round(mas.target.x)));
        eSteerY.setValue(String.format("% 04.0f",(float)Math.round(mas.target.y)));
        eSteerZ.setValue(String.format("% 04.0f",(float)Math.round(mas.target.z)));
        eSteerXDec.setDisabled(mas.target.x<=CSL.ROOM.MIN_X);
        eSteerXInc.setDisabled(mas.target.x>=CSL.ROOM.MAX_X);
        eSteerYDec.setDisabled(mas.target.y<=CSL.ROOM.MIN_Y);
        eSteerYInc.setDisabled(mas.target.y>=CSL.ROOM.MAX_Y);
        eSteerZDec.setDisabled(mas.target.z<=CSL.ROOM.MIN_Z);
        eSteerZInc.setDisabled(mas.target.z>=CSL.ROOM.MAX_Z);
        eCalibrate.setColor(!ma.isCalibrated()?LCARS.getColor(LCARS.CS_REDALERT,LCARS.EC_HEADLINE):null);
        eCalibrate.setBlinking(!ma.isCalibrated());
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      
      // Feed target array
      for (EElement el : cTarget.getItemElements())
        try
        {
          if ("".equals(el.getLabel()))
            continue;
          boolean equal = Objectt.equals((Point3d)el.getData(),mas.target);
          el.setColorStyle(equal?LCARS.EC_SECONDARY:LCARS.EC_PRIMARY);
          el.setSelected(equal);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      
      // Feed config. array
      for (EElement el : cConfig.getItemElements())
        try
        {
          if ("".equals(el.getLabel()))
            continue;
          boolean equal = Arrays.equals((boolean[])el.getData(),mas.activeMics);
          el.setColorStyle(equal?LCARS.EC_SECONDARY:LCARS.EC_PRIMARY);
          el.setSelected(equal);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      
      // Feed illum. array
      for (EElement el : cIllum.getItemElements())
        try
        {
          if ("".equals(el.getLabel()))
            continue;
          boolean equal = ((Integer)el.getData()==ma.getIlluminationMode());
          el.setColorStyle(equal?LCARS.EC_SECONDARY:LCARS.EC_PRIMARY);
          el.setSelected(equal);
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      
      // Other GUI update
      eElaLock.setBlinking(cTarget.getLock());
      
      // TODO: Updating illumination should not be done here
      try
      {
        if (!mas.equals(lastMas))
        {
          lastMas = mas;
          micArrayViewer.illuminate(ma.getIlluminationMode());
          micArrayCeiling.illuminate(ma.getIlluminationMode());
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }
  
  public static class MicArrayPanel extends HardwareAccessPanel
  {
    public MicArrayPanel(IScreen iscreen) 
    {
      super(iscreen, MicArray3D.class);
    }

    @Override
    protected void createSubPanels() 
    {
      MicArray3D.getInstance().getLcarsSubpanel(677,140).addToPanel(this);
    }
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
