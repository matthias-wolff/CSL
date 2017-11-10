package de.tucottbus.kt.csl.lcars.elements;

import java.awt.Rectangle;

import javax.media.j3d.Canvas3D;

import de.tucottbus.kt.lcars.LCARS;
import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.Screen;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.logging.Log;

/**
 * Wraps a {@link javax.media.j3d.Canvas3D Canvas3D} into an an
 * {@link ElementContributor}.
 * 
 * @author Peter Gessler, BTU Cottbus-Senftenberg
 * @author Matthias Wolff, BTU Cottbus-Senftenberg (revision)
 */
public class ECanvas3D extends ElementContributor
{
  /**
   * The 3D canvas wrapped by this element contributor.
   */
  private Canvas3D canvas;

  /**
   * The bounds of this 3D canvas wrapper in LCARS panel coordinates.
   */
  private Rectangle bounds;

  /**
   * Creates a new 3D canvas wrapper element contributor.
   * 
   * @param x
   *          The x-coordinate of the top left corner (LCARS panel coordinates). 
   * @param y
   *          The y-coordinate of the top left corner (LCARS panel coordinates).
   * @param w
   *          The width (LCARS panel coordinates).
   * @param h
   *          The height (LCARS panel coordinates).
   * @param canvas
   *          The 3D canvas to be wrapped.
   */
  public ECanvas3D(int x, int y, int w, int h, Canvas3D canvas)
  {
    super(x, y);
    this.bounds = new Rectangle(x, y, x+w, y+h);
    this.canvas = canvas;
  }

  @Override
  public void addToPanel(Panel panel)
  {
    if (canvas==null)
      return;

    super.addToPanel(panel);
    try
    {
      Screen screen = Screen.getLocal(panel.getScreen());
      canvas.repaint(10);
      screen.addAwtComponent(canvas,bounds.x,bounds.y,bounds.width-bounds.x,bounds.height-bounds.y);
    }
    catch (ClassCastException e)
    {
      canvas = null; // Cannot wrap it
      Log.err("LCARS: 3D canvas wrappers not supported on remote screens.", e);
    }
  }

  @Override
  public void removeFromPanel()
  {
    if (canvas==null)
      return;

    LCARS.getDisplay().syncExec(()->
    {
      try
      {
        Screen screen = Screen.getLocal(getPanel().getScreen());
        screen.removeAwtComponent(canvas);
      }
      catch (ClassCastException e)
      {
        Log.err("LCARS: 3D canvas wrappers not supported on remote screens.", e);
      }
    });

    super.removeFromPanel();
  }
  
}

// EOF
