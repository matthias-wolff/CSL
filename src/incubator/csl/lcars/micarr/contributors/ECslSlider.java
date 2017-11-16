package incubator.csl.lcars.micarr.contributors;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;

import de.tucottbus.kt.csl.hardware.led.ALedController;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListener;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;

/**
 * A stylish lightweight slider.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Use {@link ECslSlider} in {@link ALedController.LcarsSubPanel}</li>
 * </ul>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class ECslSlider extends ElementContributor
{
  // -- Constants --
  
  /**
   * Style constant for a vertical slider.
   */
  public static final int ES_VERTICAL = 0x00000000;
  
  /**
   * Style constant for a horizontal slider.
   */
  public static final int ES_HORIZONTAL = 0x10000000;
  
  /**
   * Style constant for a linear scale.
   */
  public static final int ES_LINEAR = 0x00000000;
  
  /**
   * Style constant for a logarithmic scale.
   */
  public static final int ES_LOGARITHMIC = 0x20000000;

  /**
   * If set, the slider knob will snap to the scale ticks, i.e. only values
   * corresponding to scale ticks can be selected.
   * 
   * @see #snapToTicks
   * @see #add(ScaleTick)
   */
  public static final int EB_SNAPTOTICKS = 0x40000000;

  protected static final int SNAPBYHOLD_TIMEOUT = 1000;
  
  // -- Public fields --
  
  /**
   * If <code>true</code> the slider is horizontal, if <code>false</code> it is
   * vertical.
   */
  public final boolean horiz;

  /**
   * If <code>true</code> the slider is logarithmic, if <code>false</code> it is
   * linear.
   */
  public final boolean log;
  
  /**
   * The slider background {@link EElement}.
   */
  public final ERect eBack;
  
  /**
   * The touch-sensitive area of the slider.
   * <ul>
   *   <li>You can modify the area by invoking
   *   {@link EElement#addGeometryModifier(de.tucottbus.kt.lcars.elements.modify.EGeometryModifier)
   *   eSens.addGeometryModifier(EGeometryModifier)}.</li>
   *   <li>You can make the area visible by invoking
   *   {@link EElement#setAlpha(float) eSens.setAlpha(0.2f)}</li>
   * </ul>
   */
  public final ERect eSens;
  
  /**
   * The slider knob.
   */
  public final ERect eKnob;

  /**
   * If <code>true</code> the slider knob will snap to scale ticks, i.e. only
   * values corresponding to scale ticks can be selected.
   * 
   * @see #EB_SNAPTOTICKS
   * @see #add(ScaleTick)
   */
  public boolean snapToTicks;
  
  // -- Protected fields --
  
  protected final   ArrayList<ScaleTick> lScaleTicks;
  protected int     style;
  protected float   min;
  protected float   max;
  protected int     dragOffset;
  protected int     dragLastPos;
  protected boolean snapByHold;
  protected long    snapByHoldMillis;
  
  // -- Life cycle --
  
  /**
   * Creates a new slider.
   * 
   * @param x
   *          The x-coordinate of the upper left corner (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the upper left corner (in LCARS panel pixels).
   * @param w
   *          The width (in LCARS panel pixels).
   * @param h
   *          The height (in LCARS panel pixels).
   * @param style
   *          A combination of color style ({@link LCARS}<code>.ES_XXX</code>),
   *          {@link ECslSlider}<code>.ES_XXX</code> and
   *          {@link ECslSlider}<code>.EB_XXX</code> constants.
   * @param fatFingerMargin
   *          Margin of touch-sensitive area around the bounding rectangle (in
   *          LCARS panel pixels).
   */
  public ECslSlider(int x, int y, int w, int h, int style, int fatFingerMargin)
  {
    super(x, y);

    this.horiz = (style & ES_HORIZONTAL)!=0;
    this.log = (style & ES_LOGARITHMIC)!=0;
    this.snapToTicks = (style & EB_SNAPTOTICKS)!=0;
    this.style = style & LCARS.ES_STYLE;
    int ffm = Math.max(0,fatFingerMargin);
    lScaleTicks = new ArrayList<ECslSlider.ScaleTick>();

    // Drag listener
    EEventListener dragListener = new EEventListener()
    {
      @Override
      public void touchDown(EEvent ee)
      {
        snapByHoldMillis = System.currentTimeMillis();
        eBack.setSelected(!eBack.isSelected());
        
        dragOffset = 0;
        dragLastPos = Integer.MIN_VALUE;
        if (ee.el==eKnob)
          if (horiz)
            dragOffset = ee.pt.x - eKnob.getBounds().width/2;
          else
            dragOffset = ee.pt.y - eKnob.getBounds().height/2;
        int pos;
        if (horiz)
          pos = ee.el.getBounds().x-eBack.getBounds().x+ee.pt.x;
        else
          pos = ee.el.getBounds().y-eBack.getBounds().y+ee.pt.y;
        pos -= dragOffset;
        if (snapToTicks)
        {
          ScaleTick tick = getClosestScaleTick(pos); 
          if(setKnobPos(tick.getPos()))
            fireSelectionChanged(tick.getValue());
        }
        else
        {
          if (setKnobPos(pos))
            fireSelectionChanged(getValue());
        }
      }

      @Override
      public void touchUp(EEvent ee)
      {
        int pos;
        eBack.setSelected(!eBack.isSelected());
        if (snapByHold)
          return;
        if (horiz)
          pos = ee.el.getBounds().x-eBack.getBounds().x+ee.pt.x;
        else
          pos = ee.el.getBounds().y-eBack.getBounds().y+ee.pt.y;
        pos -= dragOffset;
        if (snapToTicks)
        {
          ScaleTick tick = getClosestScaleTick(pos); 
          if(setKnobPos(tick.getPos()))
            fireSelectionChanged(tick.getValue());
        }
        else
        {
          if (setKnobPos(pos))
            fireSelectionChanged(getValue());
        }
      }
      
      @Override
      public void touchDrag(EEvent ee)
      {
        int pos;
        if (horiz)
          pos = ee.el.getBounds().x-eBack.getBounds().x+ee.pt.x;
        else
          pos = ee.el.getBounds().y-eBack.getBounds().y+ee.pt.y;
        pos -= dragOffset;
        if (pos==dragLastPos)
          return;

        snapByHoldMillis = System.currentTimeMillis();
        dragLastPos=pos;
        if (snapToTicks)
        {
          ScaleTick tick = getClosestScaleTick(pos); 
          if(setKnobPos(tick.getPos()))
            fireSelectionChanged(tick.getValue());
        }
        else
        {
          if (setKnobPos(pos))
            fireSelectionChanged(getValue());
        }
      }

      @Override
      public void touchHold(EEvent ee)
      {
        if (System.currentTimeMillis()-snapByHoldMillis<SNAPBYHOLD_TIMEOUT)
          return;
        
        ScaleTick tick = getClosestScaleTick(getKnobPos()); 
        if (setKnobPos(tick.getPos()))
        {
          eBack.setHighlighted(true);
          LCARS.invokeLater(()->
          {
            eBack.setHighlighted(false);
          },200);
          snapByHold = true;
          fireSelectionChanged(tick.getValue());
        }
      }
    }; 

    if (horiz)
    {
      eBack = new ERect(null,0,h/8,w,h*3/4,LCARS.ES_NONE,null);
      eKnob = new ERect(null,w/2-h/4,0,h/2,h,style|LCARS.ES_RECT_RND|LCARS.EB_OVERDRAG|LCARS.EF_SMALL|LCARS.ES_LABEL_C,null);
    }
    else
    {
      eBack = new ERect(null,w/8,0,w*3/4,h,LCARS.ES_NONE,null);
      eKnob = new ERect(null,0,h/2-w/4,w,w/2,style|LCARS.ES_RECT_RND|LCARS.EB_OVERDRAG|LCARS.EF_SMALL|LCARS.ES_LABEL_C,null);
    }
    eBack.setAlpha(0.2f);
    eSens = new ERect(null,-ffm,-ffm,w+2*ffm,h+2*ffm,LCARS.EB_OVERDRAG,null);
    eSens.setColor(LCARS.getColor(LCARS.CS_REDALERT,LCARS.EC_HEADLINE));
    eSens.setAlpha(0.f);
    eSens.addEEventListener(dragListener);
    eKnob.addEEventListener(dragListener);
    add(eBack);
    add(eSens);
    add(new ScaleTick(0,null,LCARS.ES_NONE,true,true));
    add(new ScaleTick(this.horiz?w:h,null,LCARS.ES_NONE,true,true));
    add(eKnob);
    
    setMinMaxValue(0,1);
  }

  // -- Public API --

  /**
   * Adds a scale tick.
   * 
   * @param pos
   *          The position relative to the top (vertical sliders) or left side
   *          (horizontal sliders), in LCARS panel pixels.
   * @param label
   *          The tick label, can be <code>null</code>.
   * @param fontStyle
   *          One of the {@link LCARS}<code>.EF_XXX</code> constants.
   * @return The scale tick.
   */
  public ScaleTick addScaleTick(int pos, String label, int fontStyle)
  {
    return add(new ScaleTick(pos,label,fontStyle,false,true));
  }

  /**
   * Adds a scale tick.
   * 
   * @param value
   *          The value if the tick.
   * @param label
   *          The tick label, can be <code>null</code>.
   * @param fontStyle
   *          One of the {@link LCARS}<code>.EF_XXX</code> constants.
   * @return The scale tick.
   */
  public ScaleTick addScaleTick(float value, String label, int fontStyle)
  {
    ScaleTick tick = new ScaleTick(valueToPos(value),label,fontStyle,false,true);
    tick.value = value;
    return add(tick);
  }
  
  /**
   * Adds a scale label.
   * 
   * @param pos
   *          The position relative to the top (vertical sliders) or left side
   *          (horizontal sliders), in LCARS panel pixels.
   * @param label
   *          The label.
   * @param fontStyle
   *          One of the {@link LCARS}<code>.EF_XXX</code> constants.
   * @return A scale tick object representing the label.
   */
  public ScaleTick addScaleLabel(int pos, String label, int fontStyle)
  {
    return add(new ScaleTick(pos,label,fontStyle,false,false));
  }
  
  /**
   * Returns the list of scale ticks and scale labels. The returned object is a
   * copy, modifying it has no effect on the slider. You can, however, modify the
   * {@link EElement}s in the scale ticks.
   * 
   * @see #addScaleTick(int, String, int)
   * @see #addScaleTick(float, String, int)
   * @see #addScaleLabel(int, String, int)
   */
  public Collection<ScaleTick> getScaleTicks()
  {
    return new ArrayList<ScaleTick>(lScaleTicks);
  }
  
  /**
   * Returns the scale tick closest to a knob position.
   * 
   * @param pos
   *          The knob position relative to the top (vertical sliders) or left
   *          side (horizontal sliders), in LCARS panel pixels.
   * @param The
   *          closest scale tick.
   */
  public ScaleTick getClosestScaleTick(int pos)
  {
    ScaleTick closest = null;
    int minDist = Integer.MAX_VALUE;
    for (ScaleTick tick : lScaleTicks)
      if (Math.abs(pos-tick.getPos())<minDist)
      {
        minDist = Math.abs(pos-tick.getPos());
        closest = tick;
      }
    return closest;
  }
  
  /**
   * Returns the scale tick closest to a value.
   * 
   * @param pos
   *          The value.
   * @param The
   *          closest scale tick.
   */
  public ScaleTick getNearestScaleTick(float value)
  {
    return getClosestScaleTick(valueToPos(value));
  }
  
  /**
   * Sets the minimum and maximum values for the slider. The default interval is [0,1].
   * 
   * @param min
   *          The minimum.
   * @param max
   *          The maximum.
   */
  public void setMinMaxValue(float min, float max)
  {
    this.min = logValue(min);
    this.max = logValue(max);
  }
  
  /**
   * Sets the slider knob to a value.
   * 
   * @param value
   *          The value.
   */
  public void setValue(float value)
  {
    setKnobPos(valueToPos(value));
  }
  
  /**
   * Returns the current value of the slider, i.e. the value represented by the
   * current knob position.
   */
  public float getValue()
  {
    return posToValue(getKnobPos());
  }

  /**
   * Converts a value to a slider position.
   * 
   * @param value
   *          The value.
   * @return The position relative to the top (vertical sliders) or left side
   *         (horizontal sliders), in LCARS panel pixels.
   */
  public int valueToPos(float value)
  {
    value = logValue(value);
    Rectangle b = eBack.getBounds();
    if (horiz)
    {
      int pos = Math.round((value-min)/(max-min)*b.width);
      return Math.max(0,Math.min(b.width,pos));
    }
    else
    {
      int pos =  b.height-Math.round((value-min)/(max-min)*b.height);
      return Math.max(0,Math.min(b.height,pos));
    }
  }
  
  /**
   * Converts a slider position to a value.
   * 
   * @param pos
   *         The position relative to the top (vertical sliders) or left side
   *         (horizontal sliders), in LCARS panel pixels.
   * @return The value.
   */
  public float posToValue(int pos)
  {
    Rectangle b = eBack.getBounds();
    if (horiz)
    {
      pos = Math.max(0,Math.min(b.height,pos));
      return expValue((float)pos/(float)b.width*(max-min)+min);
    }
    else
    {
      pos = Math.max(0,Math.min(b.height,pos));
      return expValue((float)(b.height-pos)/(float)b.height*(max-min)+min);
    }
  }
  
  // -- Protected API --
  
  /**
   * @return <code>true</code> if the knob position was changed,
   *         <code>false</code> otherwise.
   */
  protected boolean setKnobPos(int pos)
  {
    if (pos==getKnobPos())
      return false;
    Rectangle b = eKnob.getBounds();
    if (horiz)
    { 
      pos = Math.max(0,Math.min(eBack.getBounds().width,pos));
      b.x = eBack.getBounds().x+pos-b.width/2;
    }
    else
    {
      pos = Math.max(0,Math.min(eBack.getBounds().height,pos));
      b.y = eBack.getBounds().y+pos-b.height/2;
    }
    eKnob.setBounds(b);
    return true;
  }
  
  protected int getKnobPos()
  {
    Rectangle b = eKnob.getBounds();
    if (horiz)
      return b.x + b.width/2 - eBack.getBounds().x;
    else
      return b.y + b.height/2 - eBack.getBounds().y;
  }
  
  protected float logValue(float value)
  {
    if (!log) 
      return value;
    return (float)Math.log(Math.max(Float.MIN_NORMAL,value));
  }
  
  protected float expValue(float value)
  {
    if (!log) 
      return value;
    return (float)Math.exp(value);
  }
  
  protected ScaleTick add(ScaleTick scaleTick)
  {
    remove(eSens);
    remove(eKnob);
    
    lScaleTicks.add(scaleTick);
    add(scaleTick.eLine,false);
    if (scaleTick.eLabel!=null)
      add(scaleTick.eLabel,false);

    add(eSens,false);
    add(eKnob,false);
    return scaleTick;
  }

  // -- Selection listener implementation --
  
  public interface SelectionListener
  {
    public void selectionChanged(float value);
  }

  private ArrayList<SelectionListener> listeners;
  
  /**
   * Adds a selection listener to the slider.
   * 
   * @param listener
   *          The listener.
   */
  public void addSelectionListener(SelectionListener listener)
  {
    if (listener==null)
      return;
    if (listeners==null)
      listeners = new ArrayList<SelectionListener>();
    if (listeners.contains(listener))
      return;
    listeners.add(listener);
  }
  
  /**
   * Removes a selection listener from the slider.
   * 
   * @param listener
   *          The listener. If this listener is not registered, the method does
   *          nothing.
   */
  public void removeListener(SelectionListener listener)
  {
    if (listener==null || listeners==null)
      return;
    listeners.remove(listener);
  }
  
  /**
   * Removes all selection listeners from the slider.
   */
  public void removeAllListeners()
  {
    listeners.clear();
  }
  
  protected void fireSelectionChanged(float value)
  {
    if (listeners==null)
      return;
    for (SelectionListener listener : listeners)
      listener.selectionChanged(value);
  }

  // -- Nested classes --
  
  /**
   * A scale tick on the slider.
   */
  public class ScaleTick
  {
    /**
     * The tick line, can be <code>null</code>. 
     */
    public final ERect eLine;

    /**
     * The tick line, can be <code>null</code>. 
     */
    public final ELabel eLabel;
    
    /**
     * If <code>true</code>, the slider will snap to this scale tick.
     * 
     * <h3>Remarks:</h3>
     * <ul>
     *   <li>Tick snapping must be activated by the
     *   {@link ECslSlider#EB_SNAPTOTICKS} style or by setting
     *   {@link ECslSlider#snapToTicks}!</li>
     * </ul>
     */
    public boolean snapToTick;
    
    /**
     * The value of the tick.
     */
    protected float value;
    
    protected ScaleTick(int pos, String label, int fontStyle, boolean noLine, boolean snapToTick)
    {
      this.snapToTick = snapToTick;
      this.value = Float.NaN;

      fontStyle = (fontStyle & LCARS.ES_FONT) | style | LCARS.ES_STATIC;
      Rectangle b = eBack.getBounds();

      if (horiz)
      {
        this.eLine = new ERect(null,b.x+pos,b.y,1,b.height,style|LCARS.ES_STATIC,null);
        if (label!=null && label.length()>0)
          this.eLabel = new ELabel(null,b.x+pos-4,b.y,1,b.height,fontStyle|LCARS.ES_LABEL_E|fontStyle,label);
        else
          this.eLabel = null;
      }
      else
      {
        eLine = new ERect(null,b.x,b.y+pos,b.width,1,style|LCARS.ES_STATIC,null);
        if (label!=null && label.length()>0)
          this.eLabel = new ELabel(null,b.x,b.y+pos-1,b.width-3,1,fontStyle|LCARS.ES_LABEL_SE,label);
        else
          eLabel = null;
      }
      this.eLine.setAlpha(noLine?0.f:0.3f);
      if (this.eLabel!=null)
        this.eLabel.setAlpha(0.5f);
    }
    
    /**
     * Returns the position of the scale tick in LCARS panel pixels relative to
     * the top (vertical sliders) or left (horizontal sliders).
     */
    public int getPos()
    {
      if (horiz)
        return eLine.getBounds().x-eBack.getBounds().x;
      else
        return eLine.getBounds().y-eBack.getBounds().y;
    }
  
    public float getValue()
    {
      if (Float.isNaN(this.value))
        return posToValue(getPos());
      else
        return this.value;
    }
  }
  
}

// EOF
