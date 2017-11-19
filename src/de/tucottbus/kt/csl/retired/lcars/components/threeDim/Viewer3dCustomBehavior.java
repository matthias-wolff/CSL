package de.tucottbus.kt.csl.retired.lcars.components.threeDim;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import ij3d.behaviors.ContentTransformer;
import ij3d.behaviors.InteractiveBehavior;
import ij3d.behaviors.InteractiveViewPlatformTransformer;
import ij3d.behaviors.Picker;
import orthoslice.OrthoGroup;
import voltex.VolumeRenderer;

/**
 * Custom MouseBehavior for Viewer3D. It interprets mouse events to a
 * desired action.
 * 
 * @author Martin Birth
 */
@Deprecated
public class Viewer3dCustomBehavior extends InteractiveBehavior {
  
  private static final int PICK_POINT_MASK = MouseEvent.BUTTON1_DOWN_MASK;
  private static final int DELETE_POINT_MASK = InputEvent.ALT_DOWN_MASK |
      MouseEvent.BUTTON1_DOWN_MASK;
  
  private final ImageCanvas3D canvas;

  private final ContentTransformer contentTransformer;
  private final Picker picker;
  private final InteractiveViewPlatformTransformer viewTransformer;
  

  /**
   * Constructor.
   * 
   * @param univ
   */
  public Viewer3dCustomBehavior(Image3DUniverse univ) {
    super(univ);
    this.canvas = (ImageCanvas3D) univ.getCanvas();
    this.contentTransformer = univ.getContentTransformer();
    this.picker = univ.getPicker();
    this.viewTransformer = univ.getViewPlatformTransformer();
  }
  
  @Override
  public void doProcess(MouseEvent e) {
    
    int id = e.getID();
    int mask = e.getModifiersEx();
    Content c = univ.getSelected();
    if (id == MouseEvent.MOUSE_PRESSED) {
      if(c != null && !c.isLocked()) contentTransformer.init(c, e.getX(), e.getY());
      else viewTransformer.init(e);
      if(univ.ui.isPointTool()) {
        Content sel = c;
        if(sel == null && ((Image3DUniverse)univ).getContents().size() == 1)
          sel = (Content)univ.contents().next();
        if(sel != null) {
          sel.showPointList(true);
          e.consume();
        } if(mask == PICK_POINT_MASK) {
          picker.addPoint(sel, e);
          e.consume();
        } else if((mask & DELETE_POINT_MASK) == DELETE_POINT_MASK) {
          picker.deletePoint(sel, e);
          e.consume();
        }
      }
      if(!e.isConsumed())
        canvas.getRoiCanvas().mousePressed(e);
    } else if (id == MouseEvent.MOUSE_DRAGGED) {
      if(c != null && !c.isLocked() && (MouseEvent.BUTTON1_DOWN_MASK == (mask & MouseEvent.BUTTON1_DOWN_MASK))) contentTransformer.rotate(e);
      else viewTransformer.rotate(e);
      e.consume();
      
      if (!e.isConsumed())
          canvas.getRoiCanvas().mouseDragged(e);;
      univ.fireContentChanged(c);
    } else if (id == MouseEvent.MOUSE_RELEASED) {
      if (univ.ui.isPointTool()) {
        picker.stopMoving();
        e.consume();
      }
      if (!e.isConsumed())
        canvas.getRoiCanvas().mouseReleased(e);
    }
    
    if(id == MouseEvent.MOUSE_WHEEL) {
      int axis = -1;
      if(canvas.isKeyDown(KeyEvent.VK_X))
        axis = VolumeRenderer.X_AXIS;
      else if(canvas.isKeyDown(KeyEvent.VK_Y))
        axis = VolumeRenderer.Y_AXIS;
      else if(canvas.isKeyDown(KeyEvent.VK_Z))
        axis = VolumeRenderer.Z_AXIS;
      if(c != null && c.getType() == Content.ORTHO
                && axis != -1) {
        MouseWheelEvent we = (MouseWheelEvent)e;
        int units = 0;
        if(we.getScrollType() ==
          MouseWheelEvent.WHEEL_UNIT_SCROLL)
          units = we.getUnitsToScroll();
        for(ContentInstant ci : c.getInstants().values()) {
          OrthoGroup og = (OrthoGroup)ci.getContent();
          if(units > 0) og.increase(axis);
          else if(units < 0) og.decrease(axis);
        }
        univ.fireContentChanged(c);
      } else {
        viewTransformer.wheel_zoom(e);
      }
      e.consume();
    }
  }
  
}