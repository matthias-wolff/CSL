package de.tucottbus.kt.csl.lcars.components.threeDim.mouse;

/*
 * $RCSfile: MouseTranslate.java,v $
 *
 * Copyright (c) 2007 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 * $Revision: 1.4 $
 * $Date: 2007/02/09 17:20:13 $
 * $State: Exp $
 */

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Enumeration;

import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.WakeupCriterion;
import javax.media.j3d.WakeupOnAWTEvent;
import javax.media.j3d.WakeupOnBehaviorPost;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.sun.j3d.utils.behaviors.mouse.MouseBehaviorCallback;

import de.tucottbus.kt.csl.lcars.components.threeDim.Cube3dObjects;

/**
 * MouseTranslate is a Java3D behavior object that lets users control the
 * translation (X, Y) of an object via a mouse drag motion with the third mouse
 * button (alt-click on PC). See MouseRotate for similar usage info.
 */

public class MouseTranslateNew extends MouseBehaviorNew {

  double x_factor, y_factor= .010;
  Vector3d translationVector = new Vector3d();
  Canvas3D canvas;

  private final Vector3d lastPosition = new Vector3d();
  
  private MouseBehaviorCallback callback = null;

  /**
   * Creates a mouse translate behavior given the transform group.
   * 
   * @param transformGroup
   *          The transformGroup to operate on.
   */
  public MouseTranslateNew(Canvas3D canvas, TransformGroup transformGroup) {
    super(transformGroup);
    this.canvas=canvas;
  }

  /**
   * Creates a default translate behavior.
   */
  public MouseTranslateNew() {
    super(0);
  }

  /**
   * Creates a translate behavior. Note that this behavior still needs a
   * transform group to work on (use setTransformGroup(tg)) and the transform
   * group must add this behavior.
   * 
   * @param flags
   */
  public MouseTranslateNew(int flags) {
    super(flags);
  }

  /**
   * Creates a translate behavior. Note that this behavior still needs a
   * transform group to work on (use setTransformGroup(tg)) and the transform
   * group must add this behavior.
   * 
   * @param flags
   */
  public MouseTranslateNew(int flags, Canvas3D canvas) {
    super(flags);
    this.canvas = canvas;
  }

  /**
   * Creates a translate behavior that uses AWT listeners and behavior posts
   * rather than WakeupOnAWTEvent. The behavior is added to the specified
   * Component. A null component can be passed to specify the behavior should
   * use listeners. Components can then be added to the behavior with the
   * addListener(Component c) method.
   * 
   * @param c
   *          The Component to add the MouseListener and MouseMotionListener to.
   * @since Java 3D 1.2.1
   */
  public MouseTranslateNew(Component c) {
    super(c, 0);
  }

  /**
   * Creates a translate behavior that uses AWT listeners and behavior posts
   * rather than WakeupOnAWTEvent. The behaviors is added to the specified
   * Component and works on the given TransformGroup. A null component can be
   * passed to specify the behavior should use listeners. Components can then be
   * added to the behavior with the addListener(Component c) method.
   * 
   * @param c
   *          The Component to add the MouseListener and MouseMotionListener to.
   * @param transformGroup
   *          The TransformGroup to operate on.
   * @since Java 3D 1.2.1
   */
  public MouseTranslateNew(Component c, TransformGroup transformGroup) {
    super(c, transformGroup);
  }

  /**
   * Creates a translate behavior that uses AWT listeners and behavior posts
   * rather than WakeupOnAWTEvent. The behavior is added to the specified
   * Component. A null component can be passed to specify the behavior should
   * use listeners. Components can then be added to the behavior with the
   * addListener(Component c) method. Note that this behavior still needs a
   * transform group to work on (use setTransformGroup(tg)) and the transform
   * group must add this behavior.
   * 
   * @param flags
   *          interesting flags (wakeup conditions).
   * @since Java 3D 1.2.1
   */
  public MouseTranslateNew(Component c, int flags) {
    super(c, flags);
  }

  @Override
  public void initialize() {
    super.initialize();
    if ((flags & INVERT_INPUT) == INVERT_INPUT) {
      invert = true;
      x_factor *= -1;
      y_factor *= -1;
    }
  }

  @SuppressWarnings("rawtypes")
  @Override
  public void processStimulus(Enumeration criteria) {
    WakeupCriterion wakeup;
    AWTEvent[] events;
    MouseEvent evt;
    // int id;
    // int dx, dy;

    while (criteria.hasMoreElements()) {
      wakeup = (WakeupCriterion) criteria.nextElement();

      if (wakeup instanceof WakeupOnAWTEvent) {
        events = ((WakeupOnAWTEvent) wakeup).getAWTEvent();
        if (events.length > 0) {
          evt = (MouseEvent) events[events.length - 1];
          doProcess(evt);
        }
      }

      else if (wakeup instanceof WakeupOnBehaviorPost) {
        while (true) {
          // access to the queue must be synchronized
          synchronized (mouseq) {
            if (mouseq.isEmpty())
              break;
            evt = (MouseEvent) mouseq.remove(0);
            // consolodate MOUSE_DRAG events
            while ((evt.getID() == MouseEvent.MOUSE_DRAGGED)
                && !mouseq.isEmpty()
                && (((MouseEvent) mouseq.get(0)).getID() == MouseEvent.MOUSE_DRAGGED)) {
              evt = (MouseEvent) mouseq.remove(0);
            }
          }
          doProcess(evt);
        }
      }

    }
    wakeupOn(mouseCriterion);
  }

  void doProcess(MouseEvent evt) {
    int id;
    double dx, dy, dz;
    
    processMouseEvent(evt);

    if (((buttonPress) && ((flags & MANUAL_WAKEUP) == 0))
        || ((wakeUp) && ((flags & MANUAL_WAKEUP) != 0))) {
      id = evt.getID();
      if ((id == MouseEvent.MOUSE_DRAGGED) && !evt.isAltDown() 
          && !evt.isMetaDown()) {
        
        x = evt.getX();
        y = evt.getY();
        
        // transforming 2D in virtual 3D positions
        Point3d imagePlatePoint = new Point3d();
        Transform3D trans = new Transform3D();
        canvas.getPixelLocationInImagePlate(x, y, imagePlatePoint);
        //System.out.println("ImagePlatePt:  "+imagePlatePoint.toString());
        
        canvas.getImagePlateToVworld(trans);
        trans.transform(imagePlatePoint);
        //System.out.println("WorldPt:       "+imagePlatePoint.toString());
        
        // get the direction of the picking shape
        Point3d startPosition = new Point3d();
        canvas.getCenterEyeInImagePlate(startPosition);
        
        trans.transform(startPosition);
        imagePlatePoint.sub(startPosition);
        
        Vector3d mouseDirection = new Vector3d();
        mouseDirection.set(imagePlatePoint);
        mouseDirection.normalize();
        
        Point3d endPosition = new Point3d();
        endPosition.add(startPosition,mouseDirection);
        
        /*
        dx = x - x_last;
        dy = y - y_last;
        */
        
        // get slider positions
        Point3d[] slider = Cube3dObjects.getSliderVirtualPosFromTg(transformGroup);
        Point3d p1Slider = new Point3d(slider[0]);
        Point3d p2Slider = new Point3d(slider[1]);
        p1Slider.sub(p2Slider);
        Vector3d sliderDirection = new Vector3d(p1Slider);
        
        Vector3d axis = new Vector3d();
        axis.cross(sliderDirection,mouseDirection);
        
        /*
        System.out.println("StartPosition: "+startPosition.toString());
        System.out.println("EndPosition:   "+endPosition.toString());
        System.out.println("Direction:     "+mouseDirection.toString());
        System.out.println("SliderVec:     "+p1Slider.toString());
        */
        
        dx = imagePlatePoint.x;
        dy = imagePlatePoint.y;
        dz = imagePlatePoint.z;
        
        if ((!reset) && ((Math.abs(dy) < 50) && (Math.abs(dx) < 50) && (Math.abs(dz) < 50))) {

          transformGroup.getTransform(translateOld);
          
          // TODO: translate the 3d coordinates with the right vector
          translationVector.set(imagePlatePoint);
          
          translateOld.set(translationVector);
          transformGroup.setTransform(translateOld);

          transformChanged( translateOld );

          if (callback != null)
            callback.transformChanged(MouseBehaviorCallback.TRANSLATE,
                translateOld);

        } else {
          reset = false;
        }
        x_last = x;
        y_last = y;
        lastPosition.set(imagePlatePoint);
      } else if (id == MouseEvent.MOUSE_PRESSED) {
        x_last = evt.getX();
        y_last = evt.getY();
      }
    }
  }
  
   /**
   * Get the intersection point.
   * 
   * @param eyePos
   * @param mousePos
   * @param linepoints
   * @return
   */
  @SuppressWarnings("unused")
  private Point3d getIntersection(Point3d eyePos, Point3d mousePos,
      Point3d[] linepoints) {
    Vector3d p1 = new Vector3d(linepoints[0]);
    Vector3d p2 = new Vector3d(linepoints[1]);

    Vector3d normal = new Vector3d();
    normal.cross(p1, p2);

    double d = -p1.dot(normal);
    Vector3d i1 = new Vector3d(eyePos);
    Vector3d direction = new Vector3d(eyePos);
    direction.sub(mousePos);

    double dot = direction.dot(normal);
    if (dot == 0) {
      return null;
    }

    double t = (-d - i1.dot(normal)) / (dot);
    Vector3d intersection = new Vector3d(eyePos);
    Vector3d scaledDirection = new Vector3d(direction);
    scaledDirection.scale(t);
    intersection.add(scaledDirection);
    Point3d intersectionPoint = new Point3d(intersection);
    return intersectionPoint;
  }

  /**
   * Users can overload this method which is called every time the Behavior
   * updates the transform
   *
   * Default implementation does nothing
   */
  public void transformChanged(Transform3D transform) {}

  /**
   * The transformChanged method in the callback class will be called every time
   * the transform is updated
   */
  public void setupCallback(MouseBehaviorCallback callback) {
    this.callback = callback;
  }

}
