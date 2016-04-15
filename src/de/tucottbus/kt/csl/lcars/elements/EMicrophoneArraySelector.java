package de.tucottbus.kt.csl.lcars.elements;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import java.util.TimerTask;

import de.tucottbus.kt.csl.hardware.micarray3d.AMicArray3DPart;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayState;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.contributors.EElementArray;
import de.tucottbus.kt.lcars.contributors.ETopography;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.elements.EValue;

/**
 * Instances of this class provide the topographical GUI of a microphone array.
 * 
 * @author Matthias Wolff
 */
public class EMicrophoneArraySelector extends ETopography
{
  private AMicArray3DPart micArray;
  private int             selectedMic = -1;
  private EElementArray   eSelector;
  private EElement        eLock;
  private boolean         lock;

  // -- Constructors --
  
  /**
   * Creates a microphone array GUI.
   * 
   * @param micArray
   *          The microphone array.
   * @param sBounds
   *          The bounds of the GUI object (in LCARS screen pixels).
   * @param pBounds
   *          The physical bounds of the microphone array (in centimeters).
   */
  public EMicrophoneArraySelector
  (
    AMicArray3DPart micArray,
    MicArrayState     state,
    Rectangle         sBounds,
    Rectangle2D.Float pBounds
  )
  {
    super(sBounds.x,sBounds.y,sBounds.width,sBounds.height,LCARS.EC_HEADLINE);
    this.micArray = micArray;
    setPhysicalBounds(pBounds,"cm",false);    
    //setMapStyle(LCARS.EC_SECONDARY,false); // This is slow!
    setGrid(new Point2D.Float(50,50),null,false);
    setGridStyle(LCARS.EC_SECONDARY|LCARS.EF_TINY,0.2f,0,false);
    
    for (int micId = micArray.getMinMicId(); micId<=micArray.getMaxMicId(); micId++)
    {
      float x, y;
      if (micArray instanceof MicArrayCeiling)
      {
        x = (float)micArray.getMicPosition(micId).x;
        y = (float)micArray.getMicPosition(micId).y;
      }
      else
      {
        x = (float)micArray.getMicPosition(micId).x;
        y = (float)micArray.getMicPosition(micId).z;
      }
      addPoint(new Point2D.Float(x,y),5,LCARS.EC_ELBOUP,new Integer(micId),false);
    }
    addEEventListener(new EEventListenerAdapter()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        if (ee.el.getData()!=null && ee.el.getData() instanceof Integer)
        {
          int micId = (Integer)ee.el.getData();
          setSelection(micId);
          fireMicSelected(micId);
        }
      }
    });
    
  }

  // -- Getters and setters --
  
  /**
   * Sets the microphone selector control.
   * 
   * @param eSelector
   *          The selector control, can be <code>null</code>.
   */
  public void setSelectorControl(EElementArray eSelector)
  {
    this.eSelector = eSelector;
  }

  /**
   * Attaches a new lock control to this array. This controls can be used to toggle the animation on
   * or off. The array will register itself as listener to this controls and modify its state (e.g.
   * blinking).
   * 
   * @param eLock
   *          The "lock" control, can be <code>null</code>.
   */
  public void setLockControl(EElement eLock)
  {
    this.eLock = eLock;
  }

  /**
   * Toggles the animation on or off.
   * 
   * @param lock
   *          <true>true</code> to switch automatic page turning off,
              <code>false</code> to switch it on
   */
  public void setLock(boolean lock)
  {
    this.lock = lock;
    if (eLock!=null) eLock.setBlinking(lock);
  }

  /**
   * Determines if the animation is on or off.
   * 
   * @return <true>true</code> if off, <code>false</code> if on
   */
  public boolean getLock()
  {
    if (eLock!=null) return eLock.isBlinking();
    return this.lock;
  }
  
  /**
   * Selects a microphone in an array. 
   * 
   * @param mic
   *          The microphone to select, -1 for no selection.
   * @see #getSelection()
   */
  public void setSelection(int micId)
  {
    // Find topography point for microphone
    ERect point = null;
    for (ERect e : getPoints())
      if (((Integer)e.getData())==micId)
      {
        point = e;
        break;
      }
    
    if (point==null) micId = -1;
    selectedMic = micId;
    if (micId<0)
    {
      removeCursor(true);
      return;
    }
    else
    {
      Point2D pos = getPointPos(point);
      String s = makeCursorLabel(micId);
      if (!hasCursor())
      {
        setCursor(40,2,LCARS.EF_TINY,true);
        setCursorPos((float)pos.getX(),(float)pos.getY(),s);
      }
      else
        slideCursor((float)pos.getX(),(float)pos.getY(),200,s);
    }    
  }

  /**
   * Returns the currently selected microphone or <code>null</code> if no microphone is selected.
   * 
   * @see #setSelection(IMicrophone)
   */
  public int getSelection()
  {
    return selectedMic;
  }
  
  /**
   * Returns the microphone array associated with this GUI.
   */
  public AMicArray3DPart getMicrophoneArray()
  {
    return micArray;
  }
  
  // -- Operations --
  
  /**
   * Updates the label of the cursor according to the currently selected microphone.
   * 
   * @see #getSelection()
   */
  public void updateCursor()
  {
    setCursorLabel(makeCursorLabel(getSelection()));
  }

  /**
   * Creates the cursor label for a microphone.
   * 
   * @param mic
   *          The microphone.
   */
  private String makeCursorLabel(int micId)
  {
    if (micId<micArray.getMinMicId()) return "";
    if (micId>micArray.getMaxMicId()) return "";
    MicArrayState mas = micArray.getState();
    String s = String.format("%02d",micId)+"\n";
    if (!mas.activeMics[micId])
      s += "OFF";
    else
    {
      s += makeSteerDelay(mas.steerVec[micId],true)+"\n";
      s += makeGainDB(mas.gains[micId],true);
    }
    return s;
  }
  
  // -- Listeners handling --
  
  /**
   * Notifies listeners that a microphone has been selected.
   * 
   * @param mic
   *          The selected microphone.
   */
  private void fireMicSelected(int micId)
  {
    // TODO: implement EMicrophoneArray.fireMicSelected
  }

  // -- Overrides --
  
  @Override
  public void addToPanel(Panel panel)
  {
    super.addToPanel(panel);

    // Fill microphone selector
    if (eSelector!=null)
    {
      eSelector.removeAll();
      for (int micId=micArray.getMinMicId(); micId<=micArray.getMaxMicId(); micId++)
      {
        eSelector.add("OFF");
        EValue e = (EValue)eSelector.getItemElement(eSelector.getItemCount()-1);
        e.setLabel(String.format("%02d",micId));
        e.setData(new Integer(micId));
        e.setValueWidth(80);
      }
    }

    // Attach lock control
    if (eLock!=null)
    {
      eLock.removeAllEEventListeners();
      eLock.addEEventListener(new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          lock = !ee.el.isBlinking();
          ee.el.setBlinking(lock);
        }
      });
      eLock.setBlinking(lock);
    }
    
    // Schedule animation timer task, canceled automatically by removeFromPanel()
    scheduleTimerTask(new TimerTask()
    {
      @Override
      public void run()
      {
        if (lock) return;

        int newSel = selectedMic+1;
        if (newSel<micArray.getMinMicId())
          newSel = micArray.getMinMicId();
        if (newSel>micArray.getMaxMicId())
          newSel = micArray.getMinMicId();
        setSelection(newSel);
      }
    },"Animation",2000,2000);
  }

  @Override
  public void removeFromPanel()
  {
    cancelAllTimerTasks();
    if (eSelector!=null)
    {
      eSelector.removeAll();
    }
    if (eLock!=null)
    {
      eLock.setBlinking(false);
      eLock.removeAllEEventListeners();
    }
    super.removeFromPanel();
  }

  // -- Static methods --
  
  /**
   * Returns a human readable representation of a level value.
   * 
   * @param level
   *          The level (in dB).
   * @param unit
   *          If <code>true</code> include unit "dB".
   */
  public static String makeLevel(float level, boolean unit)
  {
    // TODO: Correct value of silence?
    if (level<-73) return "SIL";
    String s = String.format(Locale.ENGLISH,"%03.0f",level);
    if (unit) s += " dB";
    return s;
  }
  
  /**
   * Returns a human readable representation of a steering delay.
   * 
   * @param delay
   *          The steering delay (in s).
   * @param unit
   *          If <code>true</code> include unit "us".
   */
  public static String makeSteerDelay(float delay, boolean unit)
  {
    String s = String.format(Locale.ENGLISH,"%03.0f",delay*1E6);
    if (unit) s += " us";
    return s;
  }

  /**
   * Returns a human readable representation of a gain value. The return value will be converted
   * into decibels.
   * 
   * @param gain
   *          The gain value (0...1).
   * @param unit
   *          If <code>true</code> include unit "dB".
   */
  public static String makeGainDB(float gain, boolean unit)
  {
    float level = (float) (Math.max(-75,Math.min(0,((10*Math.log(gain))+10))));
    return makeLevel(level,unit);
  }
  
}
