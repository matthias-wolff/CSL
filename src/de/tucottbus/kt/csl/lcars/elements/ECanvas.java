package de.tucottbus.kt.csl.lcars.elements;

import java.awt.Point;
import java.awt.Rectangle;

import javax.media.j3d.Canvas3D;

import de.tucottbus.kt.lcars.Panel;
import de.tucottbus.kt.lcars.Screen;
import de.tucottbus.kt.lcars.contributors.ElementContributor;
import de.tucottbus.kt.lcars.logging.Log;

public class ECanvas extends ElementContributor {
  /**
   * The LCARS screen this World Wind wrapper is on.
   */
  private Screen screen;
  
  private Canvas3D canvas;
  
  private boolean isVisible;
  
  private Panel panel = null;
  
  /**
   * The bounds of this World Wind wrapper in LCARS panel coordinates.
   */
  private Rectangle bounds;
  
  public ECanvas(int x, int y, int w, int h, Canvas3D canvas)
  {
    super(x, y);
    this.bounds = new Rectangle(x,y,w,h);
    this.canvas=canvas;
    this.isVisible = true;
  }
  
  // FIXME: works really slow
  @Override
  public void addToPanel(Panel panel)
  {
    if (canvas==null)
      return;
    
    super.addToPanel(panel);
    
    this.panel = panel;
    
    try
    {
      screen = Screen.getLocal(panel.getScreen());
      Point tl = screen.panelToScreen(new Point(bounds.x,bounds.y));
      Point br = screen.panelToScreen(new Point(bounds.x+bounds.width,bounds.y+bounds.height));
      canvas.setBounds(tl.x,tl.y,br.x-tl.x,br.y-tl.y);
      screen.add(canvas);
      
    }
    catch (ClassCastException e)
    {
      Log.err("LCARS: Function not supported on remote screens.", e);
    }
  }
  
  @Override
  public void removeFromPanel()
  {
    if (panel==null) return;
    if (screen!=null && canvas!=null)
    {
      screen.remove(canvas);
    }
    super.removeFromPanel();
  }
  
  public boolean setVisible(boolean mode) {
    
    if (mode == isVisible)
      return true;
    
    if (mode == false) {
      
      isVisible = false;
      removeFromPanel();
      return false;
    } else {
      
      isVisible = true;
      addToPanel(this.panel);
      return true;
    }
  }

}
