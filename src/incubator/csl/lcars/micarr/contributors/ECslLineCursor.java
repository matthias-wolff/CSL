package incubator.csl.lcars.micarr.contributors;

import de.tucottbus.kt.lcars.LCARS;

/**
 * A line cursor with a scale and a knob.
 * 
 * <h3>Remarks:</h3>
 * <ul>
 *   <li>TODO: Implement {@link ECslLineCursor}</li>
 *   <li>TODO: Use {@link ECslLineCursor} in {@link ESensitivityPlots}</li>
 * </ul>
 * 
 * @author Matthias Wolff, BTU Cottbus-Senftenberg
 */
public class ECslLineCursor extends ECslSlider
{
  // -- Constants --

  /**
   * A horizontal line cursor with knob on the left side. 
   */
  public static final int ES_HORIZ_KNOB_W = 0x00000000;

  /**
   * A horizontal line cursor with knob on the right side. 
   */
  public static final int ES_HORIZ_KNOB_E = 0x10000000;

  /**
   * A vertical line cursor with knob at the top. 
   */
  public static final int ES_VERT_KNOB_N = 0x20000000;

  /**
   * A vertical line cursor with knob at the bottom. 
   */
  public static final int ES_VERT_KNOB_S = 0x30000000;

  /**
   * TODO: Write JavaDoc
   * 
   * @param style
   *          A combination of color style ({@link LCARS}<code>.ES_XXX</code>),
   *          {@link ECslLineCursor}<code>.ES_XXX</code>. Add
   *          {@link LCARS#ES_STATIC} if the cursor shall not be movable by the
   *          user.
   */
  public ECslLineCursor
  (
    int x, 
    int y, 
    int w, 
    int h, 
    int style,
    int fatFingerMargin
  )
  {
    super(x, y, w, h, style, fatFingerMargin);
  }

}

// EOF
