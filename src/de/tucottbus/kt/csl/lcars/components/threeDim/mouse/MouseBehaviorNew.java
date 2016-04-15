package de.tucottbus.kt.csl.lcars.components.threeDim.mouse;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Enumeration;
import java.util.LinkedList;

import javax.media.j3d.Behavior;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.media.j3d.WakeupOnBehaviorPost;
import javax.media.j3d.WakeupOr;

import com.sun.j3d.internal.J3dUtilsI18N;

/**
 * Base class for all mouse manipulators (see MouseRotate, MouseZoom and
 * MouseTranslate for examples of how to extend this base class).
 */

public abstract class MouseBehaviorNew extends Behavior implements
    MouseListener, MouseMotionListener, MouseWheelListener {

  private boolean listener = false;

  protected WakeupCriterion[] mouseEvents;
  protected WakeupOr mouseCriterion;
  protected int x, y, z;
  protected int x_last, y_last, z_last;
  protected TransformGroup transformGroup;
  protected Transform3D transformX;
  protected Transform3D transformY;
  protected Transform3D translateOld;
  protected boolean buttonPress = false;
  protected boolean reset = false;
  protected boolean invert = false;
  protected boolean wakeUp = false;
  protected int flags = 0;

  // to queue the mouse events
  @SuppressWarnings("rawtypes")
  protected LinkedList mouseq;

  // true if this behavior is enable
  protected boolean enable = true;

  /**
   * Set this flag if you want to manually wakeup the behavior.
   */
  public static final int MANUAL_WAKEUP = 0x1;

  /**
   * Set this flag if you want to invert the inputs. This is useful when the
   * transform for the view platform is being changed instead of the transform
   * for the object.
   */
  public static final int INVERT_INPUT = 0x2;

  /**
   * Creates a mouse behavior object with a given transform group.
   * 
   * @param transformGroup
   *          The transform group to be manipulated.
   */
  public MouseBehaviorNew(TransformGroup transformGroup) {
    super();
    // need to remove old behavior from group
    this.transformGroup = transformGroup;
    translateOld = new Transform3D();
    transformX = new Transform3D();
    transformY = new Transform3D();
    reset = true;
  }

  /**
   * Initializes standard fields. Note that this behavior still needs a
   * transform group to work on (use setTransformGroup(tg)) and the transform
   * group must add this behavior.
   * 
   * @param format
   *          flags
   */
  public MouseBehaviorNew(int format) {
    super();
    flags = format;
    translateOld = new Transform3D();
    transformX = new Transform3D();
    transformY = new Transform3D();
    reset = true;
  }

  /**
   * Creates a mouse behavior that uses AWT listeners and behavior posts rather
   * than WakeupOnAWTEvent. The behaviors is added to the specified Component
   * and works on the given TransformGroup. A null component can be passed to
   * specify the behaviors should use listeners. Components can then be added to
   * the behavior with the addListener(Component c) method.
   * 
   * @param c
   *          The Component to add the MouseListener and MouseMotionListener to.
   * @param transformGroup
   *          The TransformGroup to operate on.
   * @since Java 3D 1.2.1
   */
  public MouseBehaviorNew(Component c, TransformGroup transformGroup) {
    this(transformGroup);
    if (c != null) {
      c.addMouseListener(this);
      c.addMouseMotionListener(this);
      c.addMouseWheelListener(this);
    }
    listener = true;
  }

  /**
   * Creates a mouse behavior that uses AWT listeners and behavior posts rather
   * than WakeupOnAWTEvent. The behavior is added to the specified Component. A
   * null component can be passed to specify the behavior should use listeners.
   * Components can then be added to the behavior with the addListener(Component
   * c) method. Note that this behavior still needs a transform group to work on
   * (use setTransformGroup(tg)) and the transform group must add this behavior.
   * 
   * @param format
   *          interesting flags (wakeup conditions).
   * @since Java 3D 1.2.1
   */
  public MouseBehaviorNew(Component c, int format) {
    this(format);
    if (c != null) {
      c.addMouseListener(this);
      c.addMouseMotionListener(this);
      c.addMouseWheelListener(this);
    }
    listener = true;
  }

  /**
   * Swap a new transformGroup replacing the old one. This allows manipulators
   * to operate on different nodes.
   * 
   * @param transformGroup
   *          The *new* transform group to be manipulated.
   */
  public void setTransformGroup(TransformGroup transformGroup) {
    // need to remove old behavior from group
    this.transformGroup = transformGroup;
    translateOld = new Transform3D();
    transformX = new Transform3D();
    transformY = new Transform3D();
    reset = true;
  }

  /**
   * Return the transformGroup on which this node is operating
   */
  public TransformGroup getTransformGroup() {
    return this.transformGroup;
  }

  /**
   * Initializes the behavior.
   */

  @SuppressWarnings("rawtypes")
  @Override
  public void initialize() {
    mouseEvents = new WakeupCriterion[4];

    if (!listener) {
      mouseEvents[0] = new WakeupOnAWTEvent(MouseEvent.MOUSE_DRAGGED);
      mouseEvents[1] = new WakeupOnAWTEvent(MouseEvent.MOUSE_PRESSED);
      mouseEvents[2] = new WakeupOnAWTEvent(MouseEvent.MOUSE_RELEASED);
      mouseEvents[3] = new WakeupOnAWTEvent(MouseEvent.MOUSE_WHEEL);
    } else {
      mouseEvents[0] = new WakeupOnBehaviorPost(this, MouseEvent.MOUSE_DRAGGED);
      mouseEvents[1] = new WakeupOnBehaviorPost(this, MouseEvent.MOUSE_PRESSED);
      mouseEvents[2] = new WakeupOnBehaviorPost(this, MouseEvent.MOUSE_RELEASED);
      mouseEvents[3] = new WakeupOnBehaviorPost(this, MouseEvent.MOUSE_WHEEL);
      mouseq = new LinkedList();
    }
    mouseCriterion = new WakeupOr(mouseEvents);
    wakeupOn(mouseCriterion);
    x = 0;
    y = 0;
    x_last = 0;
    y_last = 0;
  }

  /**
   * Manually wake up the behavior. If MANUAL_WAKEUP flag was set upon creation,
   * you must wake up this behavior each time it is handled.
   */

  public void wakeup() {
    wakeUp = true;
  }

  /**
   * Handles mouse events
   */
  public void processMouseEvent(MouseEvent evt) {
    if (evt.getID() == MouseEvent.MOUSE_PRESSED) {
      buttonPress = true;
      return;
    } else if (evt.getID() == MouseEvent.MOUSE_RELEASED) {
      buttonPress = false;
      wakeUp = false;
    }
    /*
     * else if (evt.getID() == MouseEvent.MOUSE_MOVED) { // Process mouse move
     * event } else if (evt.getID() == MouseEvent.MOUSE_WHEEL) { // Process
     * mouse wheel event }
     */
  }

  /**
   * All mouse manipulators must implement this.
   */
  @SuppressWarnings("rawtypes")
  @Override
  public abstract void processStimulus(Enumeration criteria);

  /**
   * Adds this behavior as a MouseListener, mouseWheelListener and
   * MouseMotionListener to the specified component. This method can only be
   * called if the behavior was created with one of the constructors that takes
   * a Component as a parameter.
   * 
   * @param c
   *          The component to add the MouseListener, MouseWheelListener and
   *          MouseMotionListener to.
   * @exception IllegalStateException
   *              if the behavior was not created as a listener
   * @since Java 3D 1.2.1
   */
  public void addListener(Component c) {
    if (!listener) {
      throw new IllegalStateException(J3dUtilsI18N.getString("Behavior0"));
    }
    c.addMouseListener(this);
    c.addMouseMotionListener(this);
    c.addMouseWheelListener(this);
  }

  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}

  @SuppressWarnings("unchecked")
  @Override
  public void mousePressed(MouseEvent e) {
    // System.out.println("mousePressed");

    // add new event to the queue
    // must be MT safe
    if (enable) {
      synchronized (mouseq) {
        mouseq.add(e);
        // only need to post if this is the only event in the queue
        if (mouseq.size() == 1)
          postId(MouseEvent.MOUSE_PRESSED);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void mouseReleased(MouseEvent e) {
    // System.out.println("mouseReleased");

    // add new event to the queue
    // must be MT safe
    if (enable) {
      synchronized (mouseq) {
        mouseq.add(e);
        // only need to post if this is the only event in the queue
        if (mouseq.size() == 1)
          postId(MouseEvent.MOUSE_RELEASED);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void mouseDragged(MouseEvent e) {
    // System.out.println("mouseDragged");

    // add new event to the to the queue
    // must be MT safe.
    if (enable) {
      synchronized (mouseq) {
        mouseq.add(e);
        // only need to post if this is the only event in the queue
        if (mouseq.size() == 1)
          postId(MouseEvent.MOUSE_DRAGGED);
      }
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {}

  @Override
  public void setEnable(boolean state) {
    super.setEnable(state);
    this.enable = state;
    if (!enable && (mouseq != null)) {
      mouseq.clear();
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    System.out.println("MouseBehavior : mouseWheel enable = " + enable);

    // add new event to the to the queue
    // must be MT safe.
    if (enable) {
      synchronized (mouseq) {
        mouseq.add(e);
        // only need to post if this is the only event in the queue
        if (mouseq.size() == 1)
          postId(MouseEvent.MOUSE_WHEEL);
      }
    }
  }
}
