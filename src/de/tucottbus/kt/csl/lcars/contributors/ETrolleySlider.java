package de.tucottbus.kt.csl.lcars.contributors;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

import de.tucottbus.kt.csl.hardware.micarray3d.MicArrayCeiling;
import de.tucottbus.kt.csl.hardware.micarray3d.trolley.Trolley;
import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.contributors.ESlider;
import de.tucottbus.kt.lcars.elements.EElement;
import de.tucottbus.kt.lcars.elements.EEvent;
import de.tucottbus.kt.lcars.elements.EEventListener;
import de.tucottbus.kt.lcars.elements.EEventListenerAdapter;
import de.tucottbus.kt.lcars.elements.ELabel;
import de.tucottbus.kt.lcars.elements.ERect;
import de.tucottbus.kt.lcars.geometry.GArea;

/**
 * A {@link ESlider} representing the position target and the actual position
 * of the {@linkplain MicArrayCeiling ceiling microphone array}
 * {@linkplain Trolley trolley} in CSL.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Write JavaDocs!
 *     </li>
 * </ul>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class ETrolleySlider extends ESlider
{
  public final ELabel eKnobLabel;
  public final ERect  ePos;
  public final ELabel ePosLabel;
  
  protected EElement eLockControl;
  protected EEventListener lockControlListener;

  public ETrolleySlider(int x, int y, int w, int h)
  {
    super(x, y, w, h, LCARS.EC_SECONDARY, 0);
    setMinMaxValue(-140,249);
    eBack.setAlpha(0.15f);
    
    // Add scale
    addScaleTick(-100f,"-100",LCARS.EF_TINY);
    addScaleTick( -50f, "-50",LCARS.EF_TINY);
    ScaleTick tick = addScaleTick(0f,"0",LCARS.EF_TINY);
    tick.eLine.setColorStyle(LCARS.EC_PRIMARY);
    tick.eLabel.setColorStyle(LCARS.EC_PRIMARY);
    tick.eLine.setSelected(true);
    tick.eLabel.setSelected(true);
    addScaleTick( 50f, "50",LCARS.EF_TINY);
    addScaleTick(100f,"100",LCARS.EF_TINY);
    addScaleTick(150f,"150",LCARS.EF_TINY);
    addScaleTick(200f,"200",LCARS.EF_TINY);
    addScaleLabel(valueToPos(230f),"Y/cm",LCARS.EF_TINY);
    
    // Add position marker
    Rectangle b = eKnob.getBounds();
    b.x -= 5; b.y -= 5; b.width += 10; b.height += 10;
    ePos = new ERect(null,b.x,b.y,b.width,b.height,LCARS.ES_STATIC|LCARS.EC_ELBOUP|LCARS.ES_RECT_RND, null);
    ePos.addGeometryModifier((geos)->
    {
      GArea g = (GArea)geos.get(0);
      Area a = g.getArea();
      Rectangle bnds = g.getBounds();
      int aw = Math.min(bnds.width-10,bnds.height-10);
      Area inner = new Area(new RoundRectangle2D.Float(bnds.x+5,bnds.y+5,bnds.width-10,bnds.height-10,aw,aw));
      a.subtract(inner);
      g.setShape(a);
      bnds.x += bnds.width;
      bnds.width = 26;
      bnds.y = bnds.y + bnds.height / 2 - 1;
      bnds.height = 3;
      geos.add(new GArea(new Area(bnds), false));

    });
    add(ePos,false);
    ePosLabel = new ELabel(null,b.x+b.width+3,b.y,23,b.height,LCARS.ES_STATIC|LCARS.EC_ELBOUP|LCARS.ES_LABEL_E|LCARS.EF_SMALL," ACT\n 000");
    add(ePosLabel,false);
    
    // Extend knob
    eKnob.addGeometryModifier((geos)->
    {
      GArea gKnob = (GArea)geos.get(0);
      Rectangle bnds = gKnob.getBounds();
      bnds.x -= 30;
      bnds.width = 31;
      bnds.y = bnds.y + bnds.height / 2 - 1;
      bnds.height = 3;
      geos.add(new GArea(new Area(bnds), false));
    });
    b = eKnob.getBounds();
    b.x -= 33; b.width = 34;
    b.y = b.y+b.height/2-20; b.height=41;
    int style = eKnob.getStyle() & (~LCARS.ES_LABEL) | LCARS.ES_LABEL_W;
    eKnobLabel = new ELabel(null,b.x,b.y,b.width,b.height,style," TGT\n 000");
    remove(eKnob);
    add(eKnobLabel,false);
    add(eKnob,false);
    
    // Initialize
    setValue(0f);
    setActualValue(0f);
  }

  public void setActualValue(float value)
  {
    setActualPos(valueToPos(value));
  }
  
  public float getActualValue()
  {
    return posToValue(getActualPos());
  }
  
  public void setLockControl(EElement eLockControl)
  {
    if (lockControlListener==null)
      lockControlListener = new EEventListenerAdapter()
      {
        @Override
        public void touchDown(EEvent ee)
        {
          setLocked(!isLocked());
        }
      };
    
    if (this.eLockControl!=null)
      eLockControl.removeEEventListener(lockControlListener);
    
    this.eLockControl = eLockControl;
    if (eLockControl!=null)
    {
      eLockControl.addEEventListener(lockControlListener);
      eLockControl.setBlinking(isLocked());
    }
  }
  
  public void setLocked(boolean locked)
  {
    eKnob.setVisible(!locked);
    eKnobLabel.setVisible(!locked);
    if (!locked)
      setKnobPos(getActualPos());
    setStatic(locked);
    if (eLockControl!=null)
      eLockControl.setBlinking(locked);
  }
  
  public boolean isLocked()
  {
    return isStatic();
  }

  @Override
  protected boolean setKnobPos(int pos)
  {
    boolean hasChanged = super.setKnobPos(pos); 
    
    Rectangle bKnob = eKnob.getBounds();
    Rectangle bLab = eKnobLabel.getBounds();
    bLab.y = bKnob.y+bKnob.height/2-bLab.height/2 -1;
    eKnobLabel.setBounds(bLab);
    eKnobLabel.setLabel(String.format(" TGT\n% 04d",Math.round(posToValue(pos))));

    return hasChanged;
  }
  
  protected boolean setActualPos(int pos)
  {
    Rectangle b = ePos.getBounds();
    if (horiz)
      b.x = eBack.getBounds().x+pos-b.width/2;
    else
      b.y = eBack.getBounds().y+pos-b.height/2;
    if (pos==getActualPos())
      return false;
    ePos.setBounds(b);
    
    Rectangle bAct = ePos.getBounds();
    Rectangle bLab = ePosLabel.getBounds();
    bLab.y = bAct.y+bAct.height/2-bLab.height/2 -1;
    ePosLabel.setBounds(bLab);
    ePosLabel.setLabel(String.format(" ACT\n% 04d",Math.round(posToValue(pos))));

    return true;
  }
  
  protected int getActualPos()
  {
    Rectangle b = ePos.getBounds();
    if (horiz)
      return b.x + b.width/2 - eBack.getBounds().x;
    else
      return b.y + b.height/2 - eBack.getBounds().y;
  }

}

// EOF