package incubator.csl.lcars.micarr.contributors;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.ArrayList;

import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.elements.modify.EGeometryModifier;
import de.tucottbus.kt.lcars.geometry.AGeometry;
import de.tucottbus.kt.lcars.geometry.GArea;

/**
 * A {@link ECslSlider} with a cursor line attached to the slider knob and with 
 * grid lines extended to the cursor length.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Use {@link ECslSliderCursor} in {@link ESensitivityPlots}</li>
 * </ul>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class ECslSliderCursor extends ECslSlider
{
  // -- Constants --

  /**
   * Style constant for a vertical slider with a horizontal cursor line attached
   * to the left side of the slider knob.
   */
  public static final int ES_VERT_LINE_W = ES_VERTICAL | 0x00000000;

  /**
   * Style constant for a vertical slider with a horizontal cursor line attached
   * to the right side of the slider knob.
   */
  public static final int ES_VERT_LINE_E = ES_VERTICAL | 0x40000000;

  /**
   * Style constant for a horizontal slider with a vertical cursor line attached
   * to the top of the slider knob.
   */
  public static final int ES_HORIZ_LINE_N = ES_HORIZONTAL | 0x00000000;

  /**
   * Style constant for a horizontal slider with a vertical cursor line attached
   * to the bottom of the slider knob.
   */
  public static final int ES_HORIZ_LINE_S = ES_HORIZONTAL | 0x40000000;
  
  /**
   * Rotates the knob by 90 degrees.
   */
  public static final int ES_ROTATE_KNOB = 0x80000000;
  
  /**
   * Cursor line is east or south.
   */
  protected final boolean lineES;
  
  /**
   * Knob is rotated by 90 degrees.
   */
  protected final boolean rotateKnob;
  
  protected final int w;
  protected final int h;
  protected final int cl;
  protected final int cw;
  
  /**
   * Creates a new slider cursor. A slider cursor is a {@link ECslSlider} with a
   * cursor line attached to the slider knob.
   * 
   * <h3>Remarks:</h3>
   * <ul>
   *   <li>Using the {@link #ES_ROTATE_KNOB} style causes on offset of the bounding
   *   rectangle of <code>w</code>/4 for vertical sliders and <code>h</code>/4 for
   *   horizontal sliders.</li>
   *   <li>If possible, the smaller dimension, i.e. min(<code>w</code>, <code>h</code>),
   *   should be a multiple of 8. This ensures a precise layout. Otherwise, rounding 
   *   errors may lead to slight inaccuracies.</li>
   * </ul>
   *  
   * @param x
   *          The x-coordinate of the upper left corner of the slider's bounding
   *          rectangle (in LCARS panel pixels).
   * @param y
   *          The y-coordinate of the upper left corner of the slider's bounding
   *          rectangle (in LCARS panel pixels).
   * @param w
   *          The width of the slider's bounding rectangle (in LCARS panel
   *          pixels). 
   * @param h
   *          The height of the slider's bounding rectangle (in LCARS panel
   *          pixels). 
   * @param style
   *          A combination of color style ({@link LCARS}<code>.ES_XXX</code>),
   *          {@link ECslSliderCursor}<code>.ES_XXX</code>. Add
   *          {@link LCARS#ES_STATIC} if the cursor shall not be movable by the
   *          user.
   * @param fatFingerMargin
   *          Margin of touch-sensitive area around the slider's bounding
   *          rectangle (in LCARS panel pixels).
   * @param cl
   *          The length of the cursor line (in LCARS panel pixels). The cursor
   *          line will be place <em>outside</em> the bounding rectangle defined
   *          by <code>x</code>, <code>y</code>, <code>w</code>, and
   *          <code>h</code> in the direction indicated by the
   *          {@link ECslSliderCursor}<code>.ES_XXX</code>. constant used in
   *          <code>sty.e</code>.
   * @param cw
   *          The width of the cursor line (in LCARS panel pixels).
   */
  public ECslSliderCursor
  (
    int x, 
    int y, 
    int w, 
    int h, 
    int style,
    int fatFingerMargin,
    int cl,
    int cw
  )
  {
    super(x,y,w,h,getSliderStyle(style),fatFingerMargin);
    
    this.lineES = (style & 0x40000000)!=0;
    this.rotateKnob = (style & 0x80000000)!=0;
    this.w = w;
    this.h = h;
    this.cl = cl;
    this.cw = cw;

    eKnob.addGeometryModifier(new EGeometryModifier()
    {
      @Override
      public void modify(ArrayList<AGeometry> geos)
      {
        try
        {
          // Rotate the knob
          GArea geo = (GArea)geos.get(0);
          Rectangle b = geo.getBounds();
          if (rotateKnob)
          {
            AffineTransform t = new AffineTransform();
            t.setToRotation(Math.PI/2,b.x+b.width/2,b.y+b.height/2);
            Area area = geo.getArea();
            area.transform(t);
            geo.setShape(area);
          }
          
          // Add cursor line
          geo = (GArea)geos.get(0);
          b = geo.getBounds();
          Area line;
          if (horiz)
          {
            if (lineES)
              line = new Area(new Rectangle(b.x+b.width/2-cw/2,b.y+b.height,cw,cl));
            else
              line = new Area(new Rectangle(b.x+b.width/2-cw/2,b.y-cl,cw,cl));
          }
          else
          {
            if (lineES)
              line = new Area(new Rectangle(b.x+b.width,b.y+b.height/2-cw/2,cl,cw));
            else
              line = new Area(new Rectangle(b.x-cl,b.y+b.height/2-cw/2,cl,cw));
          }
          geos.add(new GArea(line,false));
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
      }
    });

    // Adjust slider background if knob is rotated
    if (rotateKnob)
      eBack.addGeometryModifier(new EGeometryModifier()
      {
        @Override
        public void modify(ArrayList<AGeometry> geos)
        {
          GArea geo = (GArea)geos.get(0);
          Rectangle b = geo.getBounds();
          if (horiz) { b.y += h/8; b.height = h/2; }
          else       { b.x += w/8; b.width = w/2;  }
          geo.setShape(b);
        }
      });
  }

  @Override
  protected ScaleTick add(ScaleTick scaleTick)
  {
    ScaleTick st = super.add(scaleTick);
    
    // Modify tick line
    if (st.eLine!=null)
      st.eLine.addGeometryModifier(new EGeometryModifier()
      {
        @Override
        public void modify(ArrayList<AGeometry> geos)
        {
          GArea geo = (GArea)geos.get(0);
          Rectangle b = geo.getBounds();
          
          // TODO: Adjust tick line length for rotated knob
          if (rotateKnob)
          {
            if (horiz) { b.y += h/8; b.height = h/2; }
            else       { b.x += w/8; b.width = w/2;  }
          }
          
          // TODO: Enlarge bounding rectangle
          if (horiz)
          {
            int offset = rotateKnob ? 0 : h/8;
            b.y += lineES ? 0 : -cl-offset;
            b.height += cl+offset;
          }
          else
          {
            int offset = rotateKnob ? 0 : w/8;
            b.x += lineES ? 0 : -cl-offset;
            b.width += cl+offset;
          }
          geo.setShape(b);
        }
      });
    
    // Modify tick label
    if (rotateKnob && !horiz && st.eLabel!=null)
    {
      Rectangle b = st.eLabel.getBounds();
      b.x -= eBack.getBounds().width/8;
      st.eLabel.setBounds(b);
     }
    
    return st;
  }
  
  private static int getSliderStyle(int style)
  {
    return style & 0x3FFFFFFF;
  }

}

// EOF
